package com.secondy.client.module;

import com.secondy.client.settings.BoolSetting;
import com.secondy.client.settings.ColorSetting;
import com.secondy.client.settings.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Траектория эндер-жемчуга (и по желанию снежков/яиц), пока предмет в руке.
 * Рисуется как 2D-оверлей: путь точками и маркер места падения. Работает от первого лица;
 * столкновения с существами не учитываются (только блоки).
 */
public class TrajectoryModule extends Module {
    private static final int MAX_TICKS = 120;

    private final BoolSetting extra = add(new BoolSetting("Also snowballs and eggs", "extra", false));
    private final BoolSetting showPath = add(new BoolSetting("Draw path", "show_path", true));
    private final BoolSetting showDist = add(new BoolSetting("Distance to landing spot", "show_dist", true));
    private final NumberSetting dotStep = add(new NumberSetting("Path dot spacing (ticks)", "dot_step", 2, 1, 5, 1, ""));
    private final NumberSetting dotSize = add(new NumberSetting("Dot size", "dot_size", 2, 1, 4, 1, " px"));
    private final NumberSetting markerSize = add(new NumberSetting("Landing marker size", "marker_size", 7, 3, 14, 1, " px"));
    private final ColorSetting pathColor = add(new ColorSetting("Path color", "path_color", 0x64D2FF));
    private final ColorSetting landColor = add(new ColorSetting("Landing color", "land_color", 0xFF453A));

    public TrajectoryModule() {
        super("Pearl Trajectory", "Shows where the held ender pearl will land", Category.VISUAL);
    }

    private boolean match(ItemStack s) {
        return s.isOf(Items.ENDER_PEARL) || (extra.get() && (s.isOf(Items.SNOWBALL) || s.isOf(Items.EGG)));
    }

    public void draw(DrawContext ctx, MinecraftClient mc) {
        var p = mc.player;
        if (p == null || mc.world == null || !mc.options.getPerspective().isFirstPerson()) return;
        ItemStack held = p.getMainHandStack();
        if (!match(held)) held = p.getOffHandStack();
        if (!match(held)) return;

        // Стартовые параметры броска (как у ThrownItemEntity: скорость 1.5, к ней добавляется скорость игрока)
        double yaw = Math.toRadians(p.getYaw());
        double pitch = Math.toRadians(p.getPitch());
        Vec3d look = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3d pv = p.getVelocity();
        Vec3d vel = look.multiply(1.5).add(pv.x, p.isOnGround() ? 0 : pv.y, pv.z);
        Vec3d pos = new Vec3d(p.getX(), p.getEyeY() - 0.1, p.getZ());

        List<Vec3d> points = new ArrayList<>();
        Vec3d landing = null;
        double floor = mc.world.getBottomY() - 32;
        for (int i = 0; i < MAX_TICKS; i++) {
            Vec3d next = pos.add(vel);
            var hit = mc.world.raycast(new RaycastContext(pos, next,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            if (hit.getType() != HitResult.Type.MISS) {
                landing = hit.getPos();
                points.add(landing);
                break;
            }
            points.add(next);
            pos = next;
            vel = vel.multiply(0.99).add(0, -0.03, 0);
            if (pos.y < floor) break;
        }

        // Камера: глаза игрока, углы взгляда
        Vec3d cam = p.getEyePos();
        Vec3d right = new Vec3d(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3d up = right.crossProduct(look);
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        double tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        double aspect = (double) sw / sh;

        if (showPath.get()) {
            int step = dotStep.getInt(), size = dotSize.getInt();
            int col = pathColor.get(200);
            for (int i = step - 1; i < points.size(); i += step) {
                int[] s = project(points.get(i), cam, look, right, up, tanHalf, aspect, sw, sh);
                if (s != null) ctx.fill(s[0] - size / 2, s[1] - size / 2, s[0] - size / 2 + size, s[1] - size / 2 + size, col);
            }
        }

        if (landing != null) {
            int[] s = project(landing, cam, look, right, up, tanHalf, aspect, sw, sh);
            if (s != null) {
                int m = markerSize.getInt();
                int col = landColor.get();
                int x1 = s[0] - m / 2, y1 = s[1] - m / 2;
                ctx.fill(x1, y1, x1 + m, y1 + 1, col);
                ctx.fill(x1, y1 + m - 1, x1 + m, y1 + m, col);
                ctx.fill(x1, y1, x1 + 1, y1 + m, col);
                ctx.fill(x1 + m - 1, y1, x1 + m, y1 + m, col);
                if (showDist.get()) {
                    String t = String.format(Locale.ROOT, "%.1f m", cam.distanceTo(landing));
                    ctx.drawText(mc.textRenderer, t, s[0] - mc.textRenderer.getWidth(t) / 2, y1 + m + 3, col, true);
                }
            }
        }
    }

    /** Проекция точки мира на экран (в GUI-координатах) или null, если точка за спиной. */
    private static int[] project(Vec3d pt, Vec3d cam, Vec3d look, Vec3d right, Vec3d up,
                                 double tanHalf, double aspect, int sw, int sh) {
        Vec3d d = pt.subtract(cam);
        double z = d.dotProduct(look);
        if (z < 0.05) return null;
        double nx = d.dotProduct(right) / (z * tanHalf * aspect);
        double ny = d.dotProduct(up) / (z * tanHalf);
        if (Math.abs(nx) > 1.5 || Math.abs(ny) > 1.5) return null;
        return new int[]{ (int) Math.round(sw / 2.0 + nx * sw / 2.0), (int) Math.round(sh / 2.0 - ny * sh / 2.0) };
    }
}
