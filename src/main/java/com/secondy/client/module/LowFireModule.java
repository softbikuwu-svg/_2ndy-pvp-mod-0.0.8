package com.secondy.client.module;

import com.secondy.client.settings.NumberSetting;

/** Lowers the fire overlay so it does not cover the screen (hooked in mixin/FireOverlay*Mixin). */
public class LowFireModule extends Module {
    public final NumberSetting lower = add(new NumberSetting("Lower by", "lower", 40, 0, 100, 5, "%"));

    public LowFireModule() {
        super("Low Fire", "Fire overlay sits lower on the screen", Category.VISUAL);
    }

    /** Offset in blocks (view space). */
    public float offset() { return lower.getFloat() / 100f * 0.6f; }
}
