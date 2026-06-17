package dev.eriksonn.aeronautics.api.levitite_blend_crystallization;

import com.simibubi.create.foundation.utility.BlockHelper;
import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.index.AeroTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public interface CrystalPropagationContext {
   void onCrystallizationInitialize(Level var1, BlockPos var2, boolean var3);

   void onCrystallize(Level var1, BlockPos var2);

   default void onDefaultCrystallize(Level level, BlockPos pos) {
      if (!level.isClientSide) {
         level.setBlockAndUpdate(pos, this.getCrystalBlockState(level, pos));
         if ((Boolean)AeroConfig.server().blocks.breakBlocksOnCrystallize.get()) {
            for (Direction dir : Direction.values()) {
               if (level.getBlockState(pos.relative(dir)).is(AeroTags.BlockTags.LEVITITE_BREAKABLE)) {
                  boolean shouldBreak = true;

                  for (Direction dir2 : Direction.values()) {
                     if (level.getFluidState(pos.relative(dir).relative(dir2)).is(LevititeBlendHelper.getFluid())) {
                        shouldBreak = false;
                        break;
                     }
                  }

                  if (shouldBreak) {
                     BlockHelper.destroyBlock(level, pos.relative(dir), 1.0F);
                  }
               }
            }
         }
      }
   }

   void onCrystallizationFail(Level var1, BlockPos var2, int var3, boolean var4);

   BlockState getCrystalBlockState(Level var1, BlockPos var2);

   default int getNewAge(Level level, int attempts, boolean isDormant) {
      return level.random.nextInt(10, 40);
   }

   default boolean shouldCrystallize(Level level, int attempts, boolean isDormant) {
      float maxAttempts = isDormant ? 10.0F : 5.0F;
      return level.random.nextFloat() < (float)attempts / maxAttempts;
   }

   boolean canSpreadTo(FluidState var1);

   CrystalPropagationContext getContextForSpread(Level var1, BlockPos var2);

   TagKey<Block> getCatalyzerTag();
}
