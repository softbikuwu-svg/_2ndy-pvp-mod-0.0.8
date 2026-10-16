package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

public class SaturationModule extends HudModule {
    public SaturationModule() {
        super("Saturation", "Saturation and hunger", Category.HUD, 6, 246);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        var p = mc.player;
        if (p == null) return;
        var h = p.getHungerManager();
        label(ctx, mc, String.format(Locale.ROOT, "Saturation: %.1f | Hunger: %d", h.getSaturationLevel(), h.getFoodLevel()));
    }
}
