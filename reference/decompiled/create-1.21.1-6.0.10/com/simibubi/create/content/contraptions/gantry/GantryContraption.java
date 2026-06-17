package com.simibubi.create.content.contraptions.gantry;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllContraptionTypes;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.TranslatingContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class GantryContraption extends TranslatingContraption {
   protected Direction facing;

   public GantryContraption() {
   }

   public GantryContraption(Direction facing) {
      this.facing = facing;
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
   protected boolean isAnchoringBlockAt(BlockPos pos) {
      return super.isAnchoringBlockAt(pos.relative(this.facing));
   }

   @Override
   public ContraptionType getType() {
      return (ContraptionType)AllContraptionTypes.GANTRY.value();
   }

   public Direction getFacing() {
      return this.facing;
   }

   @Override
   protected boolean shouldUpdateAfterMovement(StructureBlockInfo info) {
      return super.shouldUpdateAfterMovement(info) && !AllBlocks.GANTRY_CARRIAGE.has(info.state());
   }
}
