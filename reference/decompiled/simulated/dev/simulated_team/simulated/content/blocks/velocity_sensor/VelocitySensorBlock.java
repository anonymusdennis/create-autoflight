package dev.simulated_team.simulated.content.blocks.velocity_sensor;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VelocitySensorBlock extends AbstractDirectionalAxisBlock implements IBE<VelocitySensorBlockEntity>, CommonRedstoneBlock {
   public static final MapCodec<VelocitySensorBlock> CODEC = simpleCodec(VelocitySensorBlock::new);
   public static IntegerProperty POWERED = IntegerProperty.create("powered", 0, 2);
   private static final VelocitySensorShaper VELOCITY_SENSOR = VelocitySensorShaper.make();

   public VelocitySensorBlock(Properties properties) {
      super(properties);
   }

   protected MapCodec<? extends DirectionalBlock> codec() {
      return CODEC;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)super.getStateForPlacement(context).setValue(POWERED, 0);
   }

   protected int getSignal(@NotNull BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction) {
      int powered = (Integer)blockState.getValue(POWERED);
      if (powered == 0) {
         return 0;
      } else {
         Direction positiveDir = getDirectionOfAxis(blockState);
         if (powered == 2) {
            positiveDir = positiveDir.getOpposite();
         }

         if (direction != positiveDir) {
            return 0;
         } else {
            int power = 0;
            if (blockState.hasBlockEntity()) {
               power = ((VelocitySensorBlockEntity)blockGetter.getBlockEntity(blockPos)).getRedstoneStrength();
            }

            return power;
         }
      }
   }

   protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      Direction facing = (Direction)state.getValue(FACING);
      if (!facing.getAxis().isHorizontal()) {
         return 0;
      } else if (facing.getAxis() == Direction.EAST.getAxis() && !(Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE)) {
         return 0;
      } else {
         return facing.getAxis() == Direction.NORTH.getAxis() && state.getValue(AXIS_ALONG_FIRST_COORDINATE) ? 0 : this.getSignal(state, level, pos, direction);
      }
   }

   @Override
   public boolean commonConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
      return direction != null && getAxis(state) == direction.getAxis();
   }

   protected boolean isSignalSource(BlockState blockState) {
      return true;
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{POWERED}));
   }

   public Class<VelocitySensorBlockEntity> getBlockEntityClass() {
      return VelocitySensorBlockEntity.class;
   }

   public BlockEntityType<? extends VelocitySensorBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends VelocitySensorBlockEntity>)SimBlockEntityTypes.VELOCITY_SENSOR.get();
   }

   public VoxelShape getShape(BlockState state, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return VELOCITY_SENSOR.get((Direction)state.getValue(FACING), (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE));
   }
}
