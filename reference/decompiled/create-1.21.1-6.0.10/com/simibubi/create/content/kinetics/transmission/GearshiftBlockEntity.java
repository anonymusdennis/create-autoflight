package com.simibubi.create.content.kinetics.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class GearshiftBlockEntity extends SplitShaftBlockEntity {
   public GearshiftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public float getRotationSpeedModifier(Direction face) {
      return this.hasSource() && face != this.getSourceFacing() && this.getBlockState().getValue(BlockStateProperties.POWERED) ? -1.0F : 1.0F;
   }
}
