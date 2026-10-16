package com.secondy.client.util;

import net.minecraft.client.gui.DrawContext;

public final class Theme {
    private Theme() {}

    public static boolean chroma = false;

    /** Основной цвет интерфейса (настраивается во вкладке «Цвет»). */
    public static int accent = 0xFF0A84FF;
    /** Красить ли текст HUD-модулей в основной цвет. */
    public static boolean hudAccent = false;

    private static int rainbow(float offset) {
        float h = ((System.currentTimeMillis() % 4000L) / 4000f + offset) % 1f;
        return java.awt.Color.HSBtoRGB(h, 0.75f, 1f) | 0xFF000000;
    }

    /** Text color: белый, основной цвет (если включено) или радужный (Chroma). */
    public static int color(float offset) {
        if (chroma) return rainbow(offset);
        if (hudAccent) return accent;
        return 0xFFFFFFFF;
    }

    /** Цвет элементов меню (переключатели, вкладки). */
    public static int accentColor(float offset) {
        return chroma ? rainbow(offset) : accent;
    }

    public static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }
}
