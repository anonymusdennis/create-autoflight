package dev.ryanhcode.sable.api.event;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface SableSubLevelContainerReadyEvent {
   void onSubLevelContainerReady(Level var1, SubLevelContainer var2);
}
