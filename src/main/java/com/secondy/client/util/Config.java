package com.secondy.client.util;

import com.secondy.client.SecondyClient;
import com.secondy.client.module.HudModule;
import com.secondy.client.module.Module;
import com.secondy.client.module.ModuleManager;
import com.secondy.client.settings.Setting;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** Имя модуля-менеджера пресетов: его самого в пресеты не пишем. */
    public static final String PRESETS_MODULE = "Presets";

    private Config() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("secondy_client.json");
    }

    private static Path presetFile(int slot) {
        return FabricLoader.getInstance().getConfigDir().resolve("secondy_client_preset_" + slot + ".json");
    }

    // ───────────── Основной конфиг ─────────────

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            apply(JsonParser.parseString(Files.readString(f)).getAsJsonObject());
        } catch (Exception e) {
            SecondyClient.LOGGER.error("Failed to load config", e);
        }
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(snapshot(true)));
        } catch (Exception e) {
            SecondyClient.LOGGER.error("Failed to save config", e);
        }
    }

    // ───────────── Пресеты (слоты) ─────────────

    public static boolean savePreset(int slot) {
        try {
            Files.writeString(presetFile(slot), GSON.toJson(snapshot(false)));
            return true;
        } catch (Exception e) {
            SecondyClient.LOGGER.error("Failed to save preset " + slot, e);
            return false;
        }
    }

    /** @return false, если пресета в этом слоте нет или он не читается. */
    public static boolean loadPreset(int slot) {
        try {
            Path f = presetFile(slot);
            if (!Files.exists(f)) return false;
            apply(JsonParser.parseString(Files.readString(f)).getAsJsonObject());
            save();
            return true;
        } catch (Exception e) {
            SecondyClient.LOGGER.error("Failed to load preset " + slot, e);
            return false;
        }
    }

    public static boolean presetExists(int slot) {
        return Files.exists(presetFile(slot));
    }

    public static boolean deletePreset(int slot) {
        try {
            return Files.deleteIfExists(presetFile(slot));
        } catch (Exception e) {
            SecondyClient.LOGGER.error("Failed to delete preset " + slot, e);
            return false;
        }
    }

    // ───────────── Общая логика ─────────────

    private static JsonObject snapshot(boolean includePresetsModule) {
        JsonObject root = new JsonObject();
        JsonObject g = new JsonObject();
        g.addProperty("accent", Theme.accent & 0xFFFFFF);
        g.addProperty("hudAccent", Theme.hudAccent);
        root.add("_gui", g);
        for (Module m : ModuleManager.ALL) {
            if (!includePresetsModule && m.name.equals(PRESETS_MODULE)) continue;
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            if (m instanceof HudModule h) {
                o.addProperty("x", h.x);
                o.addProperty("y", h.y);
                o.addProperty("scale", h.scale);
                o.addProperty("background", h.showBackground);
            }
            if (!m.settings.isEmpty()) {
                JsonObject so = new JsonObject();
                for (Setting st : m.settings) st.save(so);
                m.saveExtra(so);
                o.add("settings", so);
            }
            root.add(m.name, o);
        }
        return root;
    }

    private static void apply(JsonObject root) {
        if (root.has("_gui")) {
            JsonObject g = root.getAsJsonObject("_gui");
            if (g.has("accent")) Theme.accent = 0xFF000000 | g.get("accent").getAsInt();
            if (g.has("hudAccent")) Theme.hudAccent = g.get("hudAccent").getAsBoolean();
        }
        for (Module m : ModuleManager.ALL) {
            String key = m.name;
            // The optimizer used to be saved under its old Russian name.
            if (!root.has(key) && key.equals("Optimizer") && root.has("\u041e\u043f\u0442\u0438\u043c\u0438\u0437\u0430\u0446\u0438\u044f")) {
                key = "\u041e\u043f\u0442\u0438\u043c\u0438\u0437\u0430\u0446\u0438\u044f";
            }
            if (!root.has(key)) continue;
            JsonObject o = root.getAsJsonObject(key);
            if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
            if (m instanceof HudModule h) {
                if (o.has("x")) h.x = o.get("x").getAsInt();
                if (o.has("y")) h.y = o.get("y").getAsInt();
                if (o.has("scale")) h.scale = Math.max(0.5f, Math.min(3f, o.get("scale").getAsFloat()));
                if (o.has("background")) h.showBackground = o.get("background").getAsBoolean();
            }
            if (o.has("settings") && o.get("settings").isJsonObject()) {
                JsonObject so = o.getAsJsonObject("settings");
                for (Setting st : m.settings) {
                    try { st.load(so); } catch (Exception ignored) { }
                }
                try { m.loadExtra(so); } catch (Exception ignored) { }
            }
        }
    }
}
