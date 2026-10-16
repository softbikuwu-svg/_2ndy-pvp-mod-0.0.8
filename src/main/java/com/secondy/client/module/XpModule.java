package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class XpModule extends HudModule {
    public XpModule() {
        super("XP Display", "XP level and progress to the next", Category.HUD, 170, 114);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        var p = mc.player;
        if (p == null) return;
        label(ctx, mc, "XP: " + p.experienceLevel + " (" + Math.round(p.experienceProgress * 100f) + "%)");
    }
}
