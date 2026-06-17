package com.simibubi.create.content.kinetics.crank;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class HandCrankBlockEntity extends GeneratingKineticBlockEntity {
   public int inUse;
   public boolean backwards;
   public float independentAngle;
   public float chasingAngularVelocity;

   public HandCrankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public void turn(boolean back) {
      boolean update = false;
      if (this.getGeneratedSpeed() == 0.0F || back != this.backwards) {
         update = true;
      }

      this.inUse = 10;
      this.backwards = back;
      if (update && !this.level.isClientSide) {
         this.updateGeneratedRotation();
      }
   }

   public float getIndependentAngle(float partialTicks) {
      return this.independentAngle + partialTicks * this.chasingAngularVelocity;
   }

   @Override
   public float getGeneratedSpeed() {
      if (this.getBlockState().getBlock() instanceof HandCrankBlock crank) {
         int speed = (this.inUse == 0 ? 0 : (this.clockwise() ? -1 : 1)) * crank.getRotationSpeed();
         return convertToDirection((float)speed, (Direction)this.getBlockState().getValue(HandCrankBlock.FACING));
      } else {
         return 0.0F;
      }
   }

   protected boolean clockwise() {
      return this.backwards;
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putInt("InUse", this.inUse);
      compound.putBoolean("Backwards", this.backwards);
      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.inUse = compound.getInt("InUse");
      this.backwards = compound.getBoolean("Backwards");
      super.read(compound, registries, clientPacket);
   }

   @Override
   public void tick() {
      super.tick();
      float actualAngularSpeed = KineticBlockEntity.convertToAngular(this.getSpeed());
      this.chasingAngularVelocity = this.chasingAngularVelocity + (actualAngularSpeed - this.chasingAngularVelocity) / 4.0F;
      this.independentAngle = this.independentAngle + this.chasingAngularVelocity;
      if (this.inUse > 0) {
         this.inUse--;
         if (this.inUse == 0 && !this.level.isClientSide) {
            this.sequenceContext = null;
            this.updateGeneratedRotation();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public SuperByteBuffer getRenderedHandle() {
      BlockState blockState = this.getBlockState();
      Direction facing = blockState.getOptionalValue(HandCrankBlock.FACING).orElse(Direction.UP);
      return CachedBuffers.partialFacing(AllPartialModels.HAND_CRANK_HANDLE, blockState, facing.getOpposite());
   }

   @OnlyIn(Dist.CLIENT)
   public boolean shouldRenderShaft() {
      return true;
   }

   @Override
   protected Block getStressConfigKey() {
      return AllBlocks.HAND_CRANK.has(this.getBlockState()) ? (Block)AllBlocks.HAND_CRANK.get() : (Block)AllBlocks.COPPER_VALVE_HANDLE.get();
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void tickAudio() {
      super.tickAudio();
      if (this.inUse > 0 && AnimationTickHolder.getTicks() % 10 == 0) {
         if (!AllBlocks.HAND_CRANK.has(this.getBlockState())) {
            return;
         }

         AllSoundEvents.CRANKING.playAt(this.level, this.worldPosition, (float)this.inUse / 2.5F, 0.65F + (float)(10 - this.inUse) / 10.0F, true);
      }
   }
}
