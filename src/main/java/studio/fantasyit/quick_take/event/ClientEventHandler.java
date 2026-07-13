package studio.fantasyit.quick_take.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import studio.fantasyit.quick_take.QuickTake;
import studio.fantasyit.quick_take.key.QuickTakeKeyMapping;
import studio.fantasyit.quick_take.screen.QuickTakeScreen;

@EventBusSubscriber(modid = QuickTake.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasInfiniteMaterials()) {
            if (QuickTakeKeyMapping.OPEN.consumeClick()) {
                mc.setScreen(new QuickTakeScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onCharacterTyped(ScreenEvent.CharacterTyped.Post event) {
        if (event.getScreen() instanceof CreativeModeInventoryScreen) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.hasInfiniteMaterials()) {
                mc.setScreen(new QuickTakeScreen(String.valueOf(event.getCodePoint())));
            }
        }
    }
}