package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public class HeldItemModule extends HudModule {
    public HeldItemModule() {
        super("Held Item", "Held item and its durability", Category.HUD, 170, 132);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        var p = mc.player;
        if (p == null) return;
        ItemStack s = p.getMainHandStack();
        if (s.isEmpty()) {
            label(ctx, mc, "Holding: —");
            return;
        }
        String t = s.getName().getString();
        if (s.isDamageable()) t += " (" + (s.getMaxDamage() - s.getDamage()) + "/" + s.getMaxDamage() + ")";
        label(ctx, mc, t);
    }
}
