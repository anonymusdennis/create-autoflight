package dev.simulated_team.simulated.content.blocks.lasers.laser_sensor;

import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class LaserSensorBlock extends DirectedDirectionalBlock implements IBE<LaserSensorBlockEntity>, CommonRedstoneBlock {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public LaserSensorBlock(Properties props) {
      super(props);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{POWERED}));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = this.defaultBlockState();
      Direction facing = context.getNearestLookingDirection();
      Direction preferredFacing = context.getPlayer() != null && context.getPlayer().isSteppingCarefully() ? facing : facing.getOpposite();
      if (preferredFacing.getAxis() == Axis.Y) {
         state = (BlockState)state.setValue(TARGET, preferredFacing == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR);
         preferredFacing = context.getHorizontalDirection();
      }

      return (BlockState)((BlockState)state.setValue(FACING, preferredFacing)).setValue(POWERED, false);
   }

   public int getSignal(BlockState pState, BlockGetter level, BlockPos pos, Direction pDirection) {
      int power = 0;
      LaserSensorBlockEntity blockEntity = (LaserSensorBlockEntity)this.getBlockEntity(level, pos);
      if (blockEntity != null) {
         power = Math.max(0, Math.min(15, blockEntity.currentPower));
      }

      return power;
   }

   public boolean isSignalSource(BlockState state) {
      return true;
   }

   @Override
   public boolean commonCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return false;
   }

   @Override
   public boolean commonConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
      return direction == null ? false : direction != ((Direction)state.getValue(FACING)).getOpposite();
   }

   public Class<LaserSensorBlockEntity> getBlockEntityClass() {
      return LaserSensorBlockEntity.class;
   }

   public BlockEntityType<? extends LaserSensorBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends LaserSensorBlockEntity>)SimBlockEntityTypes.LASER_SENSOR.get();
   }
}
