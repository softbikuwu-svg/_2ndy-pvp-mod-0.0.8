package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.List;

public class PotionModule extends HudModule {
    public PotionModule() {
        super("Potion Effects", "Active effects and time left", Category.HUD, 6, 210);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        var p = mc.player;
        if (p == null) return;
        List<String> out = new ArrayList<>();
        for (StatusEffectInstance e : p.getStatusEffects()) {
            String name = e.getEffectType().value().getName().getString();
            int sec = e.getDuration() / 20;
            String time = e.getDuration() < 0 ? "inf" : String.format("%d:%02d", sec / 60, sec % 60);
            out.add(name + " " + (e.getAmplifier() + 1) + " (" + time + ")");
        }
        if (out.isEmpty()) out.add("No effects");
        lines(ctx, mc, out);
    }
}
