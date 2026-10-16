package com.secondy.client.mixin;

import com.secondy.client.util.HandHooks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * First-person item rendering. Each hook only reads one argument by its type (they are unique in the method),
 * which keeps this independent of the exact parameter list. HandHooks combines them.
 */
@Mixin(targets = "net.minecraft.client.render.item.HeldItemRenderer")
public abstract class HeldItemMixin {
    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private MatrixStack secondy$matrices(MatrixStack m) {
        HandHooks.matrices(m);
        return m;
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private ItemStack secondy$stack(ItemStack s) {
        HandHooks.stack(s);
        return s;
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private Hand secondy$hand(Hand h) {
        HandHooks.hand(h);
        return h;
    }
}
