package com.secondy.client.ui;

import com.secondy.client.module.HudModule;
import com.secondy.client.module.ModuleManager;
import com.secondy.client.util.Config;
import com.secondy.client.util.Input;
import com.secondy.client.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * Move and resize HUD elements with the mouse while the chat is open.
 * Drag an element to move it, drag the square handle in its bottom-right corner to resize it,
 * right-click an element to reset its size.
 */
public final class HudDragger {
    private static final float MIN_SCALE = 0.5f, MAX_SCALE = 3.0f;
    private static final int HANDLE = 6;

    private static HudModule dragging;
    private static HudModule resizing;
    private static int offX, offY;
    private static boolean wasDown, wasRight;

    private HudDragger() {}

    private static boolean onHandle(HudModule h, int mx, int my) {
        int hx = h.x + h.getScaledWidth();
        int hy = h.y + h.getScaledHeight();
        return mx >= hx - HANDLE && mx <= hx + HANDLE && my >= hy - HANDLE && my <= hy + HANDLE;
    }

    public static void update(DrawContext ctx, MinecraftClient mc, int mx, int my) {
        boolean down = Input.mouse(0);
        boolean right = Input.mouse(1);
        List<HudModule> huds = ModuleManager.huds();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        // Right click resets the size of the element under the cursor.
        if (right && !wasRight) {
            for (int i = huds.size() - 1; i >= 0; i--) {
                HudModule h = huds.get(i);
                if (h.isEnabled() && (h.contains(mx, my) || onHandle(h, mx, my))) {
                    h.scale = 1f;
                    Config.save();
                    break;
                }
            }
        }
        wasRight = right;

        if (down && !wasDown) {
            for (int i = huds.size() - 1; i >= 0; i--) {
                HudModule h = huds.get(i);
                if (!h.isEnabled()) continue;
                if (onHandle(h, mx, my)) {
                    resizing = h;
                    break;
                }
                if (h.contains(mx, my)) {
                    dragging = h;
                    offX = mx - h.x;
                    offY = my - h.y;
                    break;
                }
            }
        }
        if (!down && (dragging != null || resizing != null)) {
            dragging = null;
            resizing = null;
            Config.save();
        }

        if (resizing != null) {
            HudModule h = resizing;
            // Pick the scale that puts the bottom-right corner under the cursor (use the larger axis).
            float sx = (mx - h.x) / (float) Math.max(1, h.getWidth());
            float sy = (my - h.y) / (float) Math.max(1, h.getHeight());
            float s = Math.max(sx, sy);
            // Do not let the element grow past the screen edge.
            float maxX = (sw - h.x) / (float) Math.max(1, h.getWidth());
            float maxY = (sh - h.y) / (float) Math.max(1, h.getHeight());
            s = Math.min(s, Math.min(maxX, maxY));
            s = Math.round(s * 20f) / 20f; // snap to 0.05
            h.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
        } else if (dragging != null) {
            int maxX = Math.max(0, sw - dragging.getScaledWidth());
            int maxY = Math.max(0, sh - dragging.getScaledHeight());
            dragging.x = Math.max(0, Math.min(maxX, mx - offX));
            dragging.y = Math.max(0, Math.min(maxY, my - offY));
        }
        wasDown = down;

        for (HudModule h : huds) {
            if (!h.isEnabled()) continue;
            boolean active = h == dragging || h == resizing;
            boolean hover = h.contains(mx, my) || onHandle(h, mx, my);
            int color = active ? 0xFF55FF55 : (hover ? 0xFFFFFFFF : 0x80FFFFFF);
            Theme.outline(ctx, h.x - 1, h.y - 1, h.getScaledWidth() + 2, h.getScaledHeight() + 2, color);

            // Resize handle in the bottom-right corner
            int hx = h.x + h.getScaledWidth();
            int hy = h.y + h.getScaledHeight();
            int hc = h == resizing ? 0xFF55FF55 : (onHandle(h, mx, my) ? 0xFFFFFFFF : 0xB0FFFFFF);
            ctx.fill(hx - 2, hy - 2, hx + 4, hy + 4, 0xFF000000);
            ctx.fill(hx - 1, hy - 1, hx + 3, hy + 3, hc);

            // Scale readout while resizing
            if (h == resizing) {
                String t = Math.round(h.scale * 100f) + "%";
                ctx.drawText(mc.textRenderer, t, h.x, Math.max(0, h.y - 11), 0xFF55FF55, true);
            }
        }

        // Hint
        String hint = "Drag: move  |  Drag the corner square: resize  |  Right-click: reset size";
        int hw = mc.textRenderer.getWidth(hint);
        int hx = (sw - hw) / 2;
        ctx.fill(hx - 4, 2, hx + hw + 4, 14, 0x90000000);
        ctx.drawText(mc.textRenderer, hint, hx, 4, 0xFFE0E0E0, false);
    }
}
