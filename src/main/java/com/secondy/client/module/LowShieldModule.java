package com.secondy.client.module;

import com.secondy.client.settings.NumberSetting;

/** Lowers the shield in first person (applied in util/HandHooks). */
public class LowShieldModule extends Module {
    public final NumberSetting lower = add(new NumberSetting("Lower by", "lower", 25, 0, 100, 5, "%"));

    public LowShieldModule() {
        super("Low Shield", "Shield sits lower in first person", Category.VISUAL);
    }

    public float offset() { return lower.getFloat() / 100f * 0.6f; }
}
