package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.chassis.StickerBlock;
import com.simibubi.create.content.contraptions.chassis.StickerBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class StickerPeripheral extends SyncedPeripheral<StickerBlockEntity> {
   public StickerPeripheral(StickerBlockEntity blockEntity) {
      super(blockEntity);
   }

   @LuaFunction
   public boolean isExtended() {
      return this.blockEntity.isBlockStateExtended();
   }

   @LuaFunction
   public boolean isAttachedToBlock() {
      return this.blockEntity.isBlockStateExtended() && this.blockEntity.isAttachedToBlock();
   }

   @LuaFunction(
      mainThread = true
   )
   public boolean extend() {
      BlockState state = this.blockEntity.getBlockState();
      if (AllBlocks.STICKER.has(state) && !(Boolean)state.getValue(StickerBlock.EXTENDED)) {
         this.blockEntity.getLevel().setBlock(this.blockEntity.getBlockPos(), (BlockState)state.setValue(StickerBlock.EXTENDED, true), 2);
         return true;
      } else {
         return false;
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public boolean retract() {
      BlockState state = this.blockEntity.getBlockState();
      if (AllBlocks.STICKER.has(state) && (Boolean)state.getValue(StickerBlock.EXTENDED)) {
         this.blockEntity.getLevel().setBlock(this.blockEntity.getBlockPos(), (BlockState)state.setValue(StickerBlock.EXTENDED, false), 2);
         return true;
      } else {
         return false;
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public boolean toggle() {
      BlockState state = this.blockEntity.getBlockState();
      if (!AllBlocks.STICKER.has(state)) {
         return false;
      } else {
         boolean extended = (Boolean)state.getValue(StickerBlock.EXTENDED);
         this.blockEntity.getLevel().setBlock(this.blockEntity.getBlockPos(), (BlockState)state.setValue(StickerBlock.EXTENDED, !extended), 2);
         return true;
      }
   }

   @NotNull
   public String getType() {
      return "Create_Sticker";
   }
}
