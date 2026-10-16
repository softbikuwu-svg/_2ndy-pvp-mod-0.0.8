package com.secondy.client.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.secondy.client.settings.ActionSetting;
import com.secondy.client.settings.CycleSetting;
import com.secondy.client.settings.ModeSetting;
import com.secondy.client.settings.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Item Customizer: position, rotation and size of the item in your hand, per item.
 * The "All items" profile applies to everything that has no profile of its own; hold an item and press
 * "Add held item" to give it a separate profile. Sliders always edit the selected profile.
 */
public class ItemCustomizerModule extends Module {
    private static final String ALL = "*";
    // profile layout: x, y, z (1/100 block), rotX, rotY, rotZ (degrees), scale (%)
    private static final int N = 7;

    private final Map<String, float[]> profiles = new LinkedHashMap<>();
    private String selected = ALL;

    @SuppressWarnings("unused")
    private final CycleSetting profileRow = add(new CycleSetting("Profile", "profile", this::selectedLabel, this::nextProfile));
    @SuppressWarnings("unused")
    private final ActionSetting addHeld = add(new ActionSetting("Add held item", this::addHeldItem));
    @SuppressWarnings("unused")
    private final ActionSetting remove = add(new ActionSetting("Remove profile", this::removeSelected));
    private final ModeSetting hands = add(new ModeSetting("Hands", "hands", 0, "Both", "Main hand", "Off hand"));
    private final NumberSetting[] sl = {
            add(new NumberSetting("Position X", "px", 0, -100, 100, 1, "")),
            add(new NumberSetting("Position Y", "py", 0, -100, 100, 1, "")),
            add(new NumberSetting("Position Z", "pz", 0, -100, 100, 1, "")),
            add(new NumberSetting("Rotation X", "rx", 0, -180, 180, 1, "\u00b0")),
            add(new NumberSetting("Rotation Y", "ry", 0, -180, 180, 1, "\u00b0")),
            add(new NumberSetting("Rotation Z", "rz", 0, -180, 180, 1, "\u00b0")),
            add(new NumberSetting("Size", "scale", 100, 30, 160, 1, "%")),
    };
    @SuppressWarnings("unused")
    private final ActionSetting reset = add(new ActionSetting("Reset profile", this::resetSelected));

    public ItemCustomizerModule() {
        super("Item Customizer", "Position, rotation and size of held items", Category.VISUAL);
        profiles.put(ALL, identity());
        loadSliders();
    }

    // ───────────── profiles ─────────────

    private static float[] identity() {
        return new float[]{ 0, 0, 0, 0, 0, 0, 100 };
    }

    private float[] cur() {
        return profiles.computeIfAbsent(selected, k -> identity());
    }

    private void storeSliders() {
        float[] p = cur();
        for (int i = 0; i < N; i++) p[i] = (float) sl[i].get();
    }

    private void loadSliders() {
        float[] p = cur();
        for (int i = 0; i < N; i++) sl[i].set(p[i]);
    }

    private String selectedLabel() {
        return label(selected);
    }

    private static String label(String id) {
        if (ALL.equals(id)) return "All items";
        return id.startsWith("minecraft:") ? id.substring(10) : id;
    }

    private void select(String id) {
        storeSliders();
        selected = id;
        loadSliders();
    }

    private void nextProfile() {
        List<String> keys = new ArrayList<>(profiles.keySet());
        int i = keys.indexOf(selected);
        select(keys.get((i + 1) % keys.size()));
    }

    private void addHeldItem() {
        var p = MinecraftClient.getInstance().player;
        if (p == null) return;
        ItemStack s = p.getMainHandStack();
        if (s.isEmpty()) return;
        String id = Registries.ITEM.getId(s.getItem()).toString();
        storeSliders();
        profiles.computeIfAbsent(id, k -> identity());
        select(id);
    }

    private void removeSelected() {
        if (ALL.equals(selected)) return;
        profiles.remove(selected);
        selected = ALL;
        loadSliders();
    }

    private void resetSelected() {
        float[] p = cur();
        float[] id = identity();
        System.arraycopy(id, 0, p, 0, N);
        loadSliders();
    }

    @Override
    public void onTick(MinecraftClient mc) {
        storeSliders();
    }

    // ───────────── persistence ─────────────

    @Override
    public void saveExtra(JsonObject o) {
        storeSliders();
        JsonObject all = new JsonObject();
        for (Map.Entry<String, float[]> e : profiles.entrySet()) {
            JsonArray a = new JsonArray();
            for (float v : e.getValue()) a.add(v);
            all.add(e.getKey(), a);
        }
        o.add("profiles", all);
        o.addProperty("selected_profile", selected);
    }

    @Override
    public void loadExtra(JsonObject o) {
        if (o.has("profiles") && o.get("profiles").isJsonObject()) {
            profiles.clear();
            for (Map.Entry<String, com.google.gson.JsonElement> e : o.getAsJsonObject("profiles").entrySet()) {
                float[] p = identity();
                JsonArray a = e.getValue().getAsJsonArray();
                for (int i = 0; i < N && i < a.size(); i++) p[i] = a.get(i).getAsFloat();
                profiles.put(e.getKey(), p);
            }
            if (!profiles.containsKey(ALL)) profiles.put(ALL, identity());
        }
        selected = o.has("selected_profile") ? o.get("selected_profile").getAsString() : ALL;
        if (!profiles.containsKey(selected)) selected = ALL;
        loadSliders();
    }

    // ───────────── applying (called from util/HandHooks) ─────────────

    /**
     * Transform for this item, or null if nothing should change.
     * Layout: offset x, y, z (blocks, already mirrored for the side), rotation x, y, z (degrees, mirrored), scale.
     */
    public float[] xform(ItemStack stack, Hand hand, int side) {
        if (hands.index() == 1 && hand != Hand.MAIN_HAND) return null;
        if (hands.index() == 2 && hand != Hand.OFF_HAND) return null;
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        float[] p = profiles.get(id);
        if (p == null) p = profiles.get(ALL);
        if (p == null) return null;
        if (p == cur()) {
            // keep the live preview in sync with the sliders while the menu is open
            storeSliders();
        }
        float sc = p[6] / 100f;
        if (p[0] == 0 && p[1] == 0 && p[2] == 0 && p[3] == 0 && p[4] == 0 && p[5] == 0 && sc == 1f) return null;
        return new float[]{ p[0] / 100f * side, p[1] / 100f, p[2] / 100f, p[3], p[4] * side, p[5] * side, sc };
    }

    /** Which side of the screen the given hand is on. */
    public static int sideOf(MinecraftClient mc, Hand hand) {
        boolean rightMain = mc.options.getMainArm().getValue() == Arm.RIGHT;
        return (hand == Hand.MAIN_HAND) == rightMain ? 1 : -1;
    }
}
