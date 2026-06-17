package dev.ryanhcode.sable.api.physics.object;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.world.level.ChunkPos;

public interface ArbitraryPhysicsObject {
   void getBoundingBox(BoundingBox3d var1);

   void onUnloaded(SubLevelHoldingChunkMap var1, ChunkPos var2);

   void onRemoved();

   void onAddition(SubLevelPhysicsSystem var1);

   void wakeUp();
}
