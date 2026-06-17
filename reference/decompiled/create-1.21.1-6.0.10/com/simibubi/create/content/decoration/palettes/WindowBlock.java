package com.simibubi.create.content.decoration.palettes;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class WindowBlock extends ConnectedGlassBlock {
   protected final boolean translucent;

   public WindowBlock(Properties p_i48392_1_, boolean translucent) {
      super(p_i48392_1_);
      this.translucent = translucent;
   }

   public boolean isTranslucent() {
      return this.translucent;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
      if (state.getBlock() == adjacentBlockState.getBlock()) {
         return true;
      } else {
         if (state.getBlock() instanceof WindowBlock windowBlock && adjacentBlockState.getBlock() instanceof ConnectedGlassBlock) {
            return !windowBlock.isTranslucent() && side.getAxis().isHorizontal();
         }

         return super.skipRendering(state, adjacentBlockState, side);
      }
   }
}
