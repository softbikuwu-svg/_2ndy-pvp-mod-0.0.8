package com.secondy.client.module;

import com.secondy.client.settings.BoolSetting;
import com.secondy.client.settings.KeybindSetting;
import com.secondy.client.settings.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import org.lwjgl.glfw.GLFW;

/**
 * Freelook: while the key is held (Left Alt by default) the mouse turns only the camera,
 * the player keeps facing the same way. Released: the camera snaps back.
 * The camera / mouse hooks are in mixin/CameraMixin and mixin/EntityMixin.
 */
public class FreeLookModule extends Module {
    public final KeybindSetting key = add(new KeybindSetting("Key", "key", GLFW.GLFW_KEY_LEFT_ALT));
    public final BoolSetting thirdPerson = add(new BoolSetting("Third person view", "third", true));
    public final NumberSetting sens = add(new NumberSetting("Sensitivity", "sens", 1.0, 0.2, 2.0, 0.1, "x"));

    /** Read by the mixins. */
    public static boolean active;
    public static float yaw, pitch;

    private Perspective prev;

    public FreeLookModule() {
        super("Freelook", "Hold the key to look around without turning", Category.VISUAL);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) {
            stop(mc);
            return;
        }
        boolean want = mc.currentScreen == null && key.isDown();
        if (want && !active) {
            active = true;
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
            prev = mc.options.getPerspective();
            if (thirdPerson.get()) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        } else if (!want && active) {
            stop(mc);
        }
    }

    @Override
    public void onDisable() {
        stop(MinecraftClient.getInstance());
    }

    private void stop(MinecraftClient mc) {
        if (!active) return;
        active = false;
        if (prev != null && mc.options != null) mc.options.setPerspective(prev);
        prev = null;
    }

    /** Called from the mouse hook; deltas are the raw look deltas Minecraft would apply to the player. */
    public static void look(double dx, double dy, float sensitivity) {
        yaw += (float) (dx * 0.15 * sensitivity);
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) (dy * 0.15 * sensitivity)));
    }
}
