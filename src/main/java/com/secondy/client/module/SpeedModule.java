package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

public class SpeedModule extends HudModule {
    private double lastX, lastZ, speed;
    private boolean hasLast;

    public SpeedModule() {
        super("Speedometer", "Speed in blocks per second", Category.HUD, 6, 174);
    }

    @Override
    public void onDisable() { hasLast = false; speed = 0; }

    @Override
    public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (p == null) return;
        if (hasLast) speed = Math.hypot(p.getX() - lastX, p.getZ() - lastZ) * 20.0;
        lastX = p.getX();
        lastZ = p.getZ();
        hasLast = true;
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        label(ctx, mc, String.format(Locale.ROOT, "Speed: %.2f b/s", speed));
    }
}
