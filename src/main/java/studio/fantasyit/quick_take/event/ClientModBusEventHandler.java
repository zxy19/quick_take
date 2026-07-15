package studio.fantasyit.quick_take.event;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import studio.fantasyit.quick_take.QuickTake;
import studio.fantasyit.quick_take.helper.ItemCache;

@EventBusSubscriber(modid = QuickTake.MODID, value = Dist.CLIENT)
public class ClientModBusEventHandler {

    @SubscribeEvent
    public static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(
                Identifier.fromNamespaceAndPath(QuickTake.MODID, "item_cache"),
                new SimplePreparableReloadListener<Void>() {
                    @Override
                    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                        return null;
                    }

                    @Override
                    protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                        ItemCache.invalidate();
                    }
                }
        );
    }
}