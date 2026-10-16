package com.secondy.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;

/** Проигрывание звуков интерфейса (слышишь только ты). */
public final class Sounds {
    private Sounds() {}

    public static void play(SoundEvent event, float pitch, float volume) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;
        mc.getSoundManager().play(PositionedSoundInstance.ui(event, pitch, volume));
    }
}
