package com.secondy.client.ui;

import com.secondy.client.module.CrosshairModule;
import com.secondy.client.settings.PixelGridSetting;
import com.secondy.client.util.Config;
import com.secondy.client.util.Input;
import com.secondy.client.util.Theme;
import com.secondy.client.util.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Pixel editor for the custom crosshair.
 * Left mouse button paints, right mouse button erases. Changes apply to the game immediately.
 */
public class CrosshairEditorScreen extends Screen {
    private static final int N = PixelGridSetting.N;
    private static final int PAD = 12, RADIUS = 9, COL_W = 138;

    private static final int PANEL        = 0xE0101012;
    private static final int PANEL_BORDER = 0x2EFFFFFF;
    private static final int TEXT         = 0xFFFFFFFF;
    private static final int TEXT_DIM     = 0xFF8E8E93;

    private static final int[] PALETTE = {
            0xFFFFFF, 0x000000, 0xFF453A, 0xFF9F0A, 0xFFD60A, 0x30D158,
            0x0A84FF, 0x64D2FF, 0xBF5AF2, 0xFF375F, 0x8E8E93, 0x5E5CE6
    };
    private static final String[] MIRROR = { "Off", "Horizontal", "Vertical", "Both" };
    private static final String[] PRESETS = { "Cross", "Cross + dot", "Dot", "Circle", "Square", "T-shape" };

    private final Screen parent;
    private final CrosshairModule module;
    private final PixelGridSetting grid;

    private int brush = 0xFFFFFF;
    private int mirror = 0;
    private int presetIdx = 0;

    private boolean wasLeft, wasRight;
    private boolean waitRelease;
    private int lastX = -1, lastY = -1;
    private long openedAt;

    public CrosshairEditorScreen(Screen parent, CrosshairModule module) {
        super(Text.literal("Crosshair editor"));
        this.parent = parent;
        this.module = module;
        this.grid = module.design();
    }

