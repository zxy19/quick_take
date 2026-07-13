package studio.fantasyit.quick_take.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import studio.fantasyit.quick_take.integration.JechIntegration;

import java.util.ArrayList;
import java.util.List;

public class ItemMatcher {

    public static boolean matches(ItemStack stack, String pattern) {
        if (pattern.isBlank()) {
            return true;
        }
        String[] subPatterns = pattern.toLowerCase().split(" ");
        List<String> matchTexts = getMatchTexts(stack);
        for (String sub : subPatterns) {
            if (!matchesAny(matchTexts, sub) && !JechIntegration.tryMatch(stack, sub)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesAny(List<String> texts, String pattern) {
        for (String text : texts) {
            if (isSubsequence(pattern, text)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSubsequence(String pattern, String text) {
        int pi = 0;
        for (int ti = 0; ti < text.length() && pi < pattern.length(); ti++) {
            if (text.charAt(ti) == pattern.charAt(pi)) {
                pi++;
            }
        }
        return pi == pattern.length();
    }

    private static List<String> getMatchTexts(ItemStack stack) {
        List<String> texts = new ArrayList<>();
        texts.add(stack.getHoverName().getString().toLowerCase());
        for (Component line : getTooltipLines(stack)) {
            texts.add(line.getString().toLowerCase());
        }
        texts.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase());
        return texts;
    }

    public static List<Component> getTooltipLines(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        return stack.getTooltipLines(
                Item.TooltipContext.of(mc.level, mc.player),
                mc.player,
                net.neoforged.neoforge.client.ClientTooltipFlag.of(
                        mc.options.advancedItemTooltips
                                ? TooltipFlag.Default.ADVANCED
                                : TooltipFlag.Default.NORMAL)
        );
    }
}