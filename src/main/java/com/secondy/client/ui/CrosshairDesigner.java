package com.secondy.client.ui;

import com.secondy.client.settings.PixelGridSetting;
import com.secondy.client.util.Config;
import com.secondy.client.util.Theme;
import com.secondy.client.util.Ui;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Pixel crosshair editor that lives inside the settings panel (no separate screen).
 * Left mouse button paints, right mouse button erases. Everything applies to the game immediately.
 */
final class CrosshairDesigner {
    private static final int N = PixelGridSetting.N;

    private static final int[] PALETTE = {
            0xFFFFFF, 0xFF453A, 0xFF9F0A, 0xFFD60A, 0x30D158, 0x0A84FF, 0xBF5AF2, 0x000000
    };
    private static final String[] MIRROR = { "Off", "Horizontal", "Vertical", "Both" };
    private static final String[] PRESETS = { "Cross", "Cross + dot", "Dot", "Circle", "Square", "T-shape" };

    private int brush = 0xFFFFFF;
    private int mirror = 3;
    private int presetIdx = 0;
    private String presetLabel = "Presets";

    private boolean prevLeft, prevRight, paintOk;
    private int lastX = -1, lastY = -1;

    /** Total height of the block for a given cell size. */
    static int height(int cell) {
        return 12 + (N * cell + 8) + 8 + 12 + 14 + 6 + 16 + 4;
    }

    /** Call when the screen opens so a mouse button that is already held does not paint. */
    void sync(boolean left, boolean right) {
        prevLeft = left;
        prevRight = right;
        paintOk = false;
        lastX = lastY = -1;
    }

    // ───────────────────────── Painting ─────────────────────────

    private void paint(PixelGridSetting grid, int x, int y, int rgbOrZero) {
        grid.set(x, y, rgbOrZero);
        boolean mx = mirror == 1 || mirror == 3;
        boolean my = mirror == 2 || mirror == 3;
        if (mx) grid.set(N - 1 - x, y, rgbOrZero);
        if (my) grid.set(x, N - 1 - y, rgbOrZero);
        if (mx && my) grid.set(N - 1 - x, N - 1 - y, rgbOrZero);
    }

