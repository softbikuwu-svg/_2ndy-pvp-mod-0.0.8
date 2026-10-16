package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class LightModule extends HudModule {
    public LightModule() {
        super("Light Level", "Light level where you stand", Category.HUD, 6, 282);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        var p = mc.player;
        if (p == null || mc.world == null) return;
        label(ctx, mc, "Light: " + mc.world.getLightLevel(p.getBlockPos()));
    }
}
