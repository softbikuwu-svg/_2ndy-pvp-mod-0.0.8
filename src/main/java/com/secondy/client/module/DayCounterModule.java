package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class DayCounterModule extends HudModule {
    public DayCounterModule() {
        super("Day Counter", "In-game days passed in the world", Category.HUD, 170, 6);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        if (mc.world == null) return;
        label(ctx, mc, "Day: " + (mc.world.getTime() / 24000L + 1));
    }
}
