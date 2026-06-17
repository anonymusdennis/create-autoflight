package dev.ryanhcode.sable.api.block;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import org.jetbrains.annotations.Nullable;

public interface BlockEntitySubLevelActor {
   default void sable$tick(ServerSubLevel subLevel) {
   }

   default void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
   }

   @Nullable
   default Iterable<SubLevel> sable$getLoadingDependencies() {
      return this.sable$getConnectionDependencies();
   }

   @Nullable
   default Iterable<SubLevel> sable$getConnectionDependencies() {
      return null;
   }
}
