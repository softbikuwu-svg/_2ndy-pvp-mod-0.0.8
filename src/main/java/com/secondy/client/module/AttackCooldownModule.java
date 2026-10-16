package com.secondy.client.module;

import com.secondy.client.settings.BoolSetting;
import com.secondy.client.settings.ColorSetting;
import com.secondy.client.settings.ModeSetting;
import com.secondy.client.settings.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

/** Attack cooldown indicator (1.9+ PvP): полоска и/или проценты. */
public class AttackCooldownModule extends HudModule {
    private final ModeSetting mode = add(new ModeSetting("Style", "mode", 0, "Bar", "Percent", "Bar + %"));
    private final NumberSetting barWidth = add(new NumberSetting("Bar width", "bar_width", 60, 20, 200, 5, " px"));
    private final NumberSetting barHeight = add(new NumberSetting("Bar height", "bar_height", 5, 2, 14, 1, " px"));
    private final BoolSetting hideFull = add(new BoolSetting("Hide when fully charged", "hide_full", false));
    private final ColorSetting chargeColor = add(new ColorSetting("Charging color", "charge_color", 0xFF9F0A));
    private final ColorSetting readyColor = add(new ColorSetting("Ready color", "ready_color", 0x30D158));
    private final ColorSetting textColor = add(new ColorSetting("Text color", "text_color", 0xFFFFFF));

    public AttackCooldownModule() {
        super("Attack Cooldown", "Attack cooldown indicator", Category.PVP, 200, 140);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        var p = mc.player;
        if (p == null) return;
        boolean preview = mc.currentScreen instanceof ChatScreen;
        float prog = preview ? 0.7f : Math.max(0f, Math.min(1f, p.getAttackCooldownProgress(0.0f)));
        boolean ready = prog >= 0.999f;
        if (ready && hideFull.get() && !preview) return;

        int m = mode.index();
        boolean bar = m != 1;
        boolean text = m != 0;
        String pct = Math.round(prog * 100f) + "%";
        int bw = barWidth.getInt(), bh = barHeight.getInt();

        int w = 8, h = 4;
        if (bar) { w = Math.max(w, bw + 8); h += bh; }
        if (text) {
            w = Math.max(w, mc.textRenderer.getWidth(pct) + 8);
            h += 10 + (bar ? 2 : 0);
        }
        width = w;
        height = h;
        background(ctx);

        int ty = y + 2;
        if (bar) {
            int bx = x + (w - bw) / 2;
            ctx.fill(bx, ty, bx + bw, ty + bh, 0x80101010);
            int fill = Math.round(bw * prog);
            if (fill > 0) ctx.fill(bx, ty, bx + fill, ty + bh, ready ? readyColor.get() : chargeColor.get());
            ty += bh + 2;
        }
        if (text) {
            ctx.drawText(mc.textRenderer, pct, x + (w - mc.textRenderer.getWidth(pct)) / 2, ty, textColor.get(), true);
        }
    }
}
