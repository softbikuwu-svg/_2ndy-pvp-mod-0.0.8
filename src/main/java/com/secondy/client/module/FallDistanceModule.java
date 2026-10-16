package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/** Считает падение сам (по высоте), чтобы не зависеть от полей Entity, которые меняются между версиями. */
public class FallDistanceModule extends HudModule {
    private double peakY;
    private double fall;
    private boolean wasGround = true;

    public FallDistanceModule() {
        super("Fall Distance", "How many blocks you have fallen", Category.HUD, 170, 168);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (p == null) return;
        double y = p.getY();
        boolean ground = p.isOnGround();
        if (ground) {
            fall = 0;
            peakY = y;
        } else {
            if (wasGround) peakY = y;
            peakY = Math.max(peakY, y);
            fall = Math.max(0, peakY - y);
        }
        wasGround = ground;
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        label(ctx, mc, String.format(Locale.ROOT, "Fall: %.1f", fall));
    }
}
