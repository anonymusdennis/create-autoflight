package com.simibubi.create.content.fluids.pipes;

import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class FluidPipeBlockEntity extends SmartBlockEntity implements TransformableBlockEntity {
   public FluidPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(new FluidPipeBlockEntity.StandardPipeFluidTransportBehaviour(this));
      behaviours.add(new BracketedBlockEntityBehaviour(this, this::canHaveBracket));
      this.registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
   }

   @Override
   public void transform(BlockEntity be, StructureTransform transform) {
      BracketedBlockEntityBehaviour bracketBehaviour = this.getBehaviour(BracketedBlockEntityBehaviour.TYPE);
      if (bracketBehaviour != null) {
         bracketBehaviour.transformBracket(transform);
      }
   }

   private boolean canHaveBracket(BlockState state) {
      return !(state.getBlock() instanceof EncasedPipeBlock);
   }

   class StandardPipeFluidTransportBehaviour extends FluidTransportBehaviour {
      public StandardPipeFluidTransportBehaviour(SmartBlockEntity be) {
         super(be);
      }

      @Override
      public boolean canHaveFlowToward(BlockState state, Direction direction) {
         return (FluidPipeBlock.isPipe(state) || state.getBlock() instanceof EncasedPipeBlock)
            && (Boolean)state.getValue((Property)FluidPipeBlock.PROPERTY_BY_DIRECTION.get(direction));
      }

      @Override
      public FluidTransportBehaviour.AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
         FluidTransportBehaviour.AttachmentTypes attachment = super.getRenderedRimAttachment(world, pos, state, direction);
         BlockPos offsetPos = pos.relative(direction);
         BlockState otherState = world.getBlockState(offsetPos);
         if (state.getBlock() instanceof EncasedPipeBlock && attachment != FluidTransportBehaviour.AttachmentTypes.DRAIN) {
            return FluidTransportBehaviour.AttachmentTypes.NONE;
         } else {
            if (attachment == FluidTransportBehaviour.AttachmentTypes.RIM) {
               if (!FluidPipeBlock.isPipe(otherState)
                  && !(otherState.getBlock() instanceof EncasedPipeBlock)
                  && !(otherState.getBlock() instanceof GlassFluidPipeBlock)) {
                  FluidTransportBehaviour pipeBehaviour = BlockEntityBehaviour.get(world, offsetPos, FluidTransportBehaviour.TYPE);
                  if (pipeBehaviour != null && pipeBehaviour.canHaveFlowToward(otherState, direction.getOpposite())) {
                     return FluidTransportBehaviour.AttachmentTypes.DETAILED_CONNECTION;
                  }
               }

               if (!FluidPipeBlock.shouldDrawRim(world, pos, state, direction)) {
                  return FluidPropagator.getStraightPipeAxis(state) == direction.getAxis()
                     ? FluidTransportBehaviour.AttachmentTypes.CONNECTION
                     : FluidTransportBehaviour.AttachmentTypes.DETAILED_CONNECTION;
               }
            }

            return attachment == FluidTransportBehaviour.AttachmentTypes.NONE && state.getValue((Property)FluidPipeBlock.PROPERTY_BY_DIRECTION.get(direction))
               ? FluidTransportBehaviour.AttachmentTypes.DETAILED_CONNECTION
               : attachment;
         }
      }
   }
}
