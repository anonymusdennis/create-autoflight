package com.simibubi.create.content.contraptions.gantry;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GantryCarriageBlockEntity extends KineticBlockEntity implements IDisplayAssemblyExceptions {
   boolean assembleNextTick;
   protected AssemblyException lastException;

   public GantryCarriageBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.CONTRAPTION_ACTORS});
   }

   @Override
   public void onSpeedChanged(float previousSpeed) {
      super.onSpeedChanged(previousSpeed);
   }

   public void checkValidGantryShaft() {
      if (this.shouldAssemble()) {
         this.queueAssembly();
      }
   }

   @Override
   public void initialize() {
      super.initialize();
      if (!this.getBlockState().canSurvive(this.level, this.worldPosition)) {
         this.level.destroyBlock(this.worldPosition, true);
      }
   }

   public void queueAssembly() {
      this.assembleNextTick = true;
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level.isClientSide) {
         if (this.assembleNextTick) {
            this.tryAssemble();
            this.assembleNextTick = false;
         }
      }
   }

   @Override
   public AssemblyException getLastAssemblyException() {
      return this.lastException;
   }

   private void tryAssemble() {
      BlockState blockState = this.getBlockState();
      if (blockState.getBlock() instanceof GantryCarriageBlock) {
         Direction direction = (Direction)blockState.getValue(GantryCarriageBlock.FACING);
         GantryContraption contraption = new GantryContraption(direction);
         if (this.level.getBlockEntity(this.worldPosition.relative(direction.getOpposite())) instanceof GantryShaftBlockEntity shaftBE) {
            BlockState shaftState = shaftBE.getBlockState();
            if (AllBlocks.GANTRY_SHAFT.has(shaftState)) {
               float pinionMovementSpeed = shaftBE.getPinionMovementSpeed();
               Direction shaftOrientation = (Direction)shaftState.getValue(GantryShaftBlock.FACING);
               Direction movementDirection = shaftOrientation;
               if (pinionMovementSpeed < 0.0F) {
                  movementDirection = shaftOrientation.getOpposite();
               }

               try {
                  this.lastException = null;
                  if (!contraption.assemble(this.level, this.worldPosition)) {
                     return;
                  }

                  this.sendData();
               } catch (AssemblyException var12) {
                  this.lastException = var12;
                  this.sendData();
                  return;
               }

               if (!ContraptionCollider.isCollidingWithWorld(this.level, contraption, this.worldPosition.relative(movementDirection), movementDirection)) {
                  if (contraption.containsBlockBreakers()) {
                     this.award(AllAdvancements.CONTRAPTION_ACTORS);
                  }

                  contraption.removeBlocksFromWorld(this.level, BlockPos.ZERO);
                  GantryContraptionEntity movedContraption = GantryContraptionEntity.create(this.level, contraption, shaftOrientation);
                  BlockPos anchor = this.worldPosition;
                  movedContraption.setPos((double)anchor.getX(), (double)anchor.getY(), (double)anchor.getZ());
                  AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(this.level, this.worldPosition);
                  this.level.addFreshEntity(movedContraption);
                  if (shaftBE.sequenceContext != null && shaftBE.sequenceContext.instruction() == SequencerInstructions.TURN_DISTANCE) {
                     movedContraption.limitMovement(shaftBE.sequenceContext.getEffectiveValue((double)shaftBE.getTheoreticalSpeed()));
                  }
               }
            }
         }
      }
   }

   @Override
   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      AssemblyException.write(compound, registries, this.lastException);
      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.lastException = AssemblyException.read(compound, registries);
      super.read(compound, registries, clientPacket);
   }

   @Override
   public float propagateRotationTo(
      KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs
   ) {
      float defaultModifier = super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
      if (connectedViaAxes) {
         return defaultModifier;
      } else if (!AllBlocks.GANTRY_SHAFT.has(stateTo)) {
         return defaultModifier;
      } else if (!(Boolean)stateTo.getValue(GantryShaftBlock.POWERED)) {
         return defaultModifier;
      } else {
         Direction direction = Direction.getNearest((float)diff.getX(), (float)diff.getY(), (float)diff.getZ());
         return stateFrom.getValue(GantryCarriageBlock.FACING) != direction.getOpposite()
            ? defaultModifier
            : getGantryPinionModifier((Direction)stateTo.getValue(GantryShaftBlock.FACING), (Direction)stateFrom.getValue(GantryCarriageBlock.FACING));
      }
   }

   public static float getGantryPinionModifier(Direction shaft, Direction pinionDirection) {
      Axis shaftAxis = shaft.getAxis();
      float directionModifier = (float)shaft.getAxisDirection().getStep();
      if (shaftAxis != Axis.Y || pinionDirection != Direction.NORTH && pinionDirection != Direction.EAST) {
         if (shaftAxis != Axis.X || pinionDirection != Direction.DOWN && pinionDirection != Direction.SOUTH) {
            return shaftAxis != Axis.Z || pinionDirection != Direction.UP && pinionDirection != Direction.WEST ? directionModifier : -directionModifier;
         } else {
            return -directionModifier;
         }
      } else {
         return -directionModifier;
      }
   }

   private boolean shouldAssemble() {
      BlockState blockState = this.getBlockState();
      if (!(blockState.getBlock() instanceof GantryCarriageBlock)) {
         return false;
      } else {
         Direction facing = ((Direction)blockState.getValue(GantryCarriageBlock.FACING)).getOpposite();
         BlockState shaftState = this.level.getBlockState(this.worldPosition.relative(facing));
         if (!(shaftState.getBlock() instanceof GantryShaftBlock)) {
            return false;
         } else if ((Boolean)shaftState.getValue(GantryShaftBlock.POWERED)) {
            return false;
         } else {
            BlockEntity be = this.level.getBlockEntity(this.worldPosition.relative(facing));
            return be instanceof GantryShaftBlockEntity && ((GantryShaftBlockEntity)be).canAssembleOn();
         }
      }
   }
}
