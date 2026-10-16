package com.secondy.client.util;

import com.secondy.client.module.ItemCustomizerModule;
import com.secondy.client.module.LowShieldModule;
import com.secondy.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

/**
 * Collects the matrix stack, item and hand of a first-person item render (three separate mixin hooks,
 * so the exact method signature does not matter) and applies Low Shield / Item Customizer once all three arrived.
 */
public final class HandHooks {
    private HandHooks() {}

    private static MatrixStack matrices;
    private static ItemStack stack;
    private static Hand hand;
    private static int have;

    // Transform applied by the previous call. Vanilla pushes/pops the matrix stack only *inside* the method,
    // so whatever we do before it stays on the stack and would leak into the next hand. We undo it at the next call.
    private static MatrixStack lastM;
    private static float lastDy;
    private static float[] lastX;

    private static final float PVX = 0.56f, PVY = -0.52f, PVZ = -0.72f;

    public static void matrices(MatrixStack m) {
        undoLast(m);
        matrices = m;
        have |= 1;
        tryApply();
    }

    public static void stack(ItemStack s) { stack = s; have |= 2; tryApply(); }

    public static void hand(Hand h) { hand = h; have |= 4; tryApply(); }

    private static void tryApply() {
        if (have != 7) return;
        have = 0;
        MatrixStack m = matrices;
        ItemStack s = stack;
        Hand h = hand;
        matrices = null;
        stack = null;
        hand = null;
        try {
            apply(m, s, h);
        } catch (Throwable ignored) {
            // never break rendering because of a cosmetic feature
        }
    }

    private static void apply(MatrixStack m, ItemStack s, Hand h) {
        if (m == null || s == null || h == null || s.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        float dy = 0f;
        LowShieldModule ls = ModuleManager.get(LowShieldModule.class);
        if (ls.isEnabled() && s.isOf(Items.SHIELD)) dy = ls.offset();
        int side = ItemCustomizerModule.sideOf(mc, h);
        ItemCustomizerModule ic = ModuleManager.get(ItemCustomizerModule.class);
        float[] x = ic.isEnabled() ? ic.xform(s, h, side) : null;
        if (dy == 0f && x == null) return;

        if (dy != 0f) m.translate(0f, -dy, 0f);
        if (x != null) {
            float px = PVX * side;
            m.translate(px, PVY, PVZ);
            m.translate(x[0], x[1], x[2]);
            if (x[3] != 0) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(x[3]));
            if (x[4] != 0) m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(x[4]));
            if (x[5] != 0) m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(x[5]));
            if (x[6] != 1f) m.scale(x[6], x[6], x[6]);
            m.translate(-px, -PVY, -PVZ);
        }
        lastM = m;
        lastDy = dy;
        lastX = x == null ? null : new float[]{ x[0], x[1], x[2], x[3], x[4], x[5], x[6], side };
    }

    /** Reverses the previous transform (exact inverse, in reverse order) if it was done on this same stack. */
    private static void undoLast(MatrixStack m) {
        MatrixStack prev = lastM;
        float[] x = lastX;
        float dy = lastDy;
        lastM = null;
        lastX = null;
        lastDy = 0f;
        if (prev == null || prev != m) return;
        try {
            if (x != null) {
                float side = x[7];
                float px = PVX * side;
                m.translate(px, PVY, PVZ);
                if (x[6] != 1f) m.scale(1f / x[6], 1f / x[6], 1f / x[6]);
                if (x[5] != 0) m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-x[5]));
                if (x[4] != 0) m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-x[4]));
                if (x[3] != 0) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-x[3]));
                m.translate(-x[0], -x[1], -x[2]);
                m.translate(-px, -PVY, -PVZ);
            }
            if (dy != 0f) m.translate(0f, dy, 0f);
        } catch (Throwable ignored) {
        }
    }
}
