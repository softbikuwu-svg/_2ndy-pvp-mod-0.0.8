package com.secondy.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class PingModule extends HudModule {
    public PingModule() {
        super("Ping", "Ping to the server", Category.HUD, 6, 120);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        int ping = 0;
        var p = mc.player;
        var net = mc.getNetworkHandler();
        if (p != null && net != null) {
            var entry = net.getPlayerListEntry(p.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        label(ctx, mc, "Ping: " + ping + " ms");
    }
}
