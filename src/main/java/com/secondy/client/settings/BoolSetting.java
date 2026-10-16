package com.secondy.client.settings;

import com.google.gson.JsonObject;

/** Переключатель вкл/выкл. */
public class BoolSetting extends Setting {
    private boolean value;

    public BoolSetting(String name, String key, boolean def) {
        super(name, key);
        this.value = def;
    }

    public boolean get() { return value; }

    public void set(boolean v) { value = v; }

    public void toggle() { value = !value; }

    @Override public void save(JsonObject o) { o.addProperty(key, value); }

    @Override public void load(JsonObject o) {
        if (o.has(key)) value = o.get(key).getAsBoolean();
    }
}
