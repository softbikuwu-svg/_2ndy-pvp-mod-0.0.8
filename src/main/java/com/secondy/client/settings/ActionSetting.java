package com.secondy.client.settings;

import com.google.gson.JsonObject;

/** Кнопка в настройках (например, «Сбросить»). В конфиг ничего не пишет. */
public class ActionSetting extends Setting {
    private final Runnable action;

    public ActionSetting(String name, Runnable action) {
        super(name, "action_" + name);
        this.action = action;
    }

    public void run() { action.run(); }

    @Override public void save(JsonObject o) {}

    @Override public void load(JsonObject o) {}
}
