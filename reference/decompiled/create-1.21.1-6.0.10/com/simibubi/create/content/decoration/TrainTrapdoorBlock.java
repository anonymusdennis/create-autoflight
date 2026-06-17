package com.simibubi.create.content.decoration;

import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

public class TrainTrapdoorBlock extends TrapDoorBlock implements IWrenchable {
   @Deprecated(
      since = "6.0.7",
      forRemoval = true
   )
   @ScheduledForRemoval(
      inVersion = "1.21.1+ Port"
   )
   public TrainTrapdoorBlock(Properties properties) {
      super(SlidingDoorBlock.TRAIN_SET_TYPE.get(), properties);
   }

   public TrainTrapdoorBlock(BlockSetType type, Properties properties) {
      super(type, properties);
   }

   public static TrainTrapdoorBlock metal(Properties properties) {
      return new TrainTrapdoorBlock(SlidingDoorBlock.TRAIN_SET_TYPE.get(), properties);
   }

   public static TrainTrapdoorBlock glass(Properties properties) {
      return new TrainTrapdoorBlock(SlidingDoorBlock.GLASS_SET_TYPE.get(), properties);
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      state = (BlockState)state.cycle(OPEN);
      level.setBlock(pos, state, 2);
      if ((Boolean)state.getValue(WATERLOGGED)) {
         level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      this.playSound(player, level, pos, (Boolean)state.getValue(OPEN));
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public boolean skipRendering(BlockState state, BlockState other, Direction pDirection) {
      return state.is(this) == other.is(this) && isConnected(state, other, pDirection);
   }

   public static boolean isConnected(BlockState state, BlockState other, Direction pDirection) {
      state = (BlockState)((BlockState)state.setValue(WATERLOGGED, false)).setValue(POWERED, false);
      other = (BlockState)((BlockState)other.setValue(WATERLOGGED, false)).setValue(POWERED, false);
      boolean open = (Boolean)state.getValue(OPEN);
      Half half = (Half)state.getValue(HALF);
      Direction facing = (Direction)state.getValue(FACING);
      if (open != (Boolean)other.getValue(OPEN)) {
         return false;
      } else if (!open && half == other.getValue(HALF)) {
         return pDirection.getAxis() != Axis.Y;
      } else if (!open && half != other.getValue(HALF) && pDirection.getAxis() == Axis.Y) {
         return true;
      } else if (open && facing.getOpposite() == other.getValue(FACING) && pDirection.getAxis() == facing.getAxis()) {
         return true;
      } else {
         return (open ? (BlockState)state.setValue(HALF, Half.TOP) : state) != (open ? (BlockState)other.setValue(HALF, Half.TOP) : other)
            ? false
            : pDirection.getAxis() != facing.getAxis();
      }
   }
}
