package dev.ryanhcode.sable.physics.impl;

import dev.ryanhcode.sable.api.physics.collider.SableCollisionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public enum SableCollisionContextImpl implements SableCollisionContext {
   INSTANCE;

   public boolean isDescending() {
      return false;
   }

   public boolean isAbove(VoxelShape shape, BlockPos pos, boolean canAscend) {
      return canAscend;
   }

   public boolean isHoldingItem(Item item) {
      return false;
   }

   public boolean canStandOnFluid(FluidState fluid1, FluidState fluid2) {
      return false;
   }
}
