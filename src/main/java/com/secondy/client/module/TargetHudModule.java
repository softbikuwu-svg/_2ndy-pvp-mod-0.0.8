package com.secondy.client.module;

import com.secondy.client.settings.BoolSetting;
import com.secondy.client.settings.ColorSetting;
import com.secondy.client.settings.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Locale;

/** Target HUD: ник, здоровье и дистанция до существа, на которое ты целишься. */
public class TargetHudModule extends HudModule {
    private final BoolSetting showName = add(new BoolSetting("Show name", "show_name", true));
    private final BoolSetting showHp = add(new BoolSetting("Health as numbers", "show_hp", true));
    private final BoolSetting showDist = add(new BoolSetting("Distance", "show_dist", true));
    private final BoolSetting playersOnly = add(new BoolSetting("Players only", "players_only", false));
    private final BoolSetting hpGradient = add(new BoolSetting("Bar color by health", "hp_gradient", true));
    private final NumberSetting widthSet = add(new NumberSetting("Width", "width", 110, 70, 220, 5, " px"));
    private final NumberSetting barHeight = add(new NumberSetting("Bar height", "bar_height", 5, 3, 12, 1, " px"));
    private final NumberSetting hold = add(new NumberSetting("Keep after aiming away", "hold", 3, 0, 10, 0.5, " s"));
    private final ColorSetting barColor = add(new ColorSetting("Bar color (without gradient)", "bar_color", 0x30D158));
    private final ColorSetting textColor = add(new ColorSetting("Text color", "text_color", 0xFFFFFF));

    private LivingEntity target;
    private long lastSeen;

    public TargetHudModule() {
        super("Target HUD", "Name, health and distance of the target", Category.PVP, 200, 110);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        Entity e = mc.targetedEntity;
        if (e instanceof LivingEntity le && le.isAlive() && (!playersOnly.get() || le instanceof PlayerEntity)) {
            target = le;
            lastSeen = now;
        } else if (target != null && (!target.isAlive() || now - lastSeen > hold.get() * 1000.0)) {
            target = null;
        }
    }

    @Override
    public void onDisable() {
        target = null;
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        boolean preview = mc.currentScreen instanceof ChatScreen;
        String name;
        float hp, max, abs;
        double dist;
        if (target != null) {
            name = target.getName().getString();
            hp = target.getHealth();
            max = Math.max(1f, target.getMaxHealth());
            abs = target.getAbsorptionAmount();
            dist = mc.player == null ? 0 : mc.player.distanceTo(target);
        } else if (preview) {
            name = "Steve";
            hp = 14f;
            max = 20f;
            abs = 0f;
            dist = 3.4;
        } else {
            return;
        }

        int w = widthSet.getInt();
        int bh = barHeight.getInt();
        boolean info = showHp.get() || showDist.get();
        int h = 6 + (showName.get() ? 11 : 0) + bh + (info ? 12 : 0);
        width = w;
        height = h;
        background(ctx);

        int tc = textColor.get();
        int ty = y + 3;
        if (showName.get()) {
            ctx.drawText(mc.textRenderer, com.secondy.client.util.Ui.trim(mc.textRenderer, name, w - 8), x + 4, ty, tc, true);
            ty += 11;
        }

        float ratio = Math.max(0f, Math.min(1f, hp / max));
        int bx = x + 4, bw = w - 8;
        ctx.fill(bx, ty, bx + bw, ty + bh, 0x80101010);
        int fill = Math.round(bw * ratio);
        int col = hpGradient.get()
                ? (java.awt.Color.HSBtoRGB(ratio * 0.33f, 0.85f, 1f) | 0xFF000000)
                : barColor.get();
        if (fill > 0) ctx.fill(bx, ty, bx + fill, ty + bh, col);
        ty += bh + 3;

        if (info) {
            StringBuilder sb = new StringBuilder();
            if (showHp.get()) {
                sb.append(String.format(Locale.ROOT, "%.1f/%.0f", hp, max));
                if (abs > 0) sb.append(String.format(Locale.ROOT, " (+%.1f)", abs));
            }
            if (showDist.get()) {
                if (sb.length() > 0) sb.append("  ");
                sb.append(String.format(Locale.ROOT, "%.1f m", dist));
            }
            ctx.drawText(mc.textRenderer, sb.toString(), x + 4, ty, tc, true);
        }
    }
}
