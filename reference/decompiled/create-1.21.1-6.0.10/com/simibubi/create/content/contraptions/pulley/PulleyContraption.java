package com.simibubi.create.content.contraptions.pulley;

import com.simibubi.create.AllContraptionTypes;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.TranslatingContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class PulleyContraption extends TranslatingContraption {
   int initialOffset;

   @Override
   public ContraptionType getType() {
      return (ContraptionType)AllContraptionTypes.PULLEY.value();
   }

   public PulleyContraption() {
   }

   public PulleyContraption(int initialOffset) {
      this.initialOffset = initialOffset;
   }

   @Override
   public boolean assemble(Level world, BlockPos pos) throws AssemblyException {
      if (!this.searchMovedStructure(world, pos, null)) {
         return false;
      } else {
         this.startMoving(world);
         return true;
      }
   }

   @Override
   protected boolean isAnchoringBlockAt(BlockPos pos) {
      if (pos.getX() == this.anchor.getX() && pos.getZ() == this.anchor.getZ()) {
         int y = pos.getY();
         return y > this.anchor.getY() && y <= this.anchor.getY() + this.initialOffset + 1;
      } else {
         return false;
      }
   }

   @Override
   public CompoundTag writeNBT(Provider registries, boolean spawnPacket) {
      CompoundTag tag = super.writeNBT(registries, spawnPacket);
      tag.putInt("InitialOffset", this.initialOffset);
      return tag;
   }

   @Override
   public void readNBT(Level world, CompoundTag nbt, boolean spawnData) {
      this.initialOffset = nbt.getInt("InitialOffset");
      super.readNBT(world, nbt, spawnData);
   }

   public int getInitialOffset() {
      return this.initialOffset;
   }
}
