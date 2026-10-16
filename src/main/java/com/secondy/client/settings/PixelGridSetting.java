package com.secondy.client.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * A small square pixel image (used for the hand-drawn crosshair).
 * Each cell is 0 (empty) or an opaque 0xFFRRGGBB color. Saved to the config as a flat array of ints.
 */
public class PixelGridSetting extends Setting {
    public static final int N = 15;

    private final int[] cells = new int[N * N];
    private int[] defaults;

    public PixelGridSetting(String name, String key, int[] defaults) {
        super(name, key);
        this.defaults = defaults.length == N * N ? defaults.clone() : new int[N * N];
        reset();
    }

    public int get(int x, int y) {
        if (x < 0 || y < 0 || x >= N || y >= N) return 0;
        return cells[y * N + x];
    }

    /** Sets a cell; pass 0 to erase. Out-of-range coordinates are ignored. */
    public void set(int x, int y, int argb) {
        if (x < 0 || y < 0 || x >= N || y >= N) return;
        cells[y * N + x] = argb == 0 ? 0 : (0xFF000000 | argb);
    }

    public void clear() {
        java.util.Arrays.fill(cells, 0);
    }

    public void reset() {
        System.arraycopy(defaults, 0, cells, 0, cells.length);
    }

    /** Replaces the whole image (ignored if the array has the wrong size). */
    public void setAll(int[] src) {
        if (src == null || src.length != cells.length) return;
        for (int i = 0; i < cells.length; i++) cells[i] = src[i] == 0 ? 0 : (0xFF000000 | src[i]);
    }

    public boolean isEmpty() {
        for (int c : cells) if (c != 0) return false;
        return true;
    }

    @Override
    public void save(JsonObject o) {
        JsonArray arr = new JsonArray();
        for (int c : cells) arr.add(new JsonPrimitive(c));
        o.add(key, arr);
    }

    @Override
    public void load(JsonObject o) {
        if (!o.has(key) || !o.get(key).isJsonArray()) return;
        JsonArray arr = o.getAsJsonArray(key);
        if (arr.size() != cells.length) return;
        for (int i = 0; i < cells.length; i++) {
            JsonElement e = arr.get(i);
            int v = e.getAsInt();
            cells[i] = v == 0 ? 0 : (0xFF000000 | v);
        }
    }
}
