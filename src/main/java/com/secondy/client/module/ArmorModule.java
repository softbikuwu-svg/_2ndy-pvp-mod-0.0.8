package com.secondy.client.module;

import com.secondy.client.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class ArmorModule extends HudModule {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public ArmorModule() {
        super("Armor Status", "Shows armor and durability", Category.HUD, 100, 6);
        width = 56;
        height = 72;
        setEnabled(true);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        var p = mc.player;
        if (p == null) return;
        background(ctx);
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = p.getEquippedStack(SLOTS[i]);
            int iy = y + 1 + i * 18;
            if (stack.isEmpty()) continue;
            ctx.drawItem(stack, x + 2, iy);
            if (stack.isDamageable()) {
                String dur = String.valueOf(stack.getMaxDamage() - stack.getDamage());
                ctx.drawText(mc.textRenderer, dur, x + 22, iy + 4, Theme.color(i * 0.1f), true);
            }
        }
    }
}
