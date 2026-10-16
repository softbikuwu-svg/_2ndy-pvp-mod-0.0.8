package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.Optional;

public class ReachModule extends HudModule {
    private double last = 0;
    private boolean has = false;

    public ReachModule() {
        super("Reach Display", "Distance of your last hit", Category.PVP, 6, 96);
    }

    public void hit(PlayerEntity player, Entity target) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1.0f).multiply(8.0));
        Optional<Vec3d> r = target.getBoundingBox().raycast(eye, end);
        if (r.isPresent()) {
            last = eye.distanceTo(r.get());
            has = true;
        }
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        label(ctx, mc, has ? String.format(Locale.ROOT, "Reach: %.2f", last) : "Reach: -");
    }
}
