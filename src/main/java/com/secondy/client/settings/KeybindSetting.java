package com.secondy.client.settings;

import com.google.gson.JsonObject;
import com.secondy.client.util.Input;

/**
 * Rebindable key (keyboard key or mouse button). Click the row in the menu, press a key to bind it,
 * Esc cancels, Backspace resets to the default. The code is a GLFW key code, or Input.MOUSE_BASE + button.
 */
public class KeybindSetting extends Setting {
    public final int defaultCode;
    private int code;

    public KeybindSetting(String name, String key, int def) {
        super(name, key);
        this.defaultCode = def;
        this.code = def;
    }

    public int get() { return code; }

    public void set(int c) { code = c; }

    public void reset() { code = defaultCode; }

    /** True while the bound key / button is held. */
    public boolean isDown() { return Input.down(code); }

    public String display() { return Input.keyName(code); }

    @Override public void save(JsonObject o) { o.addProperty(key, code); }

    @Override public void load(JsonObject o) {
        if (o.has(key)) code = o.get(key).getAsInt();
    }
}
