package com.secondy.client.settings;

import com.google.gson.JsonObject;

/** Выбор одного варианта из списка (клик по строке — следующий вариант). */
public class ModeSetting extends Setting {
    public final String[] options;
    private int index;

    public ModeSetting(String name, String key, int def, String... options) {
        super(name, key);
        this.options = options;
        this.index = Math.max(0, Math.min(options.length - 1, def));
    }

    public int index() { return index; }

    public String get() { return options[index]; }

    public void next() { index = (index + 1) % options.length; }

    public void set(int i) { index = Math.max(0, Math.min(options.length - 1, i)); }

    @Override public void save(JsonObject o) { o.addProperty(key, index); }

    @Override public void load(JsonObject o) {
        if (o.has(key)) index = Math.max(0, Math.min(options.length - 1, o.get(key).getAsInt()));
    }
}
