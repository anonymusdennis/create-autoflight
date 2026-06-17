package dev.simulated_team.simulated.compat.computercraft.wired;

import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.service.SimPlatformService;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface DockingConnectorWiredElement {
   boolean CC_LOADED = SimPlatformService.INSTANCE.isLoaded("computercraft");

   void connect(DockingConnectorWiredElement var1);

   void disconnect(DockingConnectorWiredElement var1);

   void remove();

   static DockingConnectorWiredElement create(DockingConnectorBlockEntity blockEntity) {
      return (DockingConnectorWiredElement)(CC_LOADED ? new DockingConnectorWiredElementImpl(blockEntity) : NoopDockingConnectorWiredElement.INSTANCE);
   }
}
