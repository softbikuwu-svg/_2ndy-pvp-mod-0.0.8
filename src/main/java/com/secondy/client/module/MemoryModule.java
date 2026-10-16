package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class MemoryModule extends HudModule {
    public MemoryModule() {
        super("Memory", "RAM usage", Category.HUD, 6, 156);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
        long max = rt.maxMemory() / 1048576L;
        label(ctx, mc, "RAM: " + used + " / " + max + " MB");
    }
}
