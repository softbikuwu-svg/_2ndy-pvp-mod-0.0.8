package com.secondy.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.secondy.client.hit.HitColorHooks;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Броня в ваниле рисуется с «нулевым» оверлеем (не краснеет при ударе).
 * Подменяем его на оверлей урона, если сущность сейчас «ранена» и модуль Hit Color включён.
 * require = 0: если в этой версии игры цель не найдена — миксин молча не применяется.
 */
@Mixin(targets = "net.minecraft.client.render.entity.equipment.EquipmentRenderer")
public abstract class EquipmentRendererMixin {
    @WrapOperation(
            method = "render",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/render/OverlayTexture;DEFAULT_UV:I",
                    opcode = Opcodes.GETSTATIC),
            require = 0)
    private int secondy$tintArmor(Operation<Integer> original, @Local(argsOnly = true) Object state) {
        return HitColorHooks.armorOverlay(state, original.call());
    }
}
