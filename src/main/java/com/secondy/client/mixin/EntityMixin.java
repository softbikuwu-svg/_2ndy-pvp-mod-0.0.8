package com.secondy.client.mixin;

import com.secondy.client.module.FreeLookModule;
import com.secondy.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freelook: while active the mouse turns the camera instead of the player. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true, require = 0)
    private void secondy$freelook(double dx, double dy, CallbackInfo ci) {
        if (FreeLookModule.active && (Object) this == MinecraftClient.getInstance().player) {
            FreeLookModule.look(dx, dy, ModuleManager.get(FreeLookModule.class).sens.getFloat());
            ci.cancel();
        }
    }
}
