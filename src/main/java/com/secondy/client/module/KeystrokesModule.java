package com.secondy.client.module;

import com.secondy.client.util.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class KeystrokesModule extends HudModule {
    public KeystrokesModule() {
        super("Keystrokes", "Shows pressed WASD keys, mouse buttons and space", Category.HUD, 6, 100);
        width = 70;
        height = 84;
        setEnabled(true);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc) {
        var o = mc.options;
        key(ctx, mc, "W", x + 24, y, 22, 22, o.forwardKey.isPressed());
        key(ctx, mc, "A", x, y + 24, 22, 22, o.leftKey.isPressed());
        key(ctx, mc, "S", x + 24, y + 24, 22, 22, o.backKey.isPressed());
        key(ctx, mc, "D", x + 48, y + 24, 22, 22, o.rightKey.isPressed());
        key(ctx, mc, "LMB", x, y + 48, 34, 22, Input.mouse(0));
        key(ctx, mc, "RMB", x + 36, y + 48, 34, 22, Input.mouse(1));
        key(ctx, mc, "—", x, y + 72, 70, 12, o.jumpKey.isPressed());
    }

    private void key(DrawContext ctx, MinecraftClient mc, String text, int kx, int ky, int w, int h, boolean down) {
        if (down || showBackground) ctx.fill(kx, ky, kx + w, ky + h, down ? 0xB0FFFFFF : 0x90000000);
        int color = down ? 0xFF000000 : 0xFFFFFFFF;
        int tx = kx + (w - mc.textRenderer.getWidth(text)) / 2;
        int ty = ky + (h - 8) / 2;
        ctx.drawText(mc.textRenderer, text, tx, ty, color, false);
    }
}
