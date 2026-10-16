package com.secondy.client.module;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.secondy.client.settings.ActionSetting;
import com.secondy.client.settings.BoolSetting;
import com.secondy.client.settings.ModeSetting;
import com.secondy.client.settings.NumberSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Оптимизация через штатные настройки игры — без модов-движков и без правки рендера.
 * Каждый пункт включается отдельно. Исходные значения игрока запоминаются и возвращаются
 * при выключении модуля (и при выходе из игры), так что options.txt не портится.
 * <p>
 * По умолчанию картинку не затрагивает ничего, кроме лимита FPS в свёрнутом окне.
 * Пункты, которые чуть меняют картинку, выключены и подписаны в описании профилей.
 */
public class OptimizerModule extends Module {
    // ── Без потери картинки ──
    private final BoolSetting bgOn = add(new BoolSetting("Background FPS limit", "bg_limit", true));
    private final NumberSetting bgFps = add(new NumberSetting("Background FPS", "bg_fps", 20, 10, 60, 10, ""));
    private final BoolSetting uncap = add(new BoolSetting("Uncap FPS", "uncap", false));
    private final BoolSetting vsyncOff = add(new BoolSetting("Disable VSync", "vsync_off", false));
    // ── Чуть меняют картинку ──
    private final BoolSetting shadowsOff = add(new BoolSetting("No entity shadows", "shadows_off", false));
    private final ModeSetting particleMode = add(new ModeSetting("Particles", "particles", 0,
            "Keep", "Decreased", "Minimal"));
    private final ModeSetting entMode = add(new ModeSetting("Entity distance", "ent_mode", 0,
            "Keep", "75%", "50%"));
    private final BoolSetting viewOn = add(new BoolSetting("Limit render distance", "view_on", false));
    private final NumberSetting viewVal = add(new NumberSetting("Max distance", "view_val", 12, 6, 32, 1, " chunks"));
    private final BoolSetting biomeOn = add(new BoolSetting("Biome blend", "biome_on", false));
    private final NumberSetting biomeVal = add(new NumberSetting("Blend radius", "biome_val", 1, 0, 7, 1, ""));

    @SuppressWarnings("unused")
    private final ActionSetting pSafe = add(new ActionSetting("Profile: safe (visuals unchanged)", () -> profile(1)));
    @SuppressWarnings("unused")
    private final ActionSetting pBalance = add(new ActionSetting("Profile: balanced", () -> profile(2)));
    @SuppressWarnings("unused")
    private final ActionSetting pMax = add(new ActionSetting("Profile: max FPS", () -> profile(3)));

    /** Исходные значения опций игрока, которые мы сейчас подменяем (ключ → значение строкой). */
    private final Map<String, String> backup = new LinkedHashMap<>();
    private final List<Opt<?>> all = new ArrayList<>();

    private final Opt<Integer> maxFps = opt("max_fps", GameOptions::getMaxFps, String::valueOf, Integer::parseInt);
    private final Opt<Boolean> vsync = opt("vsync", GameOptions::getEnableVsync, String::valueOf, Boolean::parseBoolean);
    private final Opt<Boolean> shadows = opt("shadows", GameOptions::getEntityShadows, String::valueOf, Boolean::parseBoolean);
    private final Opt<ParticlesMode> particles = opt("particles", GameOptions::getParticles, ParticlesMode::name, ParticlesMode::valueOf);
    private final Opt<Double> entDist = opt("ent_dist", GameOptions::getEntityDistanceScaling, String::valueOf, Double::parseDouble);
    private final Opt<Integer> viewDist = opt("view_dist", GameOptions::getViewDistance, String::valueOf, Integer::parseInt);
    private final Opt<Integer> biome = opt("biome", GameOptions::getBiomeBlendRadius, String::valueOf, Integer::parseInt);

    private int tick;

    public OptimizerModule() {
        super("Optimizer", "More FPS through game settings, without losing visuals", Category.OPTIMIZE);
        loadBackup();
    }

    // ───────────── Подмена опций с запоминанием оригинала ─────────────

    private <T> Opt<T> opt(String key, Function<GameOptions, SimpleOption<T>> getter,
                           Function<T, String> enc, Function<String, T> dec) {
        Opt<T> o = new Opt<>(key, getter, enc, dec);
        all.add(o);
        return o;
    }

    private final class Opt<T> {
        final String key;
        final Function<GameOptions, SimpleOption<T>> getter;
        final Function<T, String> enc;
        final Function<String, T> dec;

        Opt(String key, Function<GameOptions, SimpleOption<T>> getter, Function<T, String> enc, Function<String, T> dec) {
            this.key = key;
            this.getter = getter;
            this.enc = enc;
            this.dec = dec;
        }

        /** Значение игрока: из бэкапа, если мы его уже подменили, иначе текущее. */
        T original(GameOptions o) {
            String s = backup.get(key);
            if (s != null) {
                try {
                    return dec.apply(s);
                } catch (RuntimeException ignored) { }
            }
            return getter.apply(o).getValue();
        }

