package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

public class HealthModule extends HudModule {
    public HealthModule() {
        super("Health", "Your health (and absorption)", Category.HUD, 170, 96);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        var p = mc.player;
        if (p == null) return;
        float abs = p.getAbsorptionAmount();
        String t = String.format(Locale.ROOT, "HP: %.1f / %.0f", p.getHealth(), p.getMaxHealth());
        if (abs > 0) t += String.format(Locale.ROOT, " (+%.1f)", abs);
        label(ctx, mc, t);
    }
}
