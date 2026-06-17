package com.simibubi.create.content.decoration.copycat;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.model.data.ModelData;

public class FilteredBlockAndTintGetter implements BlockAndTintGetter {
   private BlockAndTintGetter wrapped;
   private Predicate<BlockPos> filter;

   public FilteredBlockAndTintGetter(BlockAndTintGetter wrapped, Predicate<BlockPos> filter) {
      this.wrapped = wrapped;
      this.filter = filter;
   }

   public BlockEntity getBlockEntity(BlockPos pPos) {
      return this.filter.test(pPos) ? this.wrapped.getBlockEntity(pPos) : null;
   }

   public BlockState getBlockState(BlockPos pPos) {
      return this.filter.test(pPos) ? this.wrapped.getBlockState(pPos) : Blocks.AIR.defaultBlockState();
   }

   public FluidState getFluidState(BlockPos pPos) {
      return this.filter.test(pPos) ? this.wrapped.getFluidState(pPos) : Fluids.EMPTY.defaultFluidState();
   }

   public int getHeight() {
      return this.wrapped.getHeight();
   }

   public int getMinBuildHeight() {
      return this.wrapped.getMinBuildHeight();
   }

   public float getShade(Direction pDirection, boolean pShade) {
      return this.wrapped.getShade(pDirection, pShade);
   }

   public LevelLightEngine getLightEngine() {
      return this.wrapped.getLightEngine();
   }

   public int getBlockTint(BlockPos pBlockPos, ColorResolver pColorResolver) {
      return this.wrapped.getBlockTint(pBlockPos, pColorResolver);
   }

   public ModelData getModelData(BlockPos pPos) {
      return this.filter.test(pPos) ? this.wrapped.getModelData(pPos) : ModelData.EMPTY;
   }
}
