package com.secondy.client.settings;

import com.google.gson.JsonObject;

import java.util.Locale;

/** Числовая настройка с ползунком. */
public class NumberSetting extends Setting {
    public final double min, max, step;
    public final String suffix;
    private double value;

    public NumberSetting(String name, String key, double def, double min, double max, double step, String suffix) {
        super(name, key);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        this.value = clamp(def);
    }

    private double clamp(double v) {
        if (step > 0) v = Math.round((v - min) / step) * step + min;
        return Math.max(min, Math.min(max, v));
    }

    public double get() { return value; }

    public int getInt() { return (int) Math.round(value); }

    public float getFloat() { return (float) value; }

    public void set(double v) { value = clamp(v); }

    /** Доля 0..1 для ползунка. */
    public float fraction() { return (float) ((value - min) / (max - min)); }

    public void setFraction(float f) { set(min + Math.max(0f, Math.min(1f, f)) * (max - min)); }

    public String display() {
        String s = step >= 1 ? String.valueOf(getInt()) : String.format(Locale.ROOT, "%.1f", value);
        return s + suffix;
    }

    @Override public void save(JsonObject o) { o.addProperty(key, value); }

    @Override public void load(JsonObject o) {
        if (o.has(key)) value = clamp(o.get(key).getAsDouble());
    }
}
