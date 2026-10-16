package com.secondy.client.module;

import com.secondy.client.settings.BoolSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Extra lines in item tooltips: durability, stack size, item id. Hooked in SecondyClient (ItemTooltipCallback). */
public class ItemTooltipModule extends Module {
    private final BoolSetting durability = add(new BoolSetting("Durability", "durability", true));
    private final BoolSetting percent = add(new BoolSetting("Durability percent", "percent", true));
    private final BoolSetting stackInfo = add(new BoolSetting("Stack size", "stack", true));
    private final BoolSetting itemId = add(new BoolSetting("Item ID", "id", false));

    public ItemTooltipModule() {
        super("Item Tooltip+", "Durability, stack size and ID in item tooltips", Category.UTILITY);
    }

    public void append(ItemStack stack, List<Text> lines) {
        if (stack.isEmpty()) return;
        if (durability.get() && stack.isDamageable()) {
            int max = stack.getMaxDamage();
            int left = max - stack.getDamage();
            float pct = left * 100f / Math.max(1, max);
            int col = pct > 50f ? 0x55FF55 : (pct > 20f ? 0xFFAA00 : 0xFF5555);
            String t = "Durability: " + left + " / " + max + (percent.get() ? " (" + Math.round(pct) + "%)" : "");
            lines.add(Text.literal(t).styled(s -> s.withColor(col)));
        }
        if (stackInfo.get() && stack.getMaxCount() > 1) {
            lines.add(Text.literal("Stack: " + stack.getCount() + " / " + stack.getMaxCount())
                    .formatted(Formatting.GRAY));
        }
        if (itemId.get()) {
            lines.add(Text.literal(Registries.ITEM.getId(stack.getItem()).toString())
                    .formatted(Formatting.DARK_GRAY));
        }
    }
}
