package com.simibubi.create.content.decoration.bracket;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import java.util.Optional;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class BracketBlock extends WrenchableDirectionalBlock {
   public static final BooleanProperty AXIS_ALONG_FIRST_COORDINATE = DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;
   public static final EnumProperty<BracketBlock.BracketType> TYPE = EnumProperty.create("type", BracketBlock.BracketType.class);

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{AXIS_ALONG_FIRST_COORDINATE}).add(new Property[]{TYPE}));
   }

   public BracketBlock(Properties properties) {
      super(properties);
   }

   public Optional<BlockState> getSuitableBracket(BlockState blockState, Direction direction) {
      return blockState.getBlock() instanceof AbstractSimpleShaftBlock
         ? this.getSuitableBracket(
            (Axis)blockState.getValue(RotatedPillarKineticBlock.AXIS),
            direction,
            blockState.getBlock() instanceof CogWheelBlock ? BracketBlock.BracketType.COG : BracketBlock.BracketType.SHAFT
         )
         : this.getSuitableBracket(FluidPropagator.getStraightPipeAxis(blockState), direction, BracketBlock.BracketType.PIPE);
   }

   private Optional<BlockState> getSuitableBracket(Axis targetBlockAxis, Direction direction, BracketBlock.BracketType type) {
      Axis axis = direction.getAxis();
      if (targetBlockAxis != null && targetBlockAxis != axis) {
         boolean alongFirst = axis != Axis.Z ? targetBlockAxis == Axis.Z : targetBlockAxis == Axis.Y;
         return Optional.of(
            (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(TYPE, type)).setValue(FACING, direction))
               .setValue(AXIS_ALONG_FIRST_COORDINATE, !alongFirst)
         );
      } else {
         return Optional.empty();
      }
   }

   @Override
   public BlockState rotate(BlockState state, Rotation rot) {
      if (rot.ordinal() % 2 == 1) {
         state = (BlockState)state.cycle(AXIS_ALONG_FIRST_COORDINATE);
      }

      return super.rotate(state, rot);
   }

   public static enum BracketType implements StringRepresentable {
      PIPE,
      COG,
      SHAFT;

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}
