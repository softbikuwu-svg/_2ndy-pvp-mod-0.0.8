package com.secondy.client.module;

import com.secondy.client.settings.BoolSetting;
import com.secondy.client.settings.ColorSetting;
import com.secondy.client.settings.ModeSetting;
import com.secondy.client.settings.NumberSetting;
import com.secondy.client.util.Proj;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Floating damage numbers above entities that lose (or regain) health. */
public class DamageIndicatorModule extends Module {
    private final BoolSetting onlyMine = add(new BoolSetting("Only my hits", "only_mine", true));
    private final BoolSetting self = add(new BoolSetting("Show damage to me", "self", false));
    private final BoolSetting heal = add(new BoolSetting("Show healing", "heal", false));
    private final ModeSetting units = add(new ModeSetting("Units", "units", 0, "Health", "Hearts"));
    private final NumberSetting scale = add(new NumberSetting("Size", "size", 1.2, 0.6, 3.0, 0.1, "x"));
    private final NumberSetting time = add(new NumberSetting("Duration", "time", 1.0, 0.4, 3.0, 0.1, " s"));
    private final ColorSetting dmgColor = add(new ColorSetting("Damage color", "dmg_color", 0xFF453A));
    private final ColorSetting healColor = add(new ColorSetting("Healing color", "heal_color", 0x30D158));

    private static final class Popup {
        final double x, y, z;
        final float amount;
        final long born;
        Popup(double x, double y, double z, float amount, long born) {
            this.x = x; this.y = y; this.z = z; this.amount = amount; this.born = born;
        }
    }

    private Map<Integer, Float> hp = new HashMap<>();
    private final Map<Integer, Long> mine = new HashMap<>();
    private final List<Popup> popups = new ArrayList<>();

    public DamageIndicatorModule() {
        super("Damage Indicator", "Shows damage numbers over targets", Category.PVP);
    }

    /** Called when the player attacks an entity (see SecondyClient). */
    public void onAttack(Entity e) {
        mine.put(e.getId(), System.currentTimeMillis());
    }

    @Override
    public void onDisable() {
        popups.clear();
        hp.clear();
        mine.clear();
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            popups.clear();
            hp.clear();
            return;
        }
        long now = System.currentTimeMillis();
        Map<Integer, Float> next = new HashMap<>();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
            if (le == mc.player && !self.get()) continue;
            if (mc.player.squaredDistanceTo(le) > 48 * 48) continue;
            float cur = le.getHealth() + le.getAbsorptionAmount();
            next.put(le.getId(), cur);
            Float old = hp.get(le.getId());
            if (old == null) continue;
            float delta = old - cur;
            boolean lost = delta >= 0.5f;
            boolean gained = delta <= -0.5f;
            if (!lost && !(gained && heal.get())) continue;
            if (lost && onlyMine.get() && le != mc.player) {
                Long t = mine.get(le.getId());
                if (t == null || now - t > 1500) continue;
            }
            double jitter = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.6;
            popups.add(new Popup(le.getX() + jitter, le.getY() + le.getHeight() + 0.1, le.getZ() + jitter,
                    -delta, now));
        }
        hp = next;
        long life = (long) (time.get() * 1000);
        for (Iterator<Popup> it = popups.iterator(); it.hasNext(); ) {
            if (now - it.next().born > life) it.remove();
        }
        mine.values().removeIf(t -> now - t > 5000);
        if (popups.size() > 40) popups.subList(0, popups.size() - 40).clear();
    }

    public void draw(DrawContext ctx, MinecraftClient mc) {
        if (popups.isEmpty() || !Proj.begin(mc)) return;
        long now = System.currentTimeMillis();
        float life = (float) (time.get() * 1000);
        float s = scale.getFloat();
        for (Popup p : popups) {
            float age = (now - p.born) / life;
            if (age < 0f || age > 1f) continue;
            float[] sc = Proj.project(p.x, p.y + age * 0.8, p.z);
            if (sc == null) continue;
            float amt = units.index() == 1 ? p.amount / 2f : p.amount;
            String txt = (amt < 0 ? "+" : "-") + trim(Math.abs(amt));
            int alpha = age < 0.6f ? 255 : Math.round(255 * (1f - age) / 0.4f);
            int base = amt < 0 ? healColor.get() : dmgColor.get();
            int col = (Math.max(0, Math.min(255, alpha)) << 24) | (base & 0xFFFFFF);
            if ((col >>> 24) < 8) continue;
            var m = ctx.getMatrices();
            m.pushMatrix();
            m.translate(sc[0], sc[1]);
            m.scale(s, s);
            ctx.drawText(mc.textRenderer, txt, -mc.textRenderer.getWidth(txt) / 2, -4, col, true);
            m.popMatrix();
        }
    }

    private static String trim(float v) {
        String s = String.format(Locale.ROOT, "%.1f", v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}
