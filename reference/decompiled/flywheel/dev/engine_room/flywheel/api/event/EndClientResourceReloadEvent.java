package dev.engine_room.flywheel.api.event;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class EndClientResourceReloadEvent extends Event implements IModBusEvent {
   private final Minecraft minecraft;
   private final ResourceManager resourceManager;
   private final boolean initialReload;
   private final Optional<Throwable> error;

   public EndClientResourceReloadEvent(Minecraft minecraft, ResourceManager resourceManager, boolean initialReload, Optional<Throwable> error) {
      this.minecraft = minecraft;
      this.resourceManager = resourceManager;
      this.initialReload = initialReload;
      this.error = error;
   }

   public Minecraft minecraft() {
      return this.minecraft;
   }

   public ResourceManager resourceManager() {
      return this.resourceManager;
   }

   public boolean isInitialReload() {
      return this.initialReload;
   }

   public Optional<Throwable> error() {
      return this.error;
   }
}
