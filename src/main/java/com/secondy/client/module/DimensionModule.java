package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class DimensionModule extends HudModule {
    public DimensionModule() {
        super("Dimension", "Which dimension you are in", Category.HUD, 170, 150);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        if (mc.world == null) return;
        String id = mc.world.getRegistryKey().getValue().getPath();
        String name = switch (id) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> id;
        };
        label(ctx, mc, "World: " + name);
    }
}
