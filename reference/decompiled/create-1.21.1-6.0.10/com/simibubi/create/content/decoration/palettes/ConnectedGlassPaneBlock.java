package com.simibubi.create.content.decoration.palettes;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ConnectedGlassPaneBlock extends GlassPaneBlock {
   public ConnectedGlassPaneBlock(Properties builder) {
      super(builder);
   }

   @OnlyIn(Dist.CLIENT)
   public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
      return side.getAxis().isVertical() ? adjacentBlockState == state : super.skipRendering(state, adjacentBlockState, side);
   }
}
