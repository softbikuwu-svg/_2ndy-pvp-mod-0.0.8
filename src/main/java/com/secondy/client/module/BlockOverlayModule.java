package com.secondy.client.module;

import com.secondy.client.settings.BoolSetting;
import com.secondy.client.settings.ColorSetting;
import com.secondy.client.settings.NumberSetting;
import com.secondy.client.util.Proj;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

/**
 * Own outline for the block you are looking at (first person). Drawn as a 2D overlay like the pearl trajectory:
 * edges hidden behind the block itself are skipped. The vanilla outline is still drawn underneath.
 */
public class BlockOverlayModule extends Module {
    private final ColorSetting color = add(new ColorSetting("Color", "color", 0x0A84FF));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", "opacity", 90, 10, 100, 5, "%"));
    private final NumberSetting thickness = add(new NumberSetting("Thickness", "thickness", 1, 1, 3, 1, " px"));
    private final BoolSetting allEdges = add(new BoolSetting("Show hidden edges", "all_edges", false));

    public BlockOverlayModule() {
        super("Block Overlay", "Custom outline for the targeted block", Category.VISUAL);
    }

    public void draw(DrawContext ctx, MinecraftClient mc) {
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        if (mc.world == null || !Proj.begin(mc)) return;

        BlockPos pos = hit.getBlockPos();
        VoxelShape shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) return;

        int col = color.get(Math.round(opacity.getInt() * 2.55f));
        int th = thickness.getInt();
        boolean all = allEdges.get();
        int[] count = { 0 };
        shape.forEachBox((x1, y1, z1, x2, y2, z2) -> {
            if (count[0]++ >= 10) return; // complicated shapes: draw the first boxes only
            box(ctx, pos.getX() + x1, pos.getY() + y1, pos.getZ() + z1,
                    pos.getX() + x2, pos.getY() + y2, pos.getZ() + z2, th, col, all);
        });
    }

    private static void box(DrawContext ctx, double x1, double y1, double z1, double x2, double y2, double z2,
                            int th, int col, boolean all) {
        double cx = Proj.camX(), cy = Proj.camY(), cz = Proj.camZ();
        double[] xs = { x1, x2 }, ys = { y1, y2 }, zs = { z1, z2 };
        // Which faces look towards the camera: [min side, max side] per axis
        boolean[] fx = { cx < x1, cx > x2 }, fy = { cy < y1, cy > y2 }, fz = { cz < z1, cz > z2 };
        for (int a = 0; a < 2; a++) {
            for (int b = 0; b < 2; b++) {
                if (all || fy[a] || fz[b]) Proj.line(ctx, x1, ys[a], zs[b], x2, ys[a], zs[b], th, col);
                if (all || fx[a] || fz[b]) Proj.line(ctx, xs[a], y1, zs[b], xs[a], y2, zs[b], th, col);
                if (all || fx[a] || fy[b]) Proj.line(ctx, xs[a], ys[b], z1, xs[a], ys[b], z2, th, col);
            }
        }
    }
}
