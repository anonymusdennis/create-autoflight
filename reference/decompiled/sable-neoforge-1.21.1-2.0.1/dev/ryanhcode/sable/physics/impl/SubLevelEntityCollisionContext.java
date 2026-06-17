package dev.ryanhcode.sable.physics.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class SubLevelEntityCollisionContext extends EntityCollisionContext {
   public SubLevelEntityCollisionContext(Entity entity) {
      super(entity);
   }

   public boolean isAbove(VoxelShape voxelShape, BlockPos blockPos, boolean bl) {
      return false;
   }
}
