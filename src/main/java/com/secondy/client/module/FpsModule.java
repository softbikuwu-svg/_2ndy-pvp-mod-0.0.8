package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class FpsModule extends HudModule {
    public FpsModule() {
        super("FPS", "Shows your FPS", Category.HUD, 6, 6);
        setEnabled(true);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        label(ctx, mc, "FPS: " + mc.getCurrentFps());
    }
}
