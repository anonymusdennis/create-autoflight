package dev.engine_room.flywheel.api.event;

import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.Event;

public final class ReloadLevelRendererEvent extends Event {
   private final ClientLevel level;

   public ReloadLevelRendererEvent(ClientLevel level) {
      this.level = level;
   }

   public ClientLevel level() {
      return this.level;
   }
}
