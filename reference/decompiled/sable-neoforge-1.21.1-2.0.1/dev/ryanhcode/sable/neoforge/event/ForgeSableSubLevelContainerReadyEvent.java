package dev.ryanhcode.sable.neoforge.event;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

public class ForgeSableSubLevelContainerReadyEvent extends Event {
   private final Level level;
   private final SubLevelContainer container;

   public ForgeSableSubLevelContainerReadyEvent(Level level, SubLevelContainer container) {
      this.level = level;
      this.container = container;
   }

   public Level getLevel() {
      return this.level;
   }

   public SubLevelContainer getContainer() {
      return this.container;
   }
}
