package com.simibubi.create.content.decoration.palettes;

import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class LayeredBlock extends RotatedPillarBlock {
   public LayeredBlock(Properties p_55926_) {
      super(p_55926_);
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState stateForPlacement = super.getStateForPlacement(pContext);
      BlockState placedOn = pContext.getLevel().getBlockState(pContext.getClickedPos().relative(pContext.getClickedFace().getOpposite()));
      if (placedOn.getBlock() == this && (pContext.getPlayer() == null || !pContext.getPlayer().isShiftKeyDown())) {
         stateForPlacement = (BlockState)stateForPlacement.setValue(AXIS, (Axis)placedOn.getValue(AXIS));
      }

      return stateForPlacement;
   }
}
