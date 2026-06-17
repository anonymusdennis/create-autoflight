package com.simibubi.create.content.trains.signal;

import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class SingleBlockEntityEdgePoint extends TrackEdgePoint {
   public ResourceKey<Level> blockEntityDimension;
   public BlockPos blockEntityPos;

   public BlockPos getBlockEntityPos() {
      return this.blockEntityPos;
   }

   public ResourceKey<Level> getBlockEntityDimension() {
      return this.blockEntityDimension;
   }

   @Override
   public void blockEntityAdded(BlockEntity blockEntity, boolean front) {
      this.blockEntityPos = blockEntity.getBlockPos();
      this.blockEntityDimension = blockEntity.getLevel().dimension();
   }

   @Override
   public void blockEntityRemoved(BlockPos blockEntityPos, boolean front) {
      this.removeFromAllGraphs();
   }

   @Override
   public void invalidate(LevelAccessor level) {
      this.invalidateAt(level, this.blockEntityPos);
   }

   @Override
   public boolean canMerge() {
      return false;
   }

   @Override
   public void read(CompoundTag nbt, Provider registries, boolean migration, DimensionPalette dimensions) {
      super.read(nbt, registries, migration, dimensions);
      if (!migration) {
         this.blockEntityPos = NBTHelper.readBlockPos(nbt, "BlockEntityPos");
         this.blockEntityDimension = dimensions.decode(nbt.contains("BlockEntityDimension") ? nbt.getInt("BlockEntityDimension") : -1);
      }
   }

   @Override
   public void write(CompoundTag nbt, Provider registries, DimensionPalette dimensions) {
      super.write(nbt, registries, dimensions);
      nbt.put("BlockEntityPos", NbtUtils.writeBlockPos(this.blockEntityPos));
      nbt.putInt("BlockEntityDimension", dimensions.encode(this.blockEntityDimension));
   }
}
