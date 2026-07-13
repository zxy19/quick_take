package studio.fantasyit.quick_take;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import studio.fantasyit.quick_take.key.QuickTakeKeyMapping;

@Mod(value = QuickTake.MODID, dist = Dist.CLIENT)
public class QuickTake {
    public static final String MODID = "quick_take";

    public QuickTake(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(QuickTakeKeyMapping::register);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }
}
