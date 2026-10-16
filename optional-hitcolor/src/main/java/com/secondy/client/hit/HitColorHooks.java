package com.secondy.client.hit;

import com.secondy.client.module.HitColorModule;
import com.secondy.client.module.ModuleManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;

/** Вызывается из миксина: решает, какой оверлей применить к броне. */
public final class HitColorHooks {
    private HitColorHooks() {}

    /** UV «красного» оверлея урона (тот же, что ваниль применяет к телу). */
    private static final int HURT_UV = OverlayTexture.packUv(OverlayTexture.getU(0f), OverlayTexture.getV(true));

    private static HitColorModule module;

    public static int armorOverlay(Object state, int original) {
        try {
            if (module == null) module = ModuleManager.get(HitColorModule.class);
            if (!module.isEnabled() || !module.throughArmor()) return original;
            if (state instanceof LivingEntityRenderState s && s.hurt) return HURT_UV;
        } catch (Throwable ignored) {
            // рендер не должен падать из-за мода
        }
        return original;
    }
}
