package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class SprintModule extends Module {
    public SprintModule() {
        super("Toggle Sprint", "Automatically sprints while moving forward", Category.UTILITY);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        if (mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision
                && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
    }
}
