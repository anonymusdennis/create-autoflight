package com.simibubi.create.content.contraptions.bearing;

import com.simibubi.create.AllContraptionTypes;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class StabilizedContraption extends Contraption {
   private Direction facing;

   public StabilizedContraption() {
   }

   public StabilizedContraption(Direction facing) {
      this.facing = facing;
   }

   @Override
   public boolean assemble(Level world, BlockPos pos) throws AssemblyException {
      BlockPos offset = pos.relative(this.facing);
      if (!this.searchMovedStructure(world, offset, null)) {
         return false;
      } else {
         this.startMoving(world);
         return !this.blocks.isEmpty();
      }
   }

   @Override
   protected boolean isAnchoringBlockAt(BlockPos pos) {
      return false;
   }

   @Override
   public ContraptionType getType() {
      return (ContraptionType)AllContraptionTypes.STABILIZED.value();
   }

   @Override
   public CompoundTag writeNBT(Provider registries, boolean spawnPacket) {
      CompoundTag tag = super.writeNBT(registries, spawnPacket);
      tag.putInt("Facing", this.facing.get3DDataValue());
      return tag;
   }

   @Override
   public void readNBT(Level world, CompoundTag tag, boolean spawnData) {
      this.facing = Direction.from3DDataValue(tag.getInt("Facing"));
      super.readNBT(world, tag, spawnData);
   }

   @Override
   public boolean canBeStabilized(Direction facing, BlockPos localPos) {
      return false;
   }

   public Direction getFacing() {
      return this.facing;
   }
}
