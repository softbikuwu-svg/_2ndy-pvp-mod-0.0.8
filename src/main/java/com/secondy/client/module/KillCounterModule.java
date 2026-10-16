package com.secondy.client.module;

import com.secondy.client.settings.ActionSetting;
import com.secondy.client.settings.BoolSetting;
import com.secondy.client.settings.ColorSetting;
import com.secondy.client.settings.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Счётчик убийств, серии, смертей и K/D. Убийство = цель умерла в течение 8 секунд после твоего удара. */
public class KillCounterModule extends HudModule {
    private static final long KILL_WINDOW_MS = 8000L;

    private final ModeSetting layout = add(new ModeSetting("Layout", "layout", 0, "Column", "Row"));
    private final BoolSetting showKills = add(new BoolSetting("Kills", "show_kills", true));
    private final BoolSetting showStreak = add(new BoolSetting("Kill streak", "show_streak", true));
    private final BoolSetting showDeaths = add(new BoolSetting("Deaths", "show_deaths", true));
    private final BoolSetting showKd = add(new BoolSetting("K/D", "show_kd", true));
    private final ColorSetting textColor = add(new ColorSetting("Text color", "text_color", 0xFFFFFF));
    private final ColorSetting streakColor = add(new ColorSetting("Streak color", "streak_color", 0xFFD60A));
    @SuppressWarnings("unused")
    private final ActionSetting reset = add(new ActionSetting("Reset counters", this::resetAll));

    private int kills, deaths, streak;
    private LivingEntity lastTarget;
    private long lastHitAt;
    private boolean wasDead;

    public KillCounterModule() {
        super("Kill Counter", "Kills, streak, deaths and K/D", Category.PVP, 200, 160);
    }

    private void resetAll() {
        kills = 0;
        deaths = 0;
        streak = 0;
        lastTarget = null;
    }

    /** Вызывается из SecondyClient при каждом ударе по существу. */
    public void onAttack(Entity e) {
        if (!isEnabled()) return;
        if (e instanceof LivingEntity le) {
            lastTarget = le;
            lastHitAt = System.currentTimeMillis();
        }
    }

    @Override
    public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (p == null) return;

        boolean dead = p.isDead();
        if (dead && !wasDead) {
            deaths++;
            streak = 0;
        }
        wasDead = dead;

        if (lastTarget != null) {
            long dt = System.currentTimeMillis() - lastHitAt;
            if (lastTarget.isDead()) {
                if (dt <= KILL_WINDOW_MS) {
                    kills++;
                    streak++;
                }
                lastTarget = null;
            } else if (dt > KILL_WINDOW_MS) {
                lastTarget = null;
            }
        }
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        List<String> parts = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int tc = textColor.get();
        if (showKills.get()) { parts.add("Kills: " + kills); colors.add(tc); }
        if (showStreak.get()) { parts.add("Streak: " + streak); colors.add(streakColor.get()); }
        if (showDeaths.get()) { parts.add("Deaths: " + deaths); colors.add(tc); }
        if (showKd.get()) {
            double kd = deaths == 0 ? kills : (double) kills / deaths;
            parts.add(String.format(Locale.ROOT, "K/D: %.2f", kd));
            colors.add(tc);
        }
        if (parts.isEmpty()) return;

        var tr = mc.textRenderer;
        if (layout.index() == 1) {
            int w = 0;
            for (String s : parts) w += tr.getWidth(s) + 10;
            width = w - 2;
            height = 14;
            background(ctx);
            int cx = x + 4;
            for (int i = 0; i < parts.size(); i++) {
                ctx.drawText(tr, parts.get(i), cx, y + 3, colors.get(i), true);
                cx += tr.getWidth(parts.get(i)) + 10;
            }
        } else {
            int w = 0;
            for (String s : parts) w = Math.max(w, tr.getWidth(s));
            width = w + 8;
            height = parts.size() * 10 + 4;
            background(ctx);
            for (int i = 0; i < parts.size(); i++) {
                ctx.drawText(tr, parts.get(i), x + 4, y + 3 + i * 10, colors.get(i), true);
            }
        }
    }
}
