package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class DirectionModule extends HudModule {
    public DirectionModule() {
        super("Direction", "Where you are facing (compass)", Category.HUD, 6, 192);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        var p = mc.player;
        if (p == null) return;
        Direction d = p.getHorizontalFacing();
        String name = switch (d) {
            case NORTH -> "North (-Z)";
            case SOUTH -> "South (+Z)";
            case EAST -> "East (+X)";
            case WEST -> "West (-X)";
            default -> d.asString();
        };
        label(ctx, mc, name + "  " + Math.round(MathHelper.wrapDegrees(p.getYaw())) + "°");
    }
}
