package com.secondy.client.settings;

import com.google.gson.JsonObject;

/** Цвет: RGB-ползунки, готовые цвета и режим «радуга». */
public class ColorSetting extends Setting {
    private int rgb;
    public boolean rainbow;

    public ColorSetting(String name, String key, int defRgb) {
        super(name, key);
        this.rgb = defRgb & 0xFFFFFF;
    }

    /** Базовый цвет (без радуги), 0xRRGGBB. */
    public int rgb() { return rgb; }

    public void setRgb(int v) { rgb = v & 0xFFFFFF; }

    /** Текущий цвет с альфой 255: радужный, если включена радуга. */
    public int get() { return 0xFF000000 | current(0f); }

    /** Текущий цвет с заданной альфой (0..255). */
    public int get(int alpha) { return (Math.max(0, Math.min(255, alpha)) << 24) | current(0f); }

    /** Текущий цвет 0xRRGGBB (радуга учитывается, offset сдвигает оттенок). */
    public int current(float offset) {
        if (!rainbow) return rgb;
        float h = ((System.currentTimeMillis() % 4000L) / 4000f + offset) % 1f;
        return java.awt.Color.HSBtoRGB(h, 0.8f, 1f) & 0xFFFFFF;
    }

    @Override public void save(JsonObject o) {
        o.addProperty(key, rgb);
        o.addProperty(key + "_rainbow", rainbow);
    }

    @Override public void load(JsonObject o) {
        if (o.has(key)) rgb = o.get(key).getAsInt() & 0xFFFFFF;
        if (o.has(key + "_rainbow")) rainbow = o.get(key + "_rainbow").getAsBoolean();
    }
}
