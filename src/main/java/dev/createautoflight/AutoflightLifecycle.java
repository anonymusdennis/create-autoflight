package dev.createautoflight;

import dev.createautoflight.client.ClientNavigationDebugCache;
import dev.createautoflight.client.ClientThrustDebugCache;
import dev.createautoflight.content.navigation.FlightCommandBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = CreateAutoflight.MOD_ID)
public final class AutoflightLifecycle {
    private AutoflightLifecycle() {}

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        FlightCommandBus.clearAll();
    }

    @EventBusSubscriber(modid = CreateAutoflight.MOD_ID, value = Dist.CLIENT)
    public static final class Client {
        private Client() {}

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientNavigationDebugCache.clearAll();
            ClientThrustDebugCache.clearAll();
        }

        @SubscribeEvent
        public static void onLevelUnload(LevelEvent.Unload event) {
            if (event.getLevel().isClientSide()) {
                ClientNavigationDebugCache.clearAll();
                ClientThrustDebugCache.clearAll();
            }
        }
    }
}
