package com.simibubi.create.content.fluids.pipes.valve;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidValveBlockEntity extends KineticBlockEntity {
   LerpedFloat pointer = LerpedFloat.linear().startWithValue(0.0).chase(0.0, 0.0, Chaser.LINEAR);

   public FluidValveBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
   }

   @Override
   public void onSpeedChanged(float previousSpeed) {
      super.onSpeedChanged(previousSpeed);
      float speed = this.getSpeed();
      this.pointer.chase(speed > 0.0F ? 1.0 : 0.0, (double)this.getChaseSpeed(), Chaser.LINEAR);
      this.sendData();
   }

   @Override
   public void tick() {
      super.tick();
      this.pointer.tickChaser();
      if (!this.level.isClientSide) {
         BlockState blockState = this.getBlockState();
         if (blockState.getBlock() instanceof FluidValveBlock) {
            boolean stateOpen = (Boolean)blockState.getValue(FluidValveBlock.ENABLED);
            if (stateOpen && this.pointer.getValue() == 0.0F) {
               switchToBlockState(this.level, this.worldPosition, (BlockState)blockState.setValue(FluidValveBlock.ENABLED, false));
            } else if (!stateOpen && this.pointer.getValue() == 1.0F) {
               switchToBlockState(this.level, this.worldPosition, (BlockState)blockState.setValue(FluidValveBlock.ENABLED, true));
            }
         }
      }
   }

   private float getChaseSpeed() {
      return Mth.clamp(Math.abs(this.getSpeed()) / 16.0F / 20.0F, 0.0F, 1.0F);
   }

   @Override
   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.put("Pointer", this.pointer.writeNBT());
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.pointer.readNBT(compound.getCompound("Pointer"), clientPacket);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(new FluidValveBlockEntity.ValvePipeBehaviour(this));
      this.registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
   }

   class ValvePipeBehaviour extends StraightPipeBlockEntity.StraightPipeFluidTransportBehaviour {
      public ValvePipeBehaviour(SmartBlockEntity be) {
         super(be);
      }

      @Override
      public boolean canHaveFlowToward(BlockState state, Direction direction) {
         return FluidValveBlock.getPipeAxis(state) == direction.getAxis();
      }

      @Override
      public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction) {
         return state.hasProperty(FluidValveBlock.ENABLED) && state.getValue(FluidValveBlock.ENABLED) ? super.canPullFluidFrom(fluid, state, direction) : false;
      }
   }
}
