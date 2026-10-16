package com.secondy.client.module;

import com.secondy.client.SecondyClient;
import com.secondy.client.mixin.OverlayTextureAccessor;
import com.secondy.client.settings.BoolSetting;
import com.secondy.client.settings.ColorSetting;
import com.secondy.client.settings.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

/**
 * Hit Color: цвет «вспышки» при ударе (ванильный красный оверлей) настраивается как угодно
 * и, если включено «Через броню», накладывается и на броню (в ваниле броня не краснеет).
 *
 * Цвет меняется правкой верхних 8 строк оверлей-текстуры (там живёт «красный» оверлей урона);
 * броня подхватывает его через миксин EquipmentRendererMixin.
 */
public class HitColorModule extends Module {
    /** Ванильный цвет оверлея урона (ARGB). */
    private static final int VANILLA_ARGB = 0xB2FF0000;

    private final ColorSetting color = add(new ColorSetting("Hit color", "color", 0xFF2D2D));
    private final NumberSetting strength = add(new NumberSetting("Color strength", "strength", 55, 10, 100, 5, "%"));
    private final BoolSetting throughArmor = add(new BoolSetting("Tint armor too", "through_armor", true));

    private int applied = Integer.MIN_VALUE;
    private boolean failed;

    public HitColorModule() {
        super("Hit Color", "Custom hit color, visible even through armor", Category.VISUAL);
    }

    public boolean throughArmor() {
        return throughArmor.get();
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (failed || mc.gameRenderer == null) return;
        // alpha в оверлее — это доля ОРИГИНАЛЬНОГО цвета: 255 = без оверлея, меньше = сильнее оверлей
        int a = Math.round(255f * (1f - strength.getInt() / 100f));
        int argb = (a << 24) | color.current(0f);
        if (argb != applied) apply(mc, argb);
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!failed && mc != null && mc.gameRenderer != null) apply(mc, VANILLA_ARGB);
    }

    private void apply(MinecraftClient mc, int argb) {
        try {
            OverlayTexture overlay = mc.gameRenderer.getOverlayTexture();
            NativeImageBackedTexture tex = ((OverlayTextureAccessor) (Object) overlay).secondy$getTexture();
            NativeImage img = tex.getImage();
            if (img == null) return;
            for (int yy = 0; yy < 8; yy++) {
                for (int xx = 0; xx < 16; xx++) img.setColorArgb(xx, yy, argb);
            }
            tex.upload();
            applied = argb;
        } catch (Throwable t) {
            failed = true;
            SecondyClient.LOGGER.error("Hit Color: failed to modify the overlay texture, module disabled", t);
        }
    }
}
