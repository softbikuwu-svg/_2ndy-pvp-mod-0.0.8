package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class FullbrightModule extends Module {
    public FullbrightModule() {
        super("Fullbright", "Full brightness (client-side night vision)", Category.VISUAL);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        if (!mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            mc.player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
        }
    }

    @Override
    public void onDisable() {
        var p = MinecraftClient.getInstance().player;
        if (p != null) p.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
