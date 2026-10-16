package com.secondy.client.mixin;

import com.secondy.client.module.FreeLookModule;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Freelook: every time the camera sets its rotation it gets the freelook angles instead.
 * Hooking setRotation itself means the third person offset is computed from the same angles.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float secondy$yaw(float yaw) {
        return FreeLookModule.active ? FreeLookModule.yaw : yaw;
    }

    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private float secondy$pitch(float pitch) {
        return FreeLookModule.active ? FreeLookModule.pitch : pitch;
    }
}
