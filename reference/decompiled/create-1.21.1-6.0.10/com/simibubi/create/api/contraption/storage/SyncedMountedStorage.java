package com.simibubi.create.api.contraption.storage;

import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;

public interface SyncedMountedStorage {
   boolean isDirty();

   void markClean();

   void afterSync(Contraption var1, BlockPos var2);
}
