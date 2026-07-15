package studio.fantasyit.quick_take.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ItemCache {
    private static List<ItemMatcher.ItemMatchEntry> cache = null;

    public static List<ItemMatcher.ItemMatchEntry> getOrBuild(Minecraft mc) {
        if (cache == null) {
            cache = build(mc);
        }
        return cache;
    }

    public static void invalidate() {
        cache = null;
    }

    private static List<ItemMatcher.ItemMatchEntry> build(Minecraft mc) {
        CreativeModeTabs.tryRebuildTabContents(
                mc.level.enabledFeatures(),
                mc.player.canUseGameMasterBlocks(),
                mc.level.registryAccess()
        );
        List<CreativeModeTab> allTabs = CreativeModeTabRegistry.getSortedCreativeModeTabs().stream()
                .filter(t -> t.getType() == CreativeModeTab.Type.CATEGORY)
                .filter(CreativeModeTab::shouldDisplay)
                .toList();
        List<ItemMatcher.ItemMatchEntry> entries = new ArrayList<>();
        for (CreativeModeTab tab : allTabs) {
            Collection<ItemStack> items = tab.getDisplayItems();
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    entries.add(new ItemMatcher.ItemMatchEntry(stack, ItemMatcher.getMatchTexts(stack)));
                }
            }
        }
        return entries;
    }
}