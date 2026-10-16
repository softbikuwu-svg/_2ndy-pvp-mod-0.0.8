package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class PlaytimeModule extends HudModule {
    private static final long START = System.currentTimeMillis();

    public PlaytimeModule() {
        super("Playtime", "Time played this session", Category.HUD, 170, 78);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        long s = (System.currentTimeMillis() - START) / 1000L;
        label(ctx, mc, String.format(java.util.Locale.ROOT, "Played: %d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60));
    }
}
