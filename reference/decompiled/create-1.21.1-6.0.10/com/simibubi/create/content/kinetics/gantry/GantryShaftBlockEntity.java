package com.simibubi.create.content.kinetics.gantry;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GantryShaftBlockEntity extends KineticBlockEntity {
   public GantryShaftBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
   }

   @Override
   protected boolean syncSequenceContext() {
      return true;
   }

   public void checkAttachedCarriageBlocks() {
      if (this.canAssembleOn()) {
         for (Direction d : Iterate.directions) {
            if (d.getAxis() != ((Direction)this.getBlockState().getValue(GantryShaftBlock.FACING)).getAxis()) {
               BlockPos offset = this.worldPosition.relative(d);
               BlockState pinionState = this.level.getBlockState(offset);
               if (AllBlocks.GANTRY_CARRIAGE.has(pinionState) && pinionState.getValue(GantryCarriageBlock.FACING) == d) {
                  BlockEntity blockEntity = this.level.getBlockEntity(offset);
                  if (blockEntity instanceof GantryCarriageBlockEntity) {
                     ((GantryCarriageBlockEntity)blockEntity).queueAssembly();
                  }
               }
            }
         }
      }
   }

   @Override
   public void onSpeedChanged(float previousSpeed) {
      super.onSpeedChanged(previousSpeed);
      this.checkAttachedCarriageBlocks();
   }

   @Override
   public float propagateRotationTo(
      KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs
   ) {
      float defaultModifier = super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
      if (connectedViaAxes) {
         return defaultModifier;
      } else if (!(Boolean)stateFrom.getValue(GantryShaftBlock.POWERED)) {
         return defaultModifier;
      } else if (!AllBlocks.GANTRY_CARRIAGE.has(stateTo)) {
         return defaultModifier;
      } else {
         Direction direction = Direction.getNearest((float)diff.getX(), (float)diff.getY(), (float)diff.getZ());
         return stateTo.getValue(GantryCarriageBlock.FACING) != direction
            ? defaultModifier
            : GantryCarriageBlockEntity.getGantryPinionModifier(
               (Direction)stateFrom.getValue(GantryShaftBlock.FACING), (Direction)stateTo.getValue(GantryCarriageBlock.FACING)
            );
      }
   }

   @Override
   public boolean isCustomConnection(KineticBlockEntity other, BlockState state, BlockState otherState) {
      if (!AllBlocks.GANTRY_CARRIAGE.has(otherState)) {
         return false;
      } else {
         BlockPos diff = other.getBlockPos().subtract(this.worldPosition);
         Direction direction = Direction.getNearest((float)diff.getX(), (float)diff.getY(), (float)diff.getZ());
         return otherState.getValue(GantryCarriageBlock.FACING) == direction;
      }
   }

   public boolean canAssembleOn() {
      BlockState blockState = this.getBlockState();
      if (!AllBlocks.GANTRY_SHAFT.has(blockState)) {
         return false;
      } else if ((Boolean)blockState.getValue(GantryShaftBlock.POWERED)) {
         return false;
      } else {
         float speed = this.getPinionMovementSpeed();
         switch ((GantryShaftBlock.Part)blockState.getValue(GantryShaftBlock.PART)) {
            case END:
               return speed < 0.0F;
            case MIDDLE:
               return speed != 0.0F;
            case START:
               return speed > 0.0F;
            case SINGLE:
            default:
               return false;
         }
      }
   }

   public float getPinionMovementSpeed() {
      BlockState blockState = this.getBlockState();
      return !AllBlocks.GANTRY_SHAFT.has(blockState) ? 0.0F : Mth.clamp(convertToLinear(-this.getSpeed()), -0.49F, 0.49F);
   }

   @Override
   protected boolean isNoisy() {
      return false;
   }
}