        /** target != null — выставить; null — вернуть оригинал (если мы его меняли). */
        void drive(GameOptions o, T target) {
            SimpleOption<T> opt = getter.apply(o);
            if (target == null) {
                restore(o);
                return;
            }
            if (!backup.containsKey(key)) {
                backup.put(key, enc.apply(opt.getValue()));
                saveBackup();
            }
            if (!target.equals(opt.getValue())) opt.setValue(target);
        }

        void restore(GameOptions o) {
            String s = backup.remove(key);
            if (s == null) return;
            saveBackup();
            try {
                SimpleOption<T> opt = getter.apply(o);
                T old = dec.apply(s);
                if (!old.equals(opt.getValue())) opt.setValue(old);
            } catch (RuntimeException ignored) { }
        }
    }

    // ───────────── Бэкап на диске (на случай вылета) ─────────────

    private static Path backupFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("secondy_client_optimizer_backup.json");
    }

    private void loadBackup() {
        try {
            Path f = backupFile();
            if (!Files.exists(f)) return;
            JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            for (String k : o.keySet()) backup.put(k, o.get(k).getAsString());
        } catch (Exception ignored) { }
    }

    private void saveBackup() {
        try {
            Path f = backupFile();
            if (backup.isEmpty()) {
                Files.deleteIfExists(f);
                return;
            }
            JsonObject o = new JsonObject();
            for (Map.Entry<String, String> e : backup.entrySet()) o.addProperty(e.getKey(), e.getValue());
            Files.writeString(f, o.toString());
        } catch (Exception ignored) { }
    }

    /** Вызывается один раз после запуска: если прошлая сессия не успела вернуть настройки (вылет) — возвращаем. */
    public void recover(MinecraftClient mc) {
        restoreAll(mc);
    }

    public void restoreAll(MinecraftClient mc) {
        if (mc == null || mc.options == null) return;
        for (Opt<?> o : all) o.restore(mc.options);
    }

    // ───────────── Логика ─────────────

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.options == null) return;
        // Раз в полсекунды достаточно: опции меняются редко
        if (tick++ % 10 != 0) return;
        GameOptions o = mc.options;

        // FPS: в фоне — низкий лимит, иначе (по желанию) без лимита
        Integer fps = null;
        if (bgOn.get() && !mc.isWindowFocused()) fps = bgFps.getInt();
        else if (uncap.get()) fps = 260;
        maxFps.drive(o, fps);

        vsync.drive(o, vsyncOff.get() ? Boolean.FALSE : null);
        shadows.drive(o, shadowsOff.get() ? Boolean.FALSE : null);

        ParticlesMode pm = switch (particleMode.index()) {
            case 1 -> ParticlesMode.DECREASED;
            case 2 -> ParticlesMode.MINIMAL;
            default -> null;
        };
        // Только в сторону уменьшения: если у игрока уже «Минимум», не повышаем
        if (pm != null) {
            ParticlesMode orig = particles.original(o);
            if (orig.ordinal() > pm.ordinal()) pm = orig;
        }
        particles.drive(o, pm);

        // Игра хранит этот параметр шагами по 0.25, поэтому только 0.75 и 0.5
        Double ent = switch (entMode.index()) {
            case 1 -> Double.valueOf(0.75);
            case 2 -> Double.valueOf(0.5);
            default -> null;
        };
        if (ent != null) ent = Math.min(entDist.original(o), ent);
        entDist.drive(o, ent);

        Integer view = null;
        if (viewOn.get()) view = Math.min(viewDist.original(o), viewVal.getInt());
        viewDist.drive(o, view);

        Integer bl = null;
        if (biomeOn.get()) bl = Math.min(biome.original(o), biomeVal.getInt());
        biome.drive(o, bl);
    }

    @Override
    public void onDisable() {
        tick = 0;
        restoreAll(MinecraftClient.getInstance());
    }

    /** 1 — безопасный, 2 — баланс, 3 — максимум FPS. */
    private void profile(int p) {
        bgOn.set(true);
        bgFps.set(20);
        uncap.set(p >= 3);
        vsyncOff.set(p >= 3);
        shadowsOff.set(p >= 2);
        particleMode.set(p == 1 ? 0 : p == 2 ? 1 : 2);
        entMode.set(p >= 3 ? 1 : 0);
        viewOn.set(p >= 3);
        viewVal.set(10);
        biomeOn.set(p >= 2);
        biomeVal.set(p == 2 ? 2 : 1);
        setEnabled(true);
        tick = 0;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.inGameHud != null) {
            String name = p == 1 ? "safe" : p == 2 ? "balanced" : "max FPS";
            mc.inGameHud.getChatHud().addMessage(Text.literal("[_2ndy] Optimizer: profile \"" + name + "\""));
        }
    }
}
