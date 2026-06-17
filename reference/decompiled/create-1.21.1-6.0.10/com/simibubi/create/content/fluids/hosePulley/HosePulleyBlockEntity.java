package com.simibubi.create.content.fluids.hosePulley;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;

public class HosePulleyBlockEntity extends KineticBlockEntity {
   LerpedFloat offset = LerpedFloat.linear().startWithValue(0.0);
   boolean isMoving = true;
   private SmartFluidTank internalTank = new SmartFluidTank(1500, this::onTankContentsChanged);
   private FluidDrainingBehaviour drainer;
   private FluidFillingBehaviour filler;
   private HosePulleyFluidHandler handler;
   private boolean infinite;

   public HosePulleyBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
      this.handler = new HosePulleyFluidHandler(
         this.internalTank, this.filler, this.drainer, () -> this.worldPosition.below((int)Math.ceil((double)this.offset.getValue())), () -> !this.isMoving
      );
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(
         FluidHandler.BLOCK,
         (BlockEntityType)AllBlockEntityTypes.HOSE_PULLEY.get(),
         (be, context) -> context != null && !HosePulleyBlock.hasPipeTowards(be.level, be.worldPosition, be.getBlockState(), context) ? null : be.handler
      );
   }

   @Override
   public void sendData() {
      this.infinite = this.filler.isInfinite() || this.drainer.isInfinite();
      super.sendData();
   }

   @Override
   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      boolean addToGoggleTooltip = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
      if (this.infinite) {
         TooltipHelper.addHint(tooltip, "hint.hose_pulley");
      }

      return addToGoggleTooltip;
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.drainer = new FluidDrainingBehaviour(this);
      this.filler = new FluidFillingBehaviour(this);
      behaviours.add(this.drainer);
      behaviours.add(this.filler);
      super.addBehaviours(behaviours);
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.HOSE_PULLEY, AllAdvancements.HOSE_PULLEY_LAVA});
   }

   protected void onTankContentsChanged(FluidStack contents) {
   }

   @Override
   public void onSpeedChanged(float previousSpeed) {
      this.isMoving = true;
      if (this.getSpeed() == 0.0F) {
         this.offset.forceNextSync();
         this.offset.setValue((double)Math.round(this.offset.getValue()));
         this.isMoving = false;
      }

      if (this.isMoving) {
         float newOffset = this.offset.getValue() + this.getMovementSpeed();
         if (newOffset < 0.0F) {
            this.isMoving = false;
         }

         if (!this.level.getBlockState(this.worldPosition.below((int)Math.ceil((double)newOffset))).canBeReplaced()) {
            this.isMoving = false;
         }

         if (this.isMoving) {
            this.drainer.reset();
            this.filler.reset();
         }
      }

      super.onSpeedChanged(previousSpeed);
   }

   @Override
   protected AABB createRenderBoundingBox() {
      return super.createRenderBoundingBox().expandTowards(0.0, (double)(-this.offset.getValue()), 0.0);
   }

   @Override
   public void tick() {
      super.tick();
      float newOffset = this.offset.getValue() + this.getMovementSpeed();
      if (newOffset < 0.0F) {
         newOffset = 0.0F;
         this.isMoving = false;
      }

      if (!this.level.getBlockState(this.worldPosition.below((int)Math.ceil((double)newOffset))).canBeReplaced()) {
         newOffset = (float)((int)newOffset);
         this.isMoving = false;
      }

      if (this.getSpeed() == 0.0F) {
         this.isMoving = false;
      }

      this.offset.setValue((double)newOffset);
      this.invalidateRenderBoundingBox();
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      if (!this.level.isClientSide) {
         if (!this.isMoving) {
            int ceil = (int)Math.ceil((double)(this.offset.getValue() + this.getMovementSpeed()));
            if (this.getMovementSpeed() > 0.0F && this.level.getBlockState(this.worldPosition.below(ceil)).canBeReplaced()) {
               this.isMoving = true;
               this.drainer.reset();
               this.filler.reset();
            } else {
               this.sendData();
            }
         }
      }
   }

   @Override
   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      if (clientPacket) {
         this.offset.forceNextSync();
      }

      compound.put("Offset", this.offset.writeNBT());
      compound.put("Tank", this.internalTank.writeToNBT(registries, new CompoundTag()));
      super.write(compound, registries, clientPacket);
      if (clientPacket) {
         compound.putBoolean("Infinite", this.infinite);
      }
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.offset.readNBT(compound.getCompound("Offset"), clientPacket);
      this.internalTank.readFromNBT(registries, compound.getCompound("Tank"));
      super.read(compound, registries, clientPacket);
      if (clientPacket) {
         this.infinite = compound.getBoolean("Infinite");
      }
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.invalidateCapabilities();
   }

   public float getMovementSpeed() {
      float movementSpeed = convertToLinear(this.getSpeed());
      if (this.level.isClientSide) {
         movementSpeed *= ServerSpeedProvider.get();
      }

      return movementSpeed;
   }

   public float getInterpolatedOffset(float pt) {
      return Math.max(this.offset.getValue(pt), 0.1875F);
   }
}
