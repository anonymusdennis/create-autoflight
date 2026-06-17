package dev.simulated_team.simulated.content.blocks.lasers.optical_sensor;

import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class OpticalSensorBlock extends DirectedDirectionalBlock implements IBE<OpticalSensorBlockEntity>, CommonRedstoneBlock {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public OpticalSensorBlock(Properties properties) {
      super(properties);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{POWERED});
      super.createBlockStateDefinition(builder);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (level.getBlockEntity(pos) instanceof OpticalSensorBlockEntity be && be.tryApplyDye(stack)) {
         level.playLocalSound((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), SoundEvents.DYE_USE, SoundSource.PLAYERS, 0.3F, 1.0F, false);
         return ItemInteractionResult.SUCCESS;
      }

      return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = this.defaultBlockState();
      Direction facing = context.getNearestLookingDirection();
      Direction preferredFacing = context.getPlayer() != null && context.getPlayer().isSteppingCarefully() ? facing : facing.getOpposite();
      if (preferredFacing.getAxis() == Axis.Y) {
         state = (BlockState)state.setValue(TARGET, preferredFacing == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR);
         preferredFacing = context.getHorizontalDirection().getOpposite();
      }

      return (BlockState)((BlockState)state.setValue(FACING, preferredFacing)).setValue(POWERED, false);
   }

   @Override
   public boolean commonConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return side != ((Direction)state.getValue(FACING)).getOpposite();
   }

   public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
      if (state.hasBlockEntity() && state.getBlock() != newState.getBlock()) {
         IBE.onRemove(state, level, pos, newState);
         level.removeBlockEntity(pos);
      }
   }

   public boolean isSignalSource(BlockState state) {
      return (Boolean)state.getValue(POWERED);
   }

   @Override
   public boolean commonCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return false;
   }

   public int getSignal(@NotNull BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      OpticalSensorBlockEntity be = (OpticalSensorBlockEntity)blockAccess.getBlockEntity(pos);
      int power = 0;
      if (be != null && this.isSignalSource(blockState) && side != ((Direction)blockState.getValue(FACING)).getOpposite()) {
         power = Math.round((be.getRaycastLength() - be.getRayDistance()) * (15.0F / be.getRaycastLength()));
      }

      return power;
   }

   public Class<OpticalSensorBlockEntity> getBlockEntityClass() {
      return OpticalSensorBlockEntity.class;
   }

   public BlockEntityType<? extends OpticalSensorBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends OpticalSensorBlockEntity>)SimBlockEntityTypes.OPTICAL_SENSOR.get();
   }
}
