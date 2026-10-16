package com.secondy.client.mixin;

import com.secondy.client.module.LowFireModule;
import com.secondy.client.module.ModuleManager;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Low Fire, for versions where renderFireOverlay is a static method. */
@Mixin(targets = "net.minecraft.client.gui.hud.InGameOverlayRenderer")
public abstract class FireOverlayStaticMixin {
    @ModifyVariable(method = "renderFireOverlay", at = @At("HEAD"), argsOnly = true, require = 0)
    private static MatrixStack secondy$lowFire(MatrixStack m) {
        LowFireModule lf = ModuleManager.get(LowFireModule.class);
        if (lf.isEnabled()) m.translate(0f, -lf.offset(), 0f);
        return m;
    }
}
