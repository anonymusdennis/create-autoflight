package dev.ryanhcode.sable.api.sublevel;

import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;

public interface SubLevelObserver {
   default void onSubLevelAdded(SubLevel subLevel) {
   }

   default void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
   }

   default void tick(SubLevelContainer subLevels) {
   }
}
