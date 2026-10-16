package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

public class CoordsModule extends HudModule {
    public CoordsModule() {
        super("Coordinates", "Shows your coordinates", Category.HUD, 6, 42);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        var p = mc.player;
        if (p == null) return;
        label(ctx, mc, String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f", p.getX(), p.getY(), p.getZ()));
    }
}
