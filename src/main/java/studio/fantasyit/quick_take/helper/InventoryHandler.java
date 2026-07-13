package studio.fantasyit.quick_take.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class InventoryHandler {

    public static void selectItem(Minecraft mc, ItemStack target) {
        if (mc.player == null || !mc.player.hasInfiniteMaterials()) {
            return;
        }
        Inventory inv = mc.player.getInventory();

        for (int i = 0; i < 9; i++) {
            if (ItemStack.isSameItemSameComponents(inv.getItem(i), target)) {
                inv.setSelectedSlot(i);
                return;
            }
        }

        int backpackSlot = findItemInBackpack(inv, target);
        if (backpackSlot < 0) {
            int emptyBackpack = findEmptyBackpackSlot(inv);
            if (emptyBackpack >= 0) {
                setItemAndSync(mc, emptyBackpack, target.copy());
                backpackSlot = emptyBackpack;
            } else {
                mc.player.drop(inv.getItem(inv.getSelectedSlot()), true);
                setItemAndSync(mc, inv.getSelectedSlot(), target.copy());
                return;
            }
        }

        int emptyHotbar = findEmptyHotbarSlot(inv);
        if (emptyHotbar >= 0) {
            setItemAndSync(mc, emptyHotbar, target.copy());
            inv.setSelectedSlot(emptyHotbar);
            if (backpackSlot >= 9) {
                inv.setItem(backpackSlot, ItemStack.EMPTY);
                mc.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, backpackSlot);
            }
        } else {
            ItemStack oldItem = inv.getItem(inv.getSelectedSlot()).copy();
            setItemAndSync(mc, inv.getSelectedSlot(), target.copy());
            inv.setItem(backpackSlot, oldItem);
            mc.gameMode.handleCreativeModeItemAdd(oldItem, backpackSlot);
        }
    }

    private static void setItemAndSync(Minecraft mc, int inventoryIndex, ItemStack stack) {
        mc.player.getInventory().setItem(inventoryIndex, stack);
        int containerSlot = inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
        mc.gameMode.handleCreativeModeItemAdd(stack, containerSlot);
    }

    private static int findItemInBackpack(Inventory inv, ItemStack target) {
        for (int i = 0; i < 36; i++) {
            if (ItemStack.isSameItemSameComponents(inv.getItem(i), target)) {
                return i;
            }
        }
        return -1;
    }

    private static int findEmptyBackpackSlot(Inventory inv) {
        for (int i = 9; i < 36; i++) {
            if (inv.getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static int findEmptyHotbarSlot(Inventory inv) {
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}