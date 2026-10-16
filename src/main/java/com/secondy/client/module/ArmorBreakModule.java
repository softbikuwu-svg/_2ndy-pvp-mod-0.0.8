package com.secondy.client.module;

import com.secondy.client.settings.BoolSetting;
import com.secondy.client.settings.ColorSetting;
import com.secondy.client.settings.NumberSetting;
import com.secondy.client.util.Sounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Предупреждает, когда прочность брони упала ниже заданного порога. */
public class ArmorBreakModule extends HudModule {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final String[] NAMES = { "Helmet", "Chestplate", "Leggings", "Boots" };

    private final NumberSetting threshold = add(new NumberSetting("Durability threshold", "threshold", 25, 5, 90, 5, "%"));
    private final BoolSetting head = add(new BoolSetting("Watch helmet", "head", true));
    private final BoolSetting chest = add(new BoolSetting("Watch chestplate", "chest", true));
    private final BoolSetting legs = add(new BoolSetting("Watch leggings", "legs", true));
    private final BoolSetting feet = add(new BoolSetting("Watch boots", "feet", true));
    private final BoolSetting blink = add(new BoolSetting("Blink", "blink", true));
    private final BoolSetting sound = add(new BoolSetting("Sound alert", "sound", true));
    private final NumberSetting volume = add(new NumberSetting("Alert volume", "volume", 70, 10, 100, 5, "%"));
    private final ColorSetting warnColor = add(new ColorSetting("Warning color", "warn_color", 0xFFD60A));
    private final ColorSetting critColor = add(new ColorSetting("Color below half the threshold", "crit_color", 0xFF453A));

    private final boolean[] warned = new boolean[4];

    public ArmorBreakModule() {
        super("Armor Break Alert", "Warns when armor durability is low", Category.PVP, 200, 200);
    }

    private boolean watching(int i) {
        return switch (i) {
            case 0 -> head.get();
            case 1 -> chest.get();
            case 2 -> legs.get();
            default -> feet.get();
        };
    }

    /** Прочность слота в процентах или -1, если брони нет или она не ломается. */
    private static int percent(MinecraftClient mc, int i) {
        ItemStack s = mc.player.getEquippedStack(SLOTS[i]);
        if (s.isEmpty() || !s.isDamageable() || s.getMaxDamage() <= 0) return -1;
        return Math.round(100f * (s.getMaxDamage() - s.getDamage()) / s.getMaxDamage());
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        for (int i = 0; i < 4; i++) {
            int pct = watching(i) ? percent(mc, i) : -1;
            if (pct >= 0 && pct <= threshold.getInt()) {
                if (!warned[i]) {
                    warned[i] = true;
                    if (sound.get()) Sounds.play(SoundEvents.BLOCK_ANVIL_LAND, 1.5f, volume.getInt() / 100f);
                }
            } else {
                warned[i] = false;
            }
        }
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null) return;
        boolean preview = mc.currentScreen instanceof ChatScreen;

        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (!watching(i)) continue;
            int pct = percent(mc, i);
            if (pct < 0 || pct > threshold.getInt()) continue;
            lines.add(String.format(Locale.ROOT, "%s: %d%%", NAMES[i], pct));
            colors.add(pct * 2 <= threshold.getInt() ? critColor.get() : warnColor.get());
        }
        if (lines.isEmpty()) {
            if (!preview) return;
            lines.add("Helmet: 20%");
            colors.add(warnColor.get());
        } else if (blink.get() && !preview && (System.currentTimeMillis() / 400L) % 2 == 0) {
            return;
        }

        var tr = mc.textRenderer;
        int w = 0;
        for (String s : lines) w = Math.max(w, tr.getWidth(s));
        width = w + 8;
        height = lines.size() * 10 + 4;
        background(ctx);
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawText(tr, lines.get(i), x + 4, y + 3 + i * 10, colors.get(i), true);
        }
    }
}
