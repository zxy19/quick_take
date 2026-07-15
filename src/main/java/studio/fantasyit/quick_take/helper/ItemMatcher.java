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

    public record ItemMatchEntry(ItemStack stack, List<String> matchTexts) {}

    public static float matchPriority(ItemMatchEntry entry, String pattern) {
        if (pattern.isBlank()) {
            return 1.0f;
        }
        String[] subPatterns = pattern.toLowerCase().split(" ");
        float minScore = 1.0f;
        for (String sub : subPatterns) {
            float best = 0;
            List<String> texts = entry.matchTexts();
            for (int ti = 0; ti < texts.size(); ti++) {
                float s = subsequenceScore(sub, texts.get(ti));
                if (ti > 0 && ti < texts.size() - 1) {
                    s *= 0.01f;
                }
                if (s > best) best = s;
            }
            if (best == 0 && JechIntegration.tryMatch(entry.stack(), sub)) {
                best = 1.0f;
            }
            if (best == 0) return 0;
            if (best < minScore) minScore = best;
        }
        return minScore;
    }

    public static float matchPriorityName(ItemMatchEntry entry, String pattern) {
        if (pattern.isBlank()) {
            return 1.0f;
        }
        String[] subPatterns = pattern.toLowerCase().split(" ");
        float totalScore = 0;
        List<String> texts = entry.matchTexts();
        String name = texts.get(0);
        int nameLen = name.length();
        for (String sub : subPatterns) {
            float best = subsequenceScore(sub, name);
            if (best == 0) {
                best = JechIntegration.tryMatchName(entry.stack(), sub);
            }
            if (best == 0) return 0;
            totalScore += best * ((float) sub.length() / nameLen);
        }
        return totalScore;
    }

    public static float matchPriorityId(ItemMatchEntry entry, String pattern) {
        if (pattern.isBlank()) {
            return 1.0f;
        }
        String[] subPatterns = pattern.toLowerCase().split(" ");
        float totalScore = 0;
        List<String> texts = entry.matchTexts();
        String id = texts.get(texts.size() - 1);
        int idLen = id.length();
        for (String sub : subPatterns) {
            float best = subsequenceScore(sub, id);
            if (best == 0) {
                best = JechIntegration.tryMatchName(entry.stack(), sub);
            }
            if (best == 0) return 0;
            totalScore += best * ((float) sub.length() / idLen);
        }
        return totalScore;
    }

    public static float matchPriorityTooltip(ItemMatchEntry entry, String pattern) {
        if (pattern.isBlank()) {
            return 1.0f;
        }
        String[] subPatterns = pattern.toLowerCase().split(" ");
        float totalScore = 0;
        List<String> texts = entry.matchTexts();
        for (String sub : subPatterns) {
            float best = 0;
            int bestTi = 0;
            for (int ti = 1; ti < texts.size() - 1; ti++) {
                float s = subsequenceScore(sub, texts.get(ti));
                if (s > best) {
                    best = s;
                    bestTi = ti;
                }
            }
            if (best == 0) {
                best = JechIntegration.tryMatchTooltip(entry.stack(), sub);
            }
            if (best == 0) return 0;
            totalScore += best * ((float) sub.length() / texts.get(bestTi).length());
        }
        return totalScore;
    }

    public static boolean matches(ItemMatchEntry entry, String pattern) {
        return matchPriority(entry, pattern) > 0;
    }

    private static float subsequenceScore(String pattern, String text) {
        int pi = 0;
        int maxConsecutive = 0;
        int currentConsecutive = 0;
        for (int ti = 0; ti < text.length() && pi < pattern.length(); ti++) {
            if (text.charAt(ti) == pattern.charAt(pi)) {
                pi++;
                currentConsecutive++;
                if (currentConsecutive > maxConsecutive) {
                    maxConsecutive = currentConsecutive;
                }
            } else if (currentConsecutive > 0) {
                currentConsecutive = 0;
            }
        }
        if (pi < pattern.length()) {
            return 0;
        }
        return (float) maxConsecutive / pattern.length();
    }

    public static List<String> getMatchTexts(ItemStack stack) {
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