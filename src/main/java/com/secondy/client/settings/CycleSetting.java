package com.secondy.client.settings;

import com.google.gson.JsonObject;

import java.util.function.Supplier;

/** A row that shows a live value and runs an action when clicked (used for dynamic lists). Not saved. */
public class CycleSetting extends Setting {
    private final Supplier<String> display;
    private final Runnable next;

    public CycleSetting(String name, String key, Supplier<String> display, Runnable next) {
        super(name, key);
        this.display = display;
        this.next = next;
    }

    public String display() { return display.get(); }

    public void next() { next.run(); }

    @Override public void save(JsonObject o) { }

    @Override public void load(JsonObject o) { }
}
