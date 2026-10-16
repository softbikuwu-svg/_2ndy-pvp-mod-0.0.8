package com.secondy.client.module;

import com.google.gson.JsonObject;
import com.secondy.client.settings.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;
    private boolean enabled;
    /** Настройки модуля (показываются в меню по кнопке «Настр.»). */
    public final List<Setting> settings = new ArrayList<>();

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
    }

    public void toggle() { setEnabled(!enabled); }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick(MinecraftClient mc) {}

    /** Extra data that does not fit into plain settings (written into the module's settings object). */
    public void saveExtra(JsonObject o) {}

    public void loadExtra(JsonObject o) {}
}
