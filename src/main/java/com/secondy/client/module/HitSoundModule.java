package com.secondy.client.module;

import com.secondy.client.settings.ActionSetting;
import com.secondy.client.settings.BoolSetting;
import com.secondy.client.settings.FileSetting;
import com.secondy.client.settings.ModeSetting;
import com.secondy.client.settings.NumberSetting;
import com.secondy.client.util.CustomSounds;
import com.secondy.client.util.Sounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Свой звук при каждом твоём ударе по существу (слышишь только ты).
 * Источник: встроенный звук Minecraft или свой файл .wav / .ogg из папки config/secondy_client/sounds.
 */
public class HitSoundModule extends Module {
    private final ModeSetting source = add(new ModeSetting("Source", "source", 0, "Built-in", "Custom file"));
    private final ModeSetting sound = add(new ModeSetting("Sound", "sound", 0,
            "XP orb", "Arrow", "Anvil", "Level-up"));
    private final FileSetting file = add(new FileSetting("File (.wav/.ogg)", "file", CustomSounds::list)
            .onChange(this::preview));
    private final NumberSetting volume = add(new NumberSetting("Volume", "volume", 80, 10, 100, 5, "%"));
    private final NumberSetting pitch = add(new NumberSetting("Pitch", "pitch", 1.2, 0.5, 2.0, 0.1, ""));
    private final BoolSetting playersOnly = add(new BoolSetting("Players only", "players_only", false));

    @SuppressWarnings("unused")
    private final ActionSetting test = add(new ActionSetting("Play sound", this::preview));
    @SuppressWarnings("unused")
    private final ActionSetting refresh = add(new ActionSetting("Refresh file list", file::refresh));
    @SuppressWarnings("unused")
    private final ActionSetting open = add(new ActionSetting("Open sounds folder", CustomSounds::openFolder));

    /** Какой файл уже заказан на предзагрузку (чтобы не дёргать поток каждый тик). */
    private String preloaded = "";

    public HitSoundModule() {
        super("Hit Sound", "Custom sound when you hit a target (custom files supported)", Category.PVP);
    }

    private boolean useFile() {
        return source.index() == 1 && file.current() != null;
    }

    /** Ошибки декодера приходят из фонового потока — показываем их в чате уже из основного. */
    private static Consumer<String> errors() {
        return msg -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) return;
            mc.execute(() -> {
                if (mc.inGameHud != null) {
                    mc.inGameHud.getChatHud().addMessage(Text.literal("[_2ndy] " + msg));
                }
            });
        };
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!useFile()) return;
        String f = file.current();
        if (!f.equals(preloaded)) {
            preloaded = f;
            CustomSounds.preload(f, volume.getInt(), errors());
        }
    }

    @Override
    public void onDisable() {
        preloaded = "";
    }

    private void playSound() {
        if (useFile()) {
            CustomSounds.play(file.current(), volume.getInt(), errors());
            return;
        }
        SoundEvent ev = switch (sound.index()) {
            case 1 -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
            case 2 -> SoundEvents.BLOCK_ANVIL_LAND;
            case 3 -> SoundEvents.ENTITY_PLAYER_LEVELUP;
            default -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        };
        Sounds.play(ev, pitch.getFloat(), volume.getInt() / 100f);
    }

    /** Кнопка «Play sound» и автопревью при выборе файла (работает, даже если модуль выключен). */
    private void preview() {
        playSound();
    }

    /** Вызывается из SecondyClient при ударе. Звук играет в момент удара (даже если урон не прошёл из-за кулдауна). */
    public void onAttack(Entity e) {
        if (!isEnabled() || !(e instanceof LivingEntity)) return;
        if (playersOnly.get() && !(e instanceof PlayerEntity)) return;
        playSound();
    }
}
