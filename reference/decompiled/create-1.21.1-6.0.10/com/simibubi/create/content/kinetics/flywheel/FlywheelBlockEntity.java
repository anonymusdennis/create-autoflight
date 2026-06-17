package com.simibubi.create.content.kinetics.flywheel;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class FlywheelBlockEntity extends KineticBlockEntity {
   LerpedFloat visualSpeed = LerpedFloat.linear();
   float angle;

   public FlywheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   protected AABB createRenderBoundingBox() {
      return super.createRenderBoundingBox().inflate(2.0);
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      if (clientPacket) {
         this.visualSpeed.chase((double)this.getGeneratedSpeed(), 0.015625, Chaser.EXP);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level.isClientSide) {
         float targetSpeed = this.getSpeed();
         this.visualSpeed.updateChaseTarget(targetSpeed);
         this.visualSpeed.tickChaser();
         this.angle = this.angle + this.visualSpeed.getValue() * 3.0F / 10.0F;
         this.angle %= 360.0F;
      }
   }
}
