package dev.simulated_team.simulated.neoforge;

import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.neoforge.events.SimNeoForgeClientEvents;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(
   value = "simulated",
   dist = {Dist.CLIENT}
)
public class SimulatedNeoForgeClient {
   public SimulatedNeoForgeClient(IEventBus modEventBus, ModContainer container) {
      container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory)(c, l) -> new BaseConfigScreen(l, "simulated"));
      NeoForge.EVENT_BUS.register(SimNeoForgeClientEvents.class);
      modEventBus.register(SimNeoForgeClientEvents.ModBusEvents.class);
      SimulatedClient.PLUNGER_LAUNCHER_RENDER_HANDLER.registerListeners(NeoForge.EVENT_BUS);
      SimulatedClient.init();
   }
}
