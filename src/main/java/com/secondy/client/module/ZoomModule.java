package com.secondy.client.module;

import com.secondy.client.settings.KeybindSetting;
import com.secondy.client.settings.NumberSetting;
import com.secondy.client.util.Input;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class ZoomModule extends Module {
    /** Rebindable in the module settings (click the row, press any key or mouse button). */
    public final KeybindSetting key = add(new KeybindSetting("Key", "key", GLFW.GLFW_KEY_C));
    public final NumberSetting fov = add(new NumberSetting("Zoom FOV", "fov", 30, 5, 60, 1, ""));

    private boolean zooming = false;
    private int saved = 70;

    public ZoomModule() {
        super("Zoom", "Zoom: hold the key (rebind in settings)", Category.UTILITY);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        boolean want = mc.currentScreen == null && key.isDown();
        if (want && !zooming) {
            saved = mc.options.getFov().getValue();
            mc.options.getFov().setValue(fov.getInt());
            zooming = true;
        } else if (want) {
            // follow the slider while zooming
            mc.options.getFov().setValue(fov.getInt());
        } else if (!want && zooming) {
            reset(mc);
        }
    }

    @Override
    public void onDisable() {
        if (zooming) reset(MinecraftClient.getInstance());
    }

    private void reset(MinecraftClient mc) {
        mc.options.getFov().setValue(saved);
        zooming = false;
    }
}
