package com.secondy.client.mixin;

import com.secondy.client.module.LowFireModule;
import com.secondy.client.module.ModuleManager;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Low Fire, for versions where renderFireOverlay is an instance method. Only one of the two variants applies. */
@Mixin(targets = "net.minecraft.client.gui.hud.InGameOverlayRenderer")
public abstract class FireOverlayMixin {
    @ModifyVariable(method = "renderFireOverlay", at = @At("HEAD"), argsOnly = true, require = 0)
    private MatrixStack secondy$lowFireInst(MatrixStack m) {
        LowFireModule lf = ModuleManager.get(LowFireModule.class);
        if (lf.isEnabled()) m.translate(0f, -lf.offset(), 0f);
        return m;
    }
}
