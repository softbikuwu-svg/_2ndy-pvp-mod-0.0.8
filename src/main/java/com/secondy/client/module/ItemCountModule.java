package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Базовый класс: считает, сколько определённых предметов лежит в инвентаре. */
public abstract class ItemCountModule extends HudModule {
    private final Item item;
    private final String label;

    protected ItemCountModule(String name, String description, int x, int y, Item item, String label) {
        super(name, description, Category.PVP, x, y);
        this.item = item;
        this.label = label;
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        var p = mc.player;
        if (p == null) return;
        int total = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) total += s.getCount();
        }
        label(ctx, mc, label + ": " + total);
    }
}
