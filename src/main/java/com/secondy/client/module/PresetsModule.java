package com.secondy.client.module;

import com.secondy.client.settings.ActionSetting;
import com.secondy.client.settings.ModeSetting;
import com.secondy.client.util.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Менеджер пресетов: сохраняет и загружает все настройки модулей в 5 слотов. */
public class PresetsModule extends Module {
    private static final int SLOTS = 5;

    private final ModeSetting slot = add(new ModeSetting("Slot", "slot", 0, "1", "2", "3", "4", "5"));
    @SuppressWarnings("unused")
    private final ActionSetting save = add(new ActionSetting("Save current settings to slot", this::saveSlot));
    @SuppressWarnings("unused")
    private final ActionSetting load = add(new ActionSetting("Load settings from slot", this::loadSlot));
    @SuppressWarnings("unused")
    private final ActionSetting delete = add(new ActionSetting("Clear slot", this::deleteSlot));

    public PresetsModule() {
        super(Config.PRESETS_MODULE, "Save and load all settings (5 slots)", Category.UTILITY);
        setEnabled(true);
        refreshLabels();
    }

    private int slotNumber() {
        return slot.index() + 1;
    }

    /** Подписи слотов показывают, какие из них заняты. */
    private void refreshLabels() {
        for (int i = 0; i < SLOTS; i++) {
            slot.options[i] = (i + 1) + (Config.presetExists(i + 1) ? " (used)" : " (empty)");
        }
    }

    private void saveSlot() {
        int n = slotNumber();
        say(Config.savePreset(n) ? "Preset " + n + " saved" : "Failed to save preset " + n);
        refreshLabels();
    }

    private void loadSlot() {
        int n = slotNumber();
        if (!Config.presetExists(n)) {
            say("Slot " + n + " is empty");
            return;
        }
        say(Config.loadPreset(n) ? "Preset " + n + " loaded" : "Failed to load preset " + n);
    }

    private void deleteSlot() {
        int n = slotNumber();
        if (!Config.presetExists(n)) {
            say("Slot " + n + " is already empty");
            return;
        }
        say(Config.deletePreset(n) ? "Slot " + n + " cleared" : "Failed to clear slot " + n);
        refreshLabels();
    }

    private static void say(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.inGameHud != null) {
            mc.inGameHud.getChatHud().addMessage(Text.literal("[_2ndy] " + text));
        }
    }
}
