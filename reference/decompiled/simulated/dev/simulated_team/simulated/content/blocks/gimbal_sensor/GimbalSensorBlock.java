package dev.simulated_team.simulated.content.blocks.gimbal_sensor;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GimbalSensorBlock extends Block implements IBE<GimbalSensorBlockEntity>, IWrenchable, CommonRedstoneBlock {
   public static final Property<Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;

   public GimbalSensorBlock(Properties pProperties) {
      super(pProperties);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{HORIZONTAL_AXIS});
      super.createBlockStateDefinition(builder);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(HORIZONTAL_AXIS, context.getHorizontalDirection().getClockWise().getAxis());
   }

   public boolean isSignalSource(BlockState state) {
      return true;
   }

   public int getSignal(BlockState pState, BlockGetter pLevel, BlockPos pPos, Direction side) {
      GimbalSensorBlockEntity be = (GimbalSensorBlockEntity)this.getBlockEntity(pLevel, pPos);
      return be == null ? 0 : be.getPower(side.getOpposite());
   }

   @Override
   public boolean commonCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return false;
   }

   @Override
   public boolean commonConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
      return direction != null;
   }

   public Class<GimbalSensorBlockEntity> getBlockEntityClass() {
      return GimbalSensorBlockEntity.class;
   }

   public BlockEntityType<? extends GimbalSensorBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends GimbalSensorBlockEntity>)SimBlockEntityTypes.GIMBAL_SENSOR.get();
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      Axis axis = (Axis)state.getValue(HORIZONTAL_AXIS);
      return (BlockState)state.setValue(HORIZONTAL_AXIS, rot.rotate(Direction.get(AxisDirection.POSITIVE, axis)).getAxis());
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext ctx) {
      return SimBlockShapes.GIMBAL_SENSOR;
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state;
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      InteractionResult result = super.onWrenched(state, context);
      if (result == InteractionResult.SUCCESS && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof GimbalSensorBlockEntity be) {
         be.randomNudge();
      }

      return result;
   }
}
