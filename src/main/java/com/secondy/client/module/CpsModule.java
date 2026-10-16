package com.secondy.client.module;

import com.secondy.client.util.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayDeque;
import java.util.Deque;

public class CpsModule extends HudModule {
    private final Deque<Long> left = new ArrayDeque<>();
    private final Deque<Long> right = new ArrayDeque<>();
    private boolean lastL, lastR;

    public CpsModule() {
        super("CPS", "Clicks per second (LMB | RMB)", Category.HUD, 6, 24);
        setEnabled(true);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        long now = System.currentTimeMillis();
        boolean l = Input.mouse(0), r = Input.mouse(1);
        if (l && !lastL) left.add(now);
        if (r && !lastR) right.add(now);
        lastL = l;
        lastR = r;
        while (!left.isEmpty() && now - left.peekFirst() > 1000) left.pollFirst();
        while (!right.isEmpty() && now - right.peekFirst() > 1000) right.pollFirst();
        label(ctx, mc, "CPS: " + left.size() + " | " + right.size());
    }
}
