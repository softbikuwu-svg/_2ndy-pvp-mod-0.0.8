package com.secondy.client;

import com.secondy.client.module.*;
import com.secondy.client.ui.HudDragger;
import com.secondy.client.ui.SecondyMenuScreen;
import com.secondy.client.util.Config;
import com.secondy.client.util.Input;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecondyClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("secondy_client");
    private boolean shiftWasDown = true;
    private boolean optimizerRecovered;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();

        // Тик: открытие меню на Right Shift + тик модулей
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            boolean s = Input.key(GLFW.GLFW_KEY_RIGHT_SHIFT);
            if (s && !shiftWasDown && mc.currentScreen == null) {
                mc.setScreen(new SecondyMenuScreen());
            }
            shiftWasDown = s;
            if (!optimizerRecovered && mc.options != null) {
                optimizerRecovered = true;
                ModuleManager.get(OptimizerModule.class).recover(mc);
            }
            ModuleManager.tick(mc);
        });

        // Отрисовка HUD
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("secondy_client", "hud"),
                (ctx, tickCounter) -> ModuleManager.renderHud(ctx, MinecraftClient.getInstance()));

        // Свой прицел: скрываем ванильный, когда включён Custom Crosshair
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (ctx, tickCounter) -> {
            if (ModuleManager.get(CrosshairModule.class).hidesVanilla()) return;
            original.render(ctx, tickCounter);
        });

        // Перетаскивание HUD в открытом чате
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof ChatScreen) {
                ScreenEvents.afterRender(screen).register(
                        (s, ctx, mx, my, delta) -> HudDragger.update(ctx, client, mx, my));
            }
        });

        // Удары: Combo и Reach
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity) {
                ModuleManager.get(ComboModule.class).hit();
                ModuleManager.get(ReachModule.class).hit(player, entity);
                ModuleManager.get(KillCounterModule.class).onAttack(entity);
                ModuleManager.get(HitSoundModule.class).onAttack(entity);
                ModuleManager.get(DamageIndicatorModule.class).onAttack(entity);
            }
            return ActionResult.PASS;
        });

        // Item Tooltip+: extra tooltip lines
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            ItemTooltipModule m = ModuleManager.get(ItemTooltipModule.class);
            if (m.isEnabled()) m.append(stack, lines);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> {
            // Возвращаем игровые настройки, которые менял оптимизатор, чтобы они не остались в options.txt
            ModuleManager.get(OptimizerModule.class).restoreAll(c);
            Config.save();
        });
        LOGGER.info("_2ndy Client loaded");
    }
}
