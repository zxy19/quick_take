package studio.fantasyit.quick_take.integration;

import me.towdium.jecharacters.utils.Match;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.quick_take.helper.ItemMatcher;

import java.util.List;

public class JechIntegration {

    public static float tryMatchName(ItemStack stack, String pattern) {
        if (!Integration.isJechLoaded()) {
            return 0;
        }
        String name = stack.getHoverName().getString();
        if (Match.contains(name, pattern)) {
            return 1.0f;
        }
        return 0;
    }

    public static float tryMatchTooltip(ItemStack stack, String pattern) {
        if (!Integration.isJechLoaded()) {
            return 0;
        }
        List<Component> tooltipLines = ItemMatcher.getTooltipLines(stack);
        for (Component line : tooltipLines) {
            if (Match.contains(line.getString(), pattern)) {
                return 0.5f;
            }
        }
        return 0;
    }

    public static boolean tryMatch(ItemStack stack, String pattern) {
        if (!Integration.isJechLoaded()) {
            return false;
        }
        String name = stack.getHoverName().getString();
        if (Match.contains(name, pattern)) {
            return true;
        }
        List<Component> tooltipLines = ItemMatcher.getTooltipLines(stack);
        for (Component line : tooltipLines) {
            if (Match.contains(line.getString(), pattern)) {
                return true;
            }
        }
        return false;
    }
}