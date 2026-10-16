package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<Module> ALL = new ArrayList<>();

    private ModuleManager() {}

    public static void init() {
        ALL.add(new FpsModule());
        ALL.add(new CpsModule());
        ALL.add(new KeystrokesModule());
        ALL.add(new CoordsModule());
        ALL.add(new ArmorModule());
        ALL.add(new PingModule());
        ALL.add(new ClockModule());
        ALL.add(new MemoryModule());
        ALL.add(new SpeedModule());
        ALL.add(new DirectionModule());
        ALL.add(new PotionModule());
        ALL.add(new ServerModule());
        ALL.add(new SaturationModule());
        ALL.add(new ItemCounterModule());
        ALL.add(new LightModule());
        ALL.add(new DayCounterModule());
        ALL.add(new PlaytimeModule());
        ALL.add(new HealthModule());
        ALL.add(new XpModule());
        ALL.add(new HeldItemModule());
        ALL.add(new DimensionModule());
        ALL.add(new FallDistanceModule());
        ALL.add(new PotCounterModule());
        ALL.add(new PearlCounterModule());
        ALL.add(new ArrowCounterModule());
        ALL.add(new ComboModule());
        ALL.add(new ReachModule());
        ALL.add(new SprintModule());
        ALL.add(new ZoomModule());
        ALL.add(new FullbrightModule());
        ALL.add(new FreeLookModule());
        ALL.add(new ChromaModule());
        ALL.add(new CrosshairModule());
        ALL.add(new BlockOverlayModule());
        ALL.add(new LowFireModule());
        ALL.add(new LowShieldModule());
        ALL.add(new ItemCustomizerModule());
        ALL.add(new ItemTooltipModule());
        ALL.add(new DamageIndicatorModule());
        ALL.add(new TargetHudModule());
        ALL.add(new AttackCooldownModule());
        ALL.add(new KillCounterModule());
        ALL.add(new DeathPointModule());
        ALL.add(new HitSoundModule());
        ALL.add(new ArmorBreakModule());
        ALL.add(new TrajectoryModule());
        ALL.add(new OptimizerModule());
        ALL.add(new PresetsModule());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> type) {
        for (Module m : ALL) if (type.isInstance(m)) return (T) m;
        throw new IllegalStateException("Module not found: " + type);
    }

    public static List<HudModule> huds() {
        List<HudModule> list = new ArrayList<>();
        for (Module m : ALL) if (m instanceof HudModule h) list.add(h);
        return list;
    }

    public static void tick(MinecraftClient mc) {
        com.secondy.client.util.Proj.tick(mc);
        for (Module m : ALL) if (m.isEnabled()) m.onTick(mc);
    }

    public static void renderHud(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null || mc.options.hudHidden) return;
        for (HudModule h : huds()) if (h.isEnabled()) h.drawScaled(ctx, mc);
        CrosshairModule cross = get(CrosshairModule.class);
        if (cross.isEnabled()) cross.draw(ctx, mc);
        TrajectoryModule traj = get(TrajectoryModule.class);
        if (traj.isEnabled()) traj.draw(ctx, mc);
        BlockOverlayModule bo = get(BlockOverlayModule.class);
        if (bo.isEnabled()) bo.draw(ctx, mc);
        DamageIndicatorModule di = get(DamageIndicatorModule.class);
        if (di.isEnabled()) di.draw(ctx, mc);
    }
}
