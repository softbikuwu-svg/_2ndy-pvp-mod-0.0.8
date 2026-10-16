package com.secondy.client.settings;

import com.google.gson.JsonObject;

/** Базовый класс настройки модуля. {@code name} — подпись в меню, {@code key} — ключ в конфиге. */
public abstract class Setting {
    public final String name;
    public final String key;

    protected Setting(String name, String key) {
        this.name = name;
        this.key = key;
    }

    public abstract void save(JsonObject o);

    public abstract void load(JsonObject o);
}
