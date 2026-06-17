package com.simibubi.create.foundation.utility.worldWrappers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class WrappedBlockAndTintGetter implements BlockAndTintGetter {
   protected final BlockAndTintGetter wrapped;

   public WrappedBlockAndTintGetter(BlockAndTintGetter wrapped) {
      this.wrapped = wrapped;
   }

   public BlockEntity getBlockEntity(BlockPos pos) {
      return this.wrapped.getBlockEntity(pos);
   }

   public BlockState getBlockState(BlockPos pos) {
      return this.wrapped.getBlockState(pos);
   }

   public FluidState getFluidState(BlockPos pos) {
      return this.wrapped.getFluidState(pos);
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
      return this.wrapped.getModelData(pPos);
   }
}