    @Override
    protected void init() {
        wasLeft = Input.mouse(0);
        wasRight = Input.mouse(1);
        waitRelease = wasLeft || wasRight;
        lastX = lastY = -1;
        openedAt = System.currentTimeMillis();
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        Config.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    // ───────────────────────── Painting ─────────────────────────

    private void paint(int x, int y, int rgbOrZero) {
        grid.set(x, y, rgbOrZero);
        boolean mx = mirror == 1 || mirror == 3;
        boolean my = mirror == 2 || mirror == 3;
        if (mx) grid.set(N - 1 - x, y, rgbOrZero);
        if (my) grid.set(x, N - 1 - y, rgbOrZero);
        if (mx && my) grid.set(N - 1 - x, N - 1 - y, rgbOrZero);
    }

    /** Paints a straight line of cells (so fast mouse movement leaves no gaps). */
    private void paintLine(int x0, int y0, int x1, int y1, int rgbOrZero) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0, y = y0;
        for (int guard = 0; guard < 64; guard++) {
            paint(x, y, rgbOrZero);
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx) { err += dx; y += sy; }
        }
    }

    private static void put(int[] g, int x, int y) {
        if (x >= 0 && y >= 0 && x < N && y < N) g[y * N + x] = 0xFFFFFFFF;
    }

    private static int[] preset(int i) {
        int c = N / 2;
        int[] g = new int[N * N];
        switch (i) {
            case 0, 1 -> {
                for (int d = 3; d <= 6; d++) {
                    put(g, c, c - d); put(g, c, c + d); put(g, c - d, c); put(g, c + d, c);
                }
                if (i == 1) put(g, c, c);
            }
            case 2 -> {
                for (int y = c - 1; y <= c + 1; y++) for (int x = c - 1; x <= c + 1; x++) put(g, x, y);
            }
            case 3 -> {
                for (int y = 0; y < N; y++) {
                    for (int x = 0; x < N; x++) {
                        double d = Math.sqrt((x - c) * (x - c) + (y - c) * (y - c));
                        if (d >= 5.0 && d <= 6.2) put(g, x, y);
                    }
                }
                put(g, c, c);
            }
            case 4 -> {
                for (int y = 0; y < N; y++) {
                    for (int x = 0; x < N; x++) {
                        if (Math.max(Math.abs(x - c), Math.abs(y - c)) == 5) put(g, x, y);
                    }
                }
            }
            default -> {
                for (int d = 2; d <= 6; d++) { put(g, c - d, c); put(g, c + d, c); put(g, c, c + d); }
                put(g, c, c);
            }
        }
        return g;
    }

    // ───────────────────────── Rendering ─────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);

        boolean left = Input.mouse(0), right = Input.mouse(1);
        boolean clicked = left && !wasLeft;
        wasLeft = left;
        wasRight = right;
        if (waitRelease) {
            if (!left && !right) waitRelease = false;
            clicked = false;
            left = false;
            right = false;
        }

        float open = Ui.ease((System.currentTimeMillis() - openedAt) / 180f);

        int cell = Math.max(6, Math.min(13, (this.height - 110) / N));
        int gridPx = N * cell;
        int pw = gridPx + COL_W + PAD * 3;
        int ph = Math.max(gridPx, 150) + 56;
        int px = (this.width - pw) / 2;
        int py = Math.max(4, (this.height - ph) / 2);

        Ui.round(ctx, px, py, pw, ph, RADIUS, Ui.alpha(PANEL, open));
        Ui.roundOutline(ctx, px, py, pw, ph, RADIUS, Ui.alpha(PANEL_BORDER, open));
        Ui.text(ctx, this.textRenderer, Text.literal("Crosshair editor").formatted(Formatting.BOLD),
                px + PAD, py + 11, Ui.alpha(Theme.color(0f), open));

        int gx = px + PAD, gy = py + 30;

        // Grid background (checkerboard) and cells
        int c = N / 2;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                int bg = ((x + y) & 1) == 0 ? 0xFF2B2B30 : 0xFF222226;
                ctx.fill(gx + x * cell, gy + y * cell, gx + (x + 1) * cell, gy + (y + 1) * cell, Ui.alpha(bg, open));
                int col = grid.get(x, y);
                if (col != 0) {
                    ctx.fill(gx + x * cell, gy + y * cell, gx + (x + 1) * cell, gy + (y + 1) * cell, Ui.alpha(col, open));
                }
            }
        }
        // Center marker: this cell sits exactly on the middle of your screen
        Theme.outline(ctx, gx + c * cell, gy + c * cell, cell, cell, Ui.alpha(Theme.accentColor(0f), 0.9f * open));
        Theme.outline(ctx, gx - 1, gy - 1, gridPx + 2, gridPx + 2, Ui.alpha(0x40FFFFFF, open));

        // Hover highlight + painting
        boolean overGrid = mx >= gx && my >= gy && mx < gx + gridPx && my < gy + gridPx;
        int cx = overGrid ? (mx - gx) / cell : -1;
        int cy = overGrid ? (my - gy) / cell : -1;
        if (overGrid) {
            Theme.outline(ctx, gx + cx * cell, gy + cy * cell, cell, cell, Ui.alpha(0xAAFFFFFF, open));
        }
        if ((left || right) && overGrid) {
            int val = left ? (0xFF000000 | brush) : 0;
            if (lastX >= 0) paintLine(lastX, lastY, cx, cy, val); else paint(cx, cy, val);
            lastX = cx;
            lastY = cy;
        } else if (!left && !right) {
            if (lastX >= 0) Config.save();
            lastX = lastY = -1;
        } else {
            lastX = lastY = -1; // dragging outside of the grid
        }

        // Right column
        int rx = gx + gridPx + PAD;
        Ui.text(ctx, this.textRenderer, "Brush color", rx, gy, Ui.alpha(TEXT_DIM, open));
        int sw = 18, sg = 4;
        for (int i = 0; i < PALETTE.length; i++) {
            int sx = rx + (i % 6) * (sw + sg);
            int sy = gy + 12 + (i / 6) * (sw + sg);
            Ui.round(ctx, sx, sy, sw, sw, 5, Ui.alpha(0xFF000000 | PALETTE[i], open));
            Ui.roundOutline(ctx, sx, sy, sw, sw, 5, Ui.alpha(0x40FFFFFF, open));
            if (PALETTE[i] == brush) {
                Ui.roundOutline(ctx, sx - 2, sy - 2, sw + 4, sw + 4, 6, Ui.alpha(0xFFFFFFFF, open));
            }
            if (clicked && in(mx, my, sx, sy, sw, sw)) brush = PALETTE[i];
        }

        int by = gy + 12 + 2 * (sw + sg) + 6;
        if (button(ctx, rx, by, COL_W, 16, "Mirror: " + MIRROR[mirror], mx, my, open) && clicked) {
            mirror = (mirror + 1) % MIRROR.length;
        }
        by += 20;
        if (button(ctx, rx, by, COL_W, 16, "Load: " + PRESETS[presetIdx], mx, my, open) && clicked) {
            grid.setAll(preset(presetIdx));
            presetIdx = (presetIdx + 1) % PRESETS.length;
            Config.save();
        }
        by += 20;
        int half = (COL_W - 4) / 2;
        if (button(ctx, rx, by, half, 16, "Clear", mx, my, open) && clicked) {
            grid.clear();
            Config.save();
        }
        if (button(ctx, rx + half + 4, by, half, 16, "Reset", mx, my, open) && clicked) {
            grid.reset();
            Config.save();
        }
        by += 24;

        // Live preview on two backgrounds (dark and bright) to check contrast
        Ui.text(ctx, this.textRenderer, "Preview", rx, by, Ui.alpha(TEXT_DIM, open));
        by += 11;
        int pvH = 34;
        ctx.fill(rx, by, rx + COL_W / 2, by + pvH, Ui.alpha(0xFF2F4A24, open));
        ctx.fill(rx + COL_W / 2, by, rx + COL_W, by + pvH, Ui.alpha(0xFFB9D6EE, open));
        int ps = Math.min(2, module.pixelSize().getInt());
        int x0 = rx + (COL_W - ps) / 2 - (N / 2) * ps;
        int y0 = by + (pvH - ps) / 2 - (N / 2) * ps;
        CrosshairModule.drawGrid(ctx, grid, x0, y0, ps, module.outlineSetting().get(), Math.round(255 * open), -1);

        // Done button
        int doneY = py + ph - PAD - 18;
        if (button(ctx, rx, doneY, COL_W, 18, "Done", mx, my, open, true) && clicked) {
            close();
            return;
        }

        // Footer hint
        String hint = "Left-click: paint   |   Right-click: erase   |   Esc: back";
        Ui.text(ctx, this.textRenderer, Ui.fit(this.textRenderer, hint, gridPx), gx, py + ph - 18,
                Ui.alpha(0xFF6C6C70, open));
    }

    private boolean button(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my, float open) {
        return button(ctx, x, y, w, h, label, mx, my, open, false);
    }

    /** Draws a button and returns whether the mouse is over it (the caller checks the click). */
    private boolean button(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my,
                           float open, boolean primary) {
        boolean hv = in(mx, my, x, y, w, h);
        int bg;
        if (primary) bg = Ui.alpha(Theme.accentColor(0f), hv ? 0.9f : 0.7f);
        else bg = hv ? 0x4AFFFFFF : 0x2EFFFFFF;
        Ui.round(ctx, x, y, w, h, 6, Ui.alpha(bg, open));
        String t = Ui.fit(this.textRenderer, label, w - 8);
        Ui.text(ctx, this.textRenderer, t, x + (w - this.textRenderer.getWidth(t)) / 2, y + (h - 8) / 2,
                Ui.alpha(TEXT, open));
        return hv;
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
