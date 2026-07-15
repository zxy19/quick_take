package studio.fantasyit.quick_take.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;
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
        openQuickTake(event.getScreen(), String.valueOf(event.getCodePoint()));
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Post event) {
        String keyName = GLFW.glfwGetKeyName(event.getKeyCode(), event.getScanCode());
        if (keyName != null && keyName.length() == 1 && Character.isLetterOrDigit(keyName.charAt(0))) {
            openQuickTake(event.getScreen(), keyName);
        }
    }

    private static void openQuickTake(net.minecraft.client.gui.screens.Screen screen, String initialText) {
        Minecraft mc = Minecraft.getInstance();
        if (screen instanceof CreativeModeInventoryScreen && !(mc.screen instanceof QuickTakeScreen)) {
            if (mc.player != null && mc.player.hasInfiniteMaterials()) {
                mc.setScreen(new QuickTakeScreen(initialText));
            }
        }
    }
}