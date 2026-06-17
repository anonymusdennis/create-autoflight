package com.simibubi.create.content.redstone.deskBell;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DeskBellBlockEntity extends SmartBlockEntity {
   public LerpedFloat animation = LerpedFloat.linear().startWithValue(0.0);
   public boolean ding;
   int blockStateTimer = 0;
   float animationOffset;

   public DeskBellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void tick() {
      super.tick();
      this.animation.tickChaser();
      if (!this.level.isClientSide) {
         if (this.blockStateTimer != 0) {
            this.blockStateTimer--;
            if (this.blockStateTimer <= 0) {
               BlockState blockState = this.getBlockState();
               if ((Boolean)blockState.getValue(DeskBellBlock.POWERED)) {
                  ((DeskBellBlock)AllBlocks.DESK_BELL.get()).unPress(blockState, this.level, this.worldPosition);
               }
            }
         }
      }
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      if (clientPacket && this.ding) {
         NBTHelper.putMarker(tag, "Ding");
      }

      this.ding = false;
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      if (clientPacket && tag.contains("Ding")) {
         this.ding();
      }
   }

   public void ding() {
      if (!this.level.isClientSide) {
         this.blockStateTimer = 20;
         this.ding = true;
         this.sendData();
      } else {
         this.animationOffset = this.level.random.nextFloat() * 2.0F * (float) Math.PI;
         this.animation.startWithValue(1.0).chase(0.0, 0.05, Chaser.LINEAR);
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }
}
