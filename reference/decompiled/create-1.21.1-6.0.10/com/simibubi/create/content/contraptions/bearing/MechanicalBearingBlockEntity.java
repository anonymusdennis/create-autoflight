package com.simibubi.create.content.contraptions.bearing;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IControlContraption;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import java.util.List;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class MechanicalBearingBlockEntity extends GeneratingKineticBlockEntity implements IBearingBlockEntity, IDisplayAssemblyExceptions {
   protected ScrollOptionBehaviour<IControlContraption.RotationMode> movementMode;
   protected ControlledContraptionEntity movedContraption;
   protected float angle;
   protected boolean running;
   protected boolean assembleNextTick;
   protected float clientAngleDiff;
   protected AssemblyException lastException;
   protected double sequencedAngleLimit;
   private float prevAngle;

   public MechanicalBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.setLazyTickRate(3);
      this.sequencedAngleLimit = -1.0;
   }

   @Override
   public boolean isWoodenTop() {
      return false;
   }

   @Override
   protected boolean syncSequenceContext() {
      return true;
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.movementMode = new ScrollOptionBehaviour<>(
         IControlContraption.RotationMode.class, CreateLang.translateDirect("contraptions.movement_mode"), this, this.getMovementModeSlot()
      );
      behaviours.add(this.movementMode);
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.CONTRAPTION_ACTORS});
   }

   @Override
   public void remove() {
      if (!this.level.isClientSide) {
         this.disassemble();
      }

      super.remove();
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putBoolean("Running", this.running);
      compound.putFloat("Angle", this.angle);
      if (this.sequencedAngleLimit >= 0.0) {
         compound.putDouble("SequencedAngleLimit", this.sequencedAngleLimit);
      }

      AssemblyException.write(compound, registries, this.lastException);
      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      if (this.wasMoved) {
         super.read(compound, registries, clientPacket);
      } else {
         float angleBefore = this.angle;
         this.running = compound.getBoolean("Running");
         this.angle = compound.getFloat("Angle");
         this.sequencedAngleLimit = compound.contains("SequencedAngleLimit") ? compound.getDouble("SequencedAngleLimit") : -1.0;
         this.lastException = AssemblyException.read(compound, registries);
         super.read(compound, registries, clientPacket);
         if (clientPacket) {
            if (this.running) {
               if (this.movedContraption == null || !this.movedContraption.isStalled()) {
                  this.clientAngleDiff = AngleHelper.getShortestAngleDiff((double)angleBefore, (double)this.angle);
                  this.angle = angleBefore;
               }
            } else {
               this.movedContraption = null;
            }
         }
      }
   }

   @Override
   public float getInterpolatedAngle(float partialTicks) {
      if (this.isVirtual()) {
         return Mth.lerp(partialTicks + 0.5F, this.prevAngle, this.angle);
      } else {
         if (this.movedContraption == null || this.movedContraption.isStalled() || !this.running) {
            partialTicks = 0.0F;
         }

         float angularSpeed = this.getAngularSpeed();
         if (this.sequencedAngleLimit >= 0.0) {
            angularSpeed = (float)Mth.clamp((double)angularSpeed, -this.sequencedAngleLimit, this.sequencedAngleLimit);
         }

         return Mth.lerp(partialTicks, this.angle, this.angle + angularSpeed);
      }
   }

   @Override
   public void onSpeedChanged(float prevSpeed) {
      super.onSpeedChanged(prevSpeed);
      this.assembleNextTick = true;
      this.sequencedAngleLimit = -1.0;
      if (this.movedContraption != null && Math.signum(prevSpeed) != Math.signum(this.getSpeed()) && prevSpeed != 0.0F) {
         if (!this.movedContraption.isStalled()) {
            this.angle = (float)Math.round(this.angle);
            this.applyRotation();
         }

         this.movedContraption.getContraption().stop(this.level);
      }

      if (!this.isWindmill() && this.sequenceContext != null && this.sequenceContext.instruction() == SequencerInstructions.TURN_ANGLE) {
         this.sequencedAngleLimit = this.sequenceContext.getEffectiveValue((double)this.getTheoreticalSpeed());
      }
   }

   public float getAngularSpeed() {
      float speed = convertToAngular(this.isWindmill() ? this.getGeneratedSpeed() : this.getSpeed());
      if (this.getSpeed() == 0.0F) {
         speed = 0.0F;
      }

      if (this.level.isClientSide) {
         speed *= ServerSpeedProvider.get();
         speed += this.clientAngleDiff / 3.0F;
      }

      return speed;
   }

   @Override
   public AssemblyException getLastAssemblyException() {
      return this.lastException;
   }

   protected boolean isWindmill() {
      return false;
   }

   @Override
   public BlockPos getBlockPosition() {
      return this.worldPosition;
   }

   public void assemble() {
      if (this.level.getBlockState(this.worldPosition).getBlock() instanceof BearingBlock) {
         Direction direction = (Direction)this.getBlockState().getValue(BearingBlock.FACING);
         BearingContraption contraption = new BearingContraption(this.isWindmill(), direction);

         try {
            if (!contraption.assemble(this.level, this.worldPosition)) {
               return;
            }

            this.lastException = null;
         } catch (AssemblyException var4) {
            this.lastException = var4;
            this.sendData();
            return;
         }

         if (this.isWindmill()) {
            this.award(AllAdvancements.WINDMILL);
         }

         if (contraption.getSailBlocks() >= 128) {
            this.award(AllAdvancements.WINDMILL_MAXED);
         }

         contraption.removeBlocksFromWorld(this.level, BlockPos.ZERO);
         this.movedContraption = ControlledContraptionEntity.create(this.level, this, contraption);
         BlockPos anchor = this.worldPosition.relative(direction);
         this.movedContraption.setPos((double)anchor.getX(), (double)anchor.getY(), (double)anchor.getZ());
         this.movedContraption.setRotationAxis(direction.getAxis());
         this.level.addFreshEntity(this.movedContraption);
         AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(this.level, this.worldPosition);
         if (contraption.containsBlockBreakers()) {
            this.award(AllAdvancements.CONTRAPTION_ACTORS);
         }

         this.running = true;
         this.angle = 0.0F;
         this.sendData();
         this.updateGeneratedRotation();
      }
   }

   public void disassemble() {
      if (this.running || this.movedContraption != null) {
         this.angle = 0.0F;
         this.sequencedAngleLimit = -1.0;
         if (this.isWindmill()) {
            this.applyRotation();
         }

         if (this.movedContraption != null) {
            this.movedContraption.disassemble();
            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(this.level, this.worldPosition);
         }

         this.movedContraption = null;
         this.running = false;
         this.updateGeneratedRotation();
         this.assembleNextTick = false;
         this.sendData();
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.prevAngle = this.angle;
      if (this.level.isClientSide) {
         this.clientAngleDiff /= 2.0F;
      }

      if (!this.level.isClientSide && this.assembleNextTick) {
         this.assembleNextTick = false;
         if (!this.running) {
            if (this.speed == 0.0F && !this.isWindmill()) {
               return;
            }

            this.assemble();
         } else {
            boolean canDisassemble = this.movementMode.get() == IControlContraption.RotationMode.ROTATE_PLACE
               || this.isNearInitialAngle() && this.movementMode.get() == IControlContraption.RotationMode.ROTATE_PLACE_RETURNED;
            if (this.speed == 0.0F && (canDisassemble || this.movedContraption == null || this.movedContraption.getContraption().getBlocks().isEmpty())) {
               if (this.movedContraption != null) {
                  this.movedContraption.getContraption().stop(this.level);
               }

               this.disassemble();
               return;
            }
         }
      }

      if (this.running) {
         if (this.movedContraption == null || !this.movedContraption.isStalled()) {
            float angularSpeed = this.getAngularSpeed();
            if (this.sequencedAngleLimit >= 0.0) {
               angularSpeed = (float)Mth.clamp((double)angularSpeed, -this.sequencedAngleLimit, this.sequencedAngleLimit);
               this.sequencedAngleLimit = Math.max(0.0, this.sequencedAngleLimit - (double)Math.abs(angularSpeed));
            }

            float newAngle = this.angle + angularSpeed;
            this.angle = newAngle % 360.0F;
         }

         this.applyRotation();
      }
   }

   public boolean isNearInitialAngle() {
      return (double)Math.abs(this.angle) < 22.5 || (double)Math.abs(this.angle) > 337.5;
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      if (this.movedContraption != null && !this.level.isClientSide) {
         this.sendData();
      }
   }

   protected void applyRotation() {
      if (this.movedContraption != null) {
         this.movedContraption.setAngle(this.angle);
         BlockState blockState = this.getBlockState();
         if (blockState.hasProperty(BlockStateProperties.FACING)) {
            this.movedContraption.setRotationAxis(((Direction)blockState.getValue(BlockStateProperties.FACING)).getAxis());
         }
      }
   }

   @Override
   public void attach(ControlledContraptionEntity contraption) {
      BlockState blockState = this.getBlockState();
      if (contraption.getContraption() instanceof BearingContraption) {
         if (blockState.hasProperty(BearingBlock.FACING)) {
            this.movedContraption = contraption;
            this.setChanged();
            BlockPos anchor = this.worldPosition.relative((Direction)blockState.getValue(BearingBlock.FACING));
            this.movedContraption.setPos((double)anchor.getX(), (double)anchor.getY(), (double)anchor.getZ());
            if (!this.level.isClientSide) {
               this.running = true;
               this.sendData();
            }
         }
      }
   }

   @Override
   public void onStall() {
      if (!this.level.isClientSide) {
         this.sendData();
      }
   }

   @Override
   public boolean isValid() {
      return !this.isRemoved();
   }

   @Override
   public boolean isAttachedTo(AbstractContraptionEntity contraption) {
      return this.movedContraption == contraption;
   }

   public boolean isRunning() {
      return this.running;
   }

   @Override
   public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      if (super.addToTooltip(tooltip, isPlayerSneaking)) {
         return true;
      } else if (isPlayerSneaking) {
         return false;
      } else if (!this.isWindmill() && this.getSpeed() == 0.0F) {
         return false;
      } else if (this.running) {
         return false;
      } else {
         BlockState state = this.getBlockState();
         if (!(state.getBlock() instanceof BearingBlock)) {
            return false;
         } else {
            BlockState attachedState = this.level.getBlockState(this.worldPosition.relative((Direction)state.getValue(BearingBlock.FACING)));
            if (attachedState.canBeReplaced()) {
               return false;
            } else {
               TooltipHelper.addHint(tooltip, "hint.empty_bearing");
               return true;
            }
         }
      }
   }

   @Override
   public void setAngle(float forcedAngle) {
      this.angle = forcedAngle;
   }

   public ControlledContraptionEntity getMovedContraption() {
      return this.movedContraption;
   }
}
