package com.simibubi.create.content.fluids.pipes;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class StraightPipeBlockEntity extends SmartBlockEntity {
   public StraightPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(new StraightPipeBlockEntity.StraightPipeFluidTransportBehaviour(this));
      behaviours.add(new BracketedBlockEntityBehaviour(this));
      this.registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
   }

   public static class StraightPipeFluidTransportBehaviour extends FluidTransportBehaviour {
      public StraightPipeFluidTransportBehaviour(SmartBlockEntity be) {
         super(be);
      }

      @Override
      public boolean canHaveFlowToward(BlockState state, Direction direction) {
         return state.hasProperty(AxisPipeBlock.AXIS) && state.getValue(AxisPipeBlock.AXIS) == direction.getAxis();
      }

      @Override
      public FluidTransportBehaviour.AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
         FluidTransportBehaviour.AttachmentTypes attachment = super.getRenderedRimAttachment(world, pos, state, direction);
         BlockState otherState = world.getBlockState(pos.relative(direction));
         Axis axis = IAxisPipe.getAxisOf(state);
         Axis otherAxis = IAxisPipe.getAxisOf(otherState);
         if (attachment == FluidTransportBehaviour.AttachmentTypes.RIM && state.getBlock() instanceof FluidValveBlock) {
            return FluidTransportBehaviour.AttachmentTypes.NONE;
         } else if (attachment == FluidTransportBehaviour.AttachmentTypes.RIM
            && !(state.getBlock() instanceof GlassFluidPipeBlock)
            && otherState.getBlock() instanceof GlassFluidPipeBlock) {
            return FluidTransportBehaviour.AttachmentTypes.PARTIAL_RIM;
         } else if (attachment == FluidTransportBehaviour.AttachmentTypes.RIM && FluidPipeBlock.isPipe(otherState)) {
            return FluidTransportBehaviour.AttachmentTypes.NONE;
         } else if (axis == otherAxis && axis != null) {
            return FluidTransportBehaviour.AttachmentTypes.NONE;
         } else {
            return otherState.getBlock() instanceof FluidValveBlock && FluidValveBlock.getPipeAxis(otherState) == direction.getAxis()
               ? FluidTransportBehaviour.AttachmentTypes.NONE
               : attachment.withoutConnector();
         }
      }
   }
}
