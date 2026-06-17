package dev.ryanhcode.sable.mixinterface.toast;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;

public interface SableToastableServer {
   void sable$reportSubLevelLoadFailure(GlobalSavedSubLevelPointer var1);

   void sable$reportSubLevelSaveFailure(SubLevelData var1);

   void sable$reportSubLevelPhysicsFailure(ServerSubLevel var1);
}
