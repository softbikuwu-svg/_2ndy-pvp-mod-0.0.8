package com.secondy.client.module;

import com.secondy.client.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** A module drawn on screen that can be dragged and resized. */
public abstract class HudModule extends Module {
    public int x, y;
    /** HUD size multiplier (resize by dragging the corner handle while the chat is open). */
    public float scale = 1f;
    /** Draw a dark backing plate behind the module. */
    public boolean showBackground = true;
    protected int width = 60, height = 14;

    protected HudModule(String name, String description, Category category, int x, int y) {
        super(name, description, category);
        this.x = x;
        this.y = y;
    }

    /** Unscaled size, as the module lays itself out. */
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    /** On-screen size after applying {@link #scale}. */
    public int getScaledWidth() { return Math.max(1, Math.round(width * scale)); }
    public int getScaledHeight() { return Math.max(1, Math.round(height * scale)); }

    public boolean contains(int mx, int my) {
        return mx >= x && mx <= x + getScaledWidth() && my >= y && my <= y + getScaledHeight();
    }

    /** Draws the module with its scale applied (scaling happens around the top-left corner). */
    public final void drawScaled(DrawContext ctx, MinecraftClient mc) {
        if (Math.abs(scale - 1f) < 0.001f) {
            draw(ctx, mc);
            return;
        }
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(scale, scale);
        m.translate((float) -x, (float) -y);
        try {
            draw(ctx, mc);
        } finally {
            m.popMatrix();
        }
    }

    public abstract void draw(DrawContext ctx, MinecraftClient mc);

    protected void background(DrawContext ctx) {
        if (!showBackground) return;
        ctx.fill(x, y, x + width, y + height, 0x90000000);
    }

    /** Plate with several lines of text. */
    protected void lines(DrawContext ctx, MinecraftClient mc, java.util.List<String> text) {
        int w = 0;
        for (String s : text) w = Math.max(w, mc.textRenderer.getWidth(s));
        width = w + 8;
        height = text.size() * 10 + 4;
        background(ctx);
        for (int i = 0; i < text.size(); i++) {
            ctx.drawText(mc.textRenderer, text.get(i), x + 4, y + 3 + i * 10, Theme.color(i * 0.08f), true);
        }
    }

    /** Plate with a single line of text. */
    protected void label(DrawContext ctx, MinecraftClient mc, String text) {
        width = mc.textRenderer.getWidth(text) + 8;
        height = 14;
        background(ctx);
        ctx.drawText(mc.textRenderer, text, x + 4, y + 3, Theme.color(0f), true);
    }
}
