package com.secondy.client.util;

import com.secondy.client.module.FreeLookModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * World -> screen projection for 2D overlays (first person only, same math as the pearl trajectory).
 * The camera position is interpolated between client ticks so overlays do not jitter at high FPS.
 */
public final class Proj {
    private Proj() {}

    private static Vec3d prev, cur;
    private static long tickNs;

    // Frame state
    private static double cx, cy, cz, lx, ly, lz, rx, ry, rz, ux, uy, uz, tanHalf, aspect;
    private static int sw, sh;

    public static void tick(MinecraftClient mc) {
        if (mc.player == null) {
            prev = cur = null;
            return;
        }
        prev = cur != null ? cur : mc.player.getEyePos();
        cur = mc.player.getEyePos();
        tickNs = System.nanoTime();
    }

    /** Prepares the camera for this frame. False if overlays should not be drawn (third person, no player). */
    public static boolean begin(MinecraftClient mc) {
        var p = mc.player;
        if (p == null || cur == null || mc.world == null || !mc.options.getPerspective().isFirstPerson()) return false;
        double t = Math.max(0.0, Math.min(1.0, (System.nanoTime() - tickNs) / 50_000_000.0));
        cx = prev.x + (cur.x - prev.x) * t;
        cy = prev.y + (cur.y - prev.y) * t;
        cz = prev.z + (cur.z - prev.z) * t;

        float yawDeg = FreeLookModule.active ? FreeLookModule.yaw : p.getYaw();
        float pitchDeg = FreeLookModule.active ? FreeLookModule.pitch : p.getPitch();
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        lx = -Math.sin(yaw) * Math.cos(pitch);
        ly = -Math.sin(pitch);
        lz = Math.cos(yaw) * Math.cos(pitch);
        rx = -Math.cos(yaw);
        ry = 0;
        rz = -Math.sin(yaw);
        // up = right x look
        ux = ry * lz - rz * ly;
        uy = rz * lx - rx * lz;
        uz = rx * ly - ry * lx;

        sw = mc.getWindow().getScaledWidth();
        sh = mc.getWindow().getScaledHeight();
        tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        aspect = (double) sw / sh;
        return true;
    }

    public static double camX() { return cx; }
    public static double camY() { return cy; }
    public static double camZ() { return cz; }

    /** Depth of a point along the view direction. */
    private static double depth(double x, double y, double z) {
        return (x - cx) * lx + (y - cy) * ly + (z - cz) * lz;
    }

    private static float[] toScreen(double x, double y, double z, double d) {
        double dx = x - cx, dy = y - cy, dz = z - cz;
        double nx = (dx * rx + dy * ry + dz * rz) / (d * tanHalf * aspect);
        double ny = (dx * ux + dy * uy + dz * uz) / (d * tanHalf);
        return new float[]{ (float) (sw / 2.0 + nx * sw / 2.0), (float) (sh / 2.0 - ny * sh / 2.0) };
    }

    /** Screen position (GUI coordinates) or null if the point is behind the camera. */
    public static float[] project(double x, double y, double z) {
        double d = depth(x, y, z);
        if (d < 0.05) return null;
        return toScreen(x, y, z, d);
    }

    /** Distance from the camera to a point. */
    public static double dist(double x, double y, double z) {
        double dx = x - cx, dy = y - cy, dz = z - cz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** 3D line between two world points, clipped against the camera's near plane. */
    public static void line(net.minecraft.client.gui.DrawContext ctx, double ax, double ay, double az,
                            double bx, double by, double bz, int thick, int color) {
        final double near = 0.05;
        double da = depth(ax, ay, az), db = depth(bx, by, bz);
        if (da < near && db < near) return;
        if (da < near) {
            double t = (near - da) / (db - da);
            ax += (bx - ax) * t; ay += (by - ay) * t; az += (bz - az) * t;
            da = near;
        } else if (db < near) {
            double t = (near - db) / (da - db);
            bx += (ax - bx) * t; by += (ay - by) * t; bz += (az - bz) * t;
            db = near;
        }
        float[] a = toScreen(ax, ay, az, da), b = toScreen(bx, by, bz, db);
        Ui.line(ctx, a[0], a[1], b[0], b[1], thick, color);
    }
}