    /** Straight line of cells, so fast mouse movement leaves no gaps. */
    private void paintLine(PixelGridSetting grid, int x0, int y0, int x1, int y1, int rgbOrZero) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0, y = y0;
        for (int guard = 0; guard < 64; guard++) {
            paint(grid, x, y, rgbOrZero);
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

    void render(DrawContext ctx, TextRenderer tr, PixelGridSetting grid, int mx, int my,
                boolean left, boolean right, boolean clicked, float open,
                int x, int y, int w, int cell) {
        boolean pressed = (left && !prevLeft) || (right && !prevRight);
        prevLeft = left;
        prevRight = right;

        Ui.text(ctx, tr, "DESIGN", x, y, Ui.alpha(0xFF8E8E93, open));

        // Dark rounded card with the grid inside
        int gridPx = N * cell;
        int cardY = y + 12;
        Ui.round(ctx, x, cardY, w, gridPx + 8, 7, Ui.alpha(0xFF1B1B1F, open));
        int gx = x + (w - gridPx) / 2, gy = cardY + 4;

        // Background: one fill + thin grid lines (instead of 225 checkerboard cells), painted cells on top
        ctx.fill(gx, gy, gx + gridPx, gy + gridPx, Ui.alpha(0xFF222226, open));
        int lineCol = Ui.alpha(0xFF2D2D32, open);
        for (int i = 0; i < N; i++) {
            ctx.fill(gx + i * cell, gy, gx + i * cell + 1, gy + gridPx, lineCol);
            ctx.fill(gx, gy + i * cell, gx + gridPx, gy + i * cell + 1, lineCol);
        }
        for (int yy = 0; yy < N; yy++) {
            for (int xx = 0; xx < N; xx++) {
                int col = grid.get(xx, yy);
                if (col != 0) {
                    ctx.fill(gx + xx * cell + 1, gy + yy * cell + 1, gx + (xx + 1) * cell, gy + (yy + 1) * cell,
                            Ui.alpha(col, open));
                }
            }
        }
        // The center cell sits exactly on the middle of the screen
        int c = N / 2;
        Theme.outline(ctx, gx + c * cell, gy + c * cell, cell, cell, Ui.alpha(Theme.accentColor(0f), 0.9f * open));

        boolean overGrid = mx >= gx && my >= gy && mx < gx + gridPx && my < gy + gridPx;
        int cx = overGrid ? (mx - gx) / cell : -1;
        int cy = overGrid ? (my - gy) / cell : -1;
        if (overGrid) {
            Theme.outline(ctx, gx + cx * cell, gy + cy * cell, cell, cell, Ui.alpha(0xAAFFFFFF, open));
        }

        if (pressed) paintOk = overGrid;
        if (!left && !right) {
            if (lastX >= 0) Config.save();
            lastX = lastY = -1;
            paintOk = false;
        } else if (paintOk && overGrid) {
            int val = left ? (0xFF000000 | brush) : 0;
            if (lastX >= 0) paintLine(grid, lastX, lastY, cx, cy, val); else paint(grid, cx, cy, val);
            lastX = cx;
            lastY = cy;
        } else {
            lastX = lastY = -1; // dragging outside of the grid
        }

        // Brush colors
        int by = cardY + gridPx + 8 + 8;
        Ui.text(ctx, tr, "BRUSH", x, by, Ui.alpha(0xFF8E8E93, open));
        int sd = 12;
        int step = Math.min(18, (w - sd) / (PALETTE.length - 1));
        int sy = by + 12;
        for (int i = 0; i < PALETTE.length; i++) {
            int sx = x + i * step;
            Ui.round(ctx, sx, sy, sd, sd, sd / 2, Ui.alpha(0xFF000000 | PALETTE[i], open));
            Ui.roundOutline(ctx, sx, sy, sd, sd, sd / 2, Ui.alpha(0x40FFFFFF, open));
            if (PALETTE[i] == brush) {
                Ui.roundOutline(ctx, sx - 2, sy - 2, sd + 4, sd + 4, (sd + 4) / 2, Ui.alpha(0xFFFFFFFF, open));
            }
            if (clicked && in(mx, my, sx - 1, sy - 1, sd + 2, sd + 2)) brush = PALETTE[i];
        }

        // Buttons: Mirror / Presets / Clear
        String[] labels = { "Mirror: " + MIRROR[mirror], presetLabel, "Clear" };
        int[] ws = new int[3];
        int sum = 0;
        for (int i = 0; i < 3; i++) { ws[i] = tr.getWidth(labels[i]) + 12; sum += ws[i]; }
        int avail = w - 8;
        if (sum > avail) for (int i = 0; i < 3; i++) ws[i] = ws[i] * avail / sum;
        int bx = x, byy = sy + 14 + 6;
        for (int i = 0; i < 3; i++) {
            boolean hv = in(mx, my, bx, byy, ws[i], 16);
            Ui.round(ctx, bx, byy, ws[i], 16, 6, Ui.alpha(hv ? 0x4AFFFFFF : 0x2EFFFFFF, open));
            String t = Ui.fit(tr, labels[i], ws[i] - 6);
            Ui.text(ctx, tr, t, bx + (ws[i] - tr.getWidth(t)) / 2, byy + 4, Ui.alpha(0xFFFFFFFF, open));
            if (clicked && hv) {
                if (i == 0) {
                    mirror = (mirror + 1) % MIRROR.length;
                } else if (i == 1) {
                    grid.setAll(preset(presetIdx));
                    presetLabel = PRESETS[presetIdx];
                    presetIdx = (presetIdx + 1) % PRESETS.length;
                    Config.save();
                } else {
                    grid.clear();
                    Config.save();
                }
            }
            bx += ws[i] + 4;
        }
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
