package com.secondy.client.module;

import com.secondy.client.settings.BoolSetting;
import com.secondy.client.settings.ColorSetting;
import com.secondy.client.settings.ModeSetting;
import com.secondy.client.settings.NumberSetting;
import com.secondy.client.settings.PixelGridSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Custom crosshair: built-in styles or your own pixel drawing, with size, thickness, gap, color, outline and on-target color. */
public class CrosshairModule extends Module {
    public static final int STYLE_CUSTOM = 4;

    private final ModeSetting style = add(new ModeSetting("Style", "style", 0,
            "Cross", "Dot", "Cross + dot", "Square", "Custom (drawn)"));
    private final PixelGridSetting design = add(new PixelGridSetting("Custom design", "design", defaultDesign()));
    private final NumberSetting pixelSize = add(new NumberSetting("Custom pixel size", "pixel_size", 1, 1, 4, 1, " px"));
    private final NumberSetting size = add(new NumberSetting("Line length", "size", 5, 1, 20, 1, " px"));
    private final NumberSetting thickness = add(new NumberSetting("Thickness", "thickness", 1, 1, 5, 1, " px"));
    private final NumberSetting gap = add(new NumberSetting("Center gap", "gap", 3, 0, 15, 1, " px"));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", "opacity", 100, 10, 100, 5, "%"));
    private final BoolSetting outline = add(new BoolSetting("Black outline", "outline", true));
    private final BoolSetting dynamic = add(new BoolSetting("Dynamic (spreads when moving)", "dynamic", false));
    private final BoolSetting onTarget = add(new BoolSetting("Change color on target", "on_target", true));
    private final BoolSetting hideVanilla = add(new BoolSetting("Hide vanilla crosshair", "hide_vanilla", true));
    private final ColorSetting color = add(new ColorSetting("Color", "color", 0xFFFFFF));
    private final ColorSetting targetColor = add(new ColorSetting("Target color", "target_color", 0xFF453A));

    private float dyn = 0f;

    public CrosshairModule() {
        super("Custom Crosshair", "Your own crosshair: built-in styles or draw it yourself", Category.VISUAL);
    }

    public PixelGridSetting design() { return design; }
    public NumberSetting pixelSize() { return pixelSize; }
    public BoolSetting outlineSetting() { return outline; }

    /** Whether the vanilla crosshair should be hidden (called from SecondyClient). */
    public boolean hidesVanilla() {
        return isEnabled() && hideVanilla.get();
    }

    /** The default hand-drawn design: a plus sign with a gap in the middle. */
    private static int[] defaultDesign() {
        int n = PixelGridSetting.N, c = n / 2;
        int[] g = new int[n * n];
        for (int d = 3; d <= 6; d++) {
            g[(c - d) * n + c] = 0xFFFFFFFF;
            g[(c + d) * n + c] = 0xFFFFFFFF;
            g[c * n + (c - d)] = 0xFFFFFFFF;
            g[c * n + (c + d)] = 0xFFFFFFFF;
        }
        return g;
    }

    /**
     * Draws a pixel design with its top-left corner at (x0, y0). Used by the HUD and by the editor preview.
     *
     * @param ps      size of one design pixel on screen
     * @param alpha   0..255
     * @param tint    0xRRGGBB to paint every pixel with, or -1 to keep the drawn colors
     */
    public static void drawGrid(DrawContext ctx, PixelGridSetting g, int x0, int y0, int ps,
                                boolean withOutline, int alpha, int tint) {
        int n = PixelGridSetting.N;
        int a = Math.max(0, Math.min(255, alpha)) << 24;
        // Pass 0: black outline behind everything. Pass 1: colors.
        for (int pass = withOutline ? 0 : 1; pass < 2; pass++) {
            for (int y = 0; y < n; y++) {
                int x = 0;
                while (x < n) {
                    int c = g.get(x, y);
                    if (c == 0) { x++; continue; }
                    int x2 = x + 1;
                    while (x2 < n && g.get(x2, y) == c) x2++;
                    int left = x0 + x * ps, right = x0 + x2 * ps;
                    int top = y0 + y * ps, bottom = top + ps;
                    if (pass == 0) {
                        ctx.fill(left - 1, top - 1, right + 1, bottom + 1, a);
                    } else {
                        int rgb = tint >= 0 ? tint : (c & 0xFFFFFF);
                        ctx.fill(left, top, right, bottom, a | rgb);
                    }
                    x = x2;
                }
            }
        }
    }

    public void draw(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null || !mc.options.getPerspective().isFirstPerson()) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        float want = 0f;
        if (dynamic.get()) {
            var p = mc.player;
            if (p.isSprinting()) want = 3f;
            else if (!p.isOnGround()) want = 4f;
            else if (p.getVelocity().horizontalLengthSquared() > 0.0004) want = 2f;
        }
        dyn += (want - dyn) * 0.2f;

        int a = Math.round(255f * opacity.getInt() / 100f);
        boolean targeted = onTarget.get() && mc.targetedEntity != null;
        int rgb = targeted ? targetColor.current(0f) : color.current(0f);
        int s = style.index();

        if (s == STYLE_CUSTOM) {
            int ps = pixelSize.getInt();
            // Center cell of the grid sits exactly on the center pixel of the screen.
            int x0 = (sw - ps) / 2 - (PixelGridSetting.N / 2) * ps;
            int y0 = (sh - ps) / 2 - (PixelGridSetting.N / 2) * ps;
            drawGrid(ctx, design, x0, y0, ps, outline.get(), a, targeted ? rgb : -1);
            return;
        }

        int fillCol = (a << 24) | rgb;
        int outCol = a << 24;

        int t = thickness.getInt();
        int g = gap.getInt() + Math.round(dyn);
        int len = size.getInt();
        // Top-left of the center block (t x t). Same rounding as the vanilla crosshair, so it is truly centered.
        int c0x = (sw - t) / 2;
        int c0y = (sh - t) / 2;

        List<int[]> rects = new ArrayList<>();
        if (s == 0 || s == 2) {
            rects.add(new int[]{c0x, c0y - g - len, c0x + t, c0y - g});                  // up
            rects.add(new int[]{c0x, c0y + t + g, c0x + t, c0y + t + g + len});          // down
            rects.add(new int[]{c0x - g - len, c0y, c0x - g, c0y + t});                  // left
            rects.add(new int[]{c0x + t + g, c0y, c0x + t + g + len, c0y + t});          // right
        }
        if (s == 1 || s == 2) {
            int d = Math.max(2, t);
            int dx = (sw - d) / 2, dy = (sh - d) / 2;
            rects.add(new int[]{dx, dy, dx + d, dy + d});
        }
        if (s == 3) {
            int half = g + len;
            int l = c0x - half, r = c0x + t + half, tp = c0y - half, b = c0y + t + half;
            rects.add(new int[]{l, tp, r, tp + t});          // top
            rects.add(new int[]{l, b - t, r, b});            // bottom
            rects.add(new int[]{l, tp + t, l + t, b - t});   // left
            rects.add(new int[]{r - t, tp + t, r, b - t});   // right
        }

        if (outline.get()) {
            for (int[] r : rects) ctx.fill(r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1, outCol);
        }
        for (int[] r : rects) ctx.fill(r[0], r[1], r[2], r[3], fillCol);
    }
}
