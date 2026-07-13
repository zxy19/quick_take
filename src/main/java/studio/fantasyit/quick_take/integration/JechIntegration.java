package studio.fantasyit.quick_take.integration;

import me.towdium.jecharacters.utils.Match;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.quick_take.helper.ItemMatcher;

import java.util.List;

public class JechIntegration {

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