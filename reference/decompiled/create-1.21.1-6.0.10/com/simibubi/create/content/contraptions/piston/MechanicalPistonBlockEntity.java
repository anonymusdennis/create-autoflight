package com.simibubi.create.content.contraptions.piston;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.DirectionalExtenderScrollOptionSlot;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class MechanicalPistonBlockEntity extends LinearActuatorBlockEntity {
   protected int extensionLength;

   public MechanicalPistonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.extensionLength = compound.getInt("ExtensionLength");
      super.read(compound, registries, clientPacket);
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      tag.putInt("ExtensionLength", this.extensionLength);
      super.write(tag, registries, clientPacket);
   }

   @Override
   public void assemble() throws AssemblyException {
      if (this.level.getBlockState(this.worldPosition).getBlock() instanceof MechanicalPistonBlock) {
         Direction direction = (Direction)this.getBlockState().getValue(BlockStateProperties.FACING);
         PistonContraption contraption = new PistonContraption(direction, this.getMovementSpeed() < 0.0F);
         if (contraption.assemble(this.level, this.worldPosition)) {
            Direction positive = Direction.get(AxisDirection.POSITIVE, direction.getAxis());
            Direction movementDirection = this.getSpeed() > 0.0F ^ direction.getAxis() != Axis.Z ? positive : positive.getOpposite();
            BlockPos anchor = contraption.anchor.relative(direction, contraption.initialExtensionProgress);
            if (!ContraptionCollider.isCollidingWithWorld(this.level, contraption, anchor.relative(movementDirection), movementDirection)) {
               this.extensionLength = contraption.extensionLength;
               float resultingOffset = (float)contraption.initialExtensionProgress + Math.signum(this.getMovementSpeed()) * 0.5F;
               if (!(resultingOffset <= 0.0F) && !(resultingOffset >= (float)this.extensionLength)) {
                  this.running = true;
                  this.offset = (float)contraption.initialExtensionProgress;
                  this.sendData();
                  this.clientOffsetDiff = 0.0F;
                  BlockPos startPos = BlockPos.ZERO.relative(direction, contraption.initialExtensionProgress);
                  contraption.removeBlocksFromWorld(this.level, startPos);
                  this.movedContraption = ControlledContraptionEntity.create(this.getLevel(), this, contraption);
                  this.resetContraptionToOffset();
                  this.forceMove = true;
                  this.level.addFreshEntity(this.movedContraption);
                  AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(this.level, this.worldPosition);
                  if (contraption.containsBlockBreakers()) {
                     this.award(AllAdvancements.CONTRAPTION_ACTORS);
                  }
               }
            }
         }
      }
   }

   @Override
   public void disassemble() {
      if (this.running || this.movedContraption != null) {
         if (!this.remove) {
            this.getLevel()
               .setBlock(
                  this.worldPosition, (BlockState)this.getBlockState().setValue(MechanicalPistonBlock.STATE, MechanicalPistonBlock.PistonState.EXTENDED), 19
               );
         }

         if (this.movedContraption != null) {
            this.resetContraptionToOffset();
            this.movedContraption.disassemble();
            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(this.level, this.worldPosition);
         }

         this.running = false;
         this.movedContraption = null;
         this.sendData();
         if (this.remove) {
            ((MechanicalPistonBlock)AllBlocks.MECHANICAL_PISTON.get()).playerWillDestroy(this.level, this.worldPosition, this.getBlockState(), null);
         }
      }
   }

   @Override
   protected void collided() {
      super.collided();
      if (!this.running && this.getMovementSpeed() > 0.0F) {
         this.assembleNextTick = true;
      }
   }

   @Override
   public float getMovementSpeed() {
      float movementSpeed = Mth.clamp(convertToLinear(this.getSpeed()), -0.49F, 0.49F);
      if (this.level.isClientSide) {
         movementSpeed *= ServerSpeedProvider.get();
      }

      Direction pistonDirection = (Direction)this.getBlockState().getValue(BlockStateProperties.FACING);
      int movementModifier = pistonDirection.getAxisDirection().getStep() * (pistonDirection.getAxis() == Axis.Z ? -1 : 1);
      movementSpeed = movementSpeed * (float)(-movementModifier) + this.clientOffsetDiff / 2.0F;
      int extensionRange = this.getExtensionRange();
      movementSpeed = Mth.clamp(movementSpeed, 0.0F - this.offset, (float)extensionRange - this.offset);
      if (this.sequencedOffsetLimit >= 0.0) {
         movementSpeed = (float)Mth.clamp((double)movementSpeed, -this.sequencedOffsetLimit, this.sequencedOffsetLimit);
      }

      return movementSpeed;
   }

   @Override
   protected int getExtensionRange() {
      return this.extensionLength;
   }

   @Override
   protected void visitNewPosition() {
   }

   @Override
   protected Vec3 toMotionVector(float speed) {
      Direction pistonDirection = (Direction)this.getBlockState().getValue(BlockStateProperties.FACING);
      return Vec3.atLowerCornerOf(pistonDirection.getNormal()).scale((double)speed);
   }

   @Override
   protected Vec3 toPosition(float offset) {
      Vec3 position = Vec3.atLowerCornerOf(((Direction)this.getBlockState().getValue(BlockStateProperties.FACING)).getNormal()).scale((double)offset);
      return position.add(Vec3.atLowerCornerOf(this.movedContraption.getContraption().anchor));
   }

   @Override
   protected ValueBoxTransform getMovementModeSlot() {
      return new DirectionalExtenderScrollOptionSlot((state, d) -> {
         Axis axis = d.getAxis();
         Axis extensionAxis = ((Direction)state.getValue(MechanicalPistonBlock.FACING)).getAxis();
         Axis shaftAxis = ((IRotate)state.getBlock()).getRotationAxis(state);
         return extensionAxis != axis && shaftAxis != axis;
      });
   }

   @Override
   protected int getInitialOffset() {
      return this.movedContraption == null ? 0 : ((PistonContraption)this.movedContraption.getContraption()).initialExtensionProgress;
   }
}
