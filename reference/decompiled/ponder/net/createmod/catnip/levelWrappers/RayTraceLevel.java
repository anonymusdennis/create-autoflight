package net.createmod.catnip.levelWrappers;

import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class RayTraceLevel implements BlockGetter {
   private final LevelAccessor template;
   private final BiFunction<BlockPos, BlockState, BlockState> stateGetter;

   public RayTraceLevel(LevelAccessor template, BiFunction<BlockPos, BlockState, BlockState> stateGetter) {
      this.template = template;
      this.stateGetter = stateGetter;
   }

   public BlockEntity getBlockEntity(BlockPos pos) {
      return this.template.getBlockEntity(pos);
   }

   public BlockState getBlockState(BlockPos pos) {
      return this.stateGetter.apply(pos, this.template.getBlockState(pos));
   }

   public FluidState getFluidState(BlockPos pos) {
      return this.template.getFluidState(pos);
   }

   public int getHeight() {
      return this.template.getHeight();
   }

   public int getMinBuildHeight() {
      return this.template.getMinBuildHeight();
   }
}
