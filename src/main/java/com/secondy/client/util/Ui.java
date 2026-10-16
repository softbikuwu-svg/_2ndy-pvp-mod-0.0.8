package com.secondy.client.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Drawing helpers: smooth rounded rectangles (texture based), alpha colors, transitions. */
public final class Ui {
    private Ui() {}

    /** Умножает альфу цвета на f (0..1). */
    public static int alpha(int color, float f) {
        int a = Math.round((color >>> 24) * Math.max(0f, Math.min(1f, f)));
        return (a << 24) | (color & 0xFFFFFF);
    }

    /** Линейный переход между двумя ARGB-цветами. */
    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 255) + (((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t);
        int r = (int) (((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
        int g = (int) (((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
        int b = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ───────────────────────── Rounded shapes (anti-aliased textures) ─────────────────────────
    //
    // Every rounded rectangle is drawn from a pre-rendered, anti-aliased 9-slice texture
    // (assets/secondy_client/textures/gui/rr/f<r>.png = fill, o<r>.png = 1px outline):
    // 9 quads instead of one fill() per pixel row, and the corners are smooth, not stair-stepped.

    private static final int MAX_R = 16;
    /** Texels per GUI pixel in the textures (see the generator script in the repo notes). */
    private static final int K = 4;
    private static final Identifier[] FILL = new Identifier[MAX_R + 1];
    private static final Identifier[] RING = new Identifier[MAX_R + 1];

    private static Identifier tex(Identifier[] cache, String prefix, int r) {
        Identifier id = cache[r];
        if (id == null) {
            id = Identifier.of("secondy_client", "textures/gui/rr/" + prefix + r + ".png");
            cache[r] = id;
        }
        return id;
    }

    private static void blit(DrawContext ctx, Identifier id, int x, int y, int w, int h,
                             int u, int v, int rw, int rh, int size, int color) {
        if (w <= 0 || h <= 0) return;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, (float) u, (float) v, w, h, rw, rh, size, size, color);
    }

    /** Smooth rounded rectangle. */
    public static void round(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
        r = Math.min(Math.min(r, MAX_R), Math.min(w, h) / 2);
        if (r <= 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        Identifier id = tex(FILL, "f", r);
        int c = r * K, t = 2 * c + 2;
        int mw = w - 2 * r, mh = h - 2 * r;
        blit(ctx, id, x, y, r, r, 0, 0, c, c, t, color);
        blit(ctx, id, x + r, y, mw, r, c, 0, 2, c, t, color);
        blit(ctx, id, x + w - r, y, r, r, c + 2, 0, c, c, t, color);
        blit(ctx, id, x, y + r, r, mh, 0, c, c, 2, t, color);
        blit(ctx, id, x + r, y + r, mw, mh, c, c, 2, 2, t, color);
        blit(ctx, id, x + w - r, y + r, r, mh, c + 2, c, c, 2, t, color);
        blit(ctx, id, x, y + h - r, r, r, 0, c + 2, c, c, t, color);
        blit(ctx, id, x + r, y + h - r, mw, r, c, c + 2, 2, c, t, color);
        blit(ctx, id, x + w - r, y + h - r, r, r, c + 2, c + 2, c, c, t, color);
    }

    /** Thin smooth rounded outline. */
    public static void roundOutline(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 2 || h <= 2 || (color >>> 24) == 0) return;
        r = Math.min(Math.min(r, MAX_R), Math.min(w, h) / 2);
        if (r <= 0) {
            Theme.outline(ctx, x, y, w, h, color);
            return;
        }
        Identifier id = tex(RING, "o", r);
        int c = r * K, t = 2 * c + 2;
        int mw = w - 2 * r, mh = h - 2 * r;
        blit(ctx, id, x, y, r, r, 0, 0, c, c, t, color);
        blit(ctx, id, x + r, y, mw, r, c, 0, 2, c, t, color);
        blit(ctx, id, x + w - r, y, r, r, c + 2, 0, c, c, t, color);
        blit(ctx, id, x, y + r, r, mh, 0, c, c, 2, t, color);
        blit(ctx, id, x + w - r, y + r, r, mh, c + 2, c, c, 2, t, color);
        blit(ctx, id, x, y + h - r, r, r, 0, c + 2, c, c, t, color);
        blit(ctx, id, x + r, y + h - r, mw, r, c, c + 2, 2, c, t, color);
        blit(ctx, id, x + w - r, y + h - r, r, r, c + 2, c + 2, c, c, t, color);
    }

    /** Straight line of the given thickness (one rotated fill, so it costs a single draw call). */
    public static void line(DrawContext ctx, float x1, float y1, float x2, float y2, int thick, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        int len = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy));
        if (len < 1 || (color >>> 24) == 0) return;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x1, y1);
        m.rotate((float) Math.atan2(dy, dx));
        m.translate(0f, -thick / 2f);
        ctx.fill(0, 0, len, thick, color);
        m.popMatrix();
    }

    /** Soft glow around a rounded rectangle: a few fading outlines, drawn outward. */
    public static void glow(DrawContext ctx, int x, int y, int w, int h, int r, int color, int layers) {
        for (int i = 1; i <= layers; i++) {
            float f = 0.5f * (layers - i + 1) / (layers + 1f);
            roundOutline(ctx, x - i, y - i, w + 2 * i, h + 2 * i, r + i, alpha(color, f));
        }
    }

    /** Текст; при почти нулевой альфе не рисуется (иначе Minecraft сделает его непрозрачным). */
    public static void text(DrawContext ctx, TextRenderer tr, String s, int x, int y, int color) {
        if ((color >>> 24) < 8) return;
        ctx.drawText(tr, s, x, y, color, false);
    }

    public static void text(DrawContext ctx, TextRenderer tr, Text t, int x, int y, int color) {
        if ((color >>> 24) < 8) return;
        ctx.drawText(tr, t, x, y, color, false);
    }

    /** Обрезает строку по ширине (без зависимости от версии API TextRenderer). */
    public static String trim(TextRenderer tr, String s, int maxWidth) {
        if (tr.getWidth(s) <= maxWidth) return s;
        int end = s.length();
        while (end > 0 && tr.getWidth(s.substring(0, end)) > maxWidth) end--;
        return s.substring(0, end);
    }

    private static final Map<String, String> FIT_CACHE = new HashMap<>();

    /** Shortens a string to fit the given width, adding ".." when it had to be cut (results are cached). */
    public static String fit(TextRenderer tr, String s, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (tr.getWidth(s) <= maxWidth) return s;
        String key = maxWidth + "|" + s;
        String cached = FIT_CACHE.get(key);
        if (cached != null) return cached;
        int dots = tr.getWidth("..");
        String res = maxWidth <= dots ? trim(tr, s, maxWidth) : trim(tr, s, maxWidth - dots) + "..";
        if (FIT_CACHE.size() > 600) FIT_CACHE.clear();
        FIT_CACHE.put(key, res);
        return res;
    }

    public static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }
}
