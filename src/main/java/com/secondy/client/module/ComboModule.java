package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class ComboModule extends HudModule {
    private int combo = 0;

    public ComboModule() {
        super("Combo Counter", "Counts consecutive hits until you get hit", Category.PVP, 6, 78);
    }

    public void hit() { combo++; }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player != null && mc.player.hurtTime > 0) combo = 0;
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        label(ctx, mc, "Combo: " + combo);
    }
}
