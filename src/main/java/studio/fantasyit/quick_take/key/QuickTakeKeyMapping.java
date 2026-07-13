package studio.fantasyit.quick_take.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class QuickTakeKeyMapping {
    public static final KeyMapping OPEN = new KeyMapping(
            "key.quick_take.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KeyMapping.Category.CREATIVE
    );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }
}