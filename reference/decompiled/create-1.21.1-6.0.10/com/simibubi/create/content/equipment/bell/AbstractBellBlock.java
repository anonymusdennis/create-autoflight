package com.simibubi.create.content.equipment.bell;

import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractBellBlock<BE extends AbstractBellBlockEntity> extends BellBlock implements IBE<BE> {
   public AbstractBellBlock(Properties properties) {
      super(properties);
   }

   public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext selection) {
      Direction facing = (Direction)state.getValue(FACING);

      return switch ((BellAttachType)state.getValue(ATTACHMENT)) {
         case CEILING -> AllShapes.BELL_CEILING.get(facing);
         case DOUBLE_WALL -> AllShapes.BELL_DOUBLE_WALL.get(facing);
         case FLOOR -> AllShapes.BELL_FLOOR.get(facing);
         case SINGLE_WALL -> AllShapes.BELL_WALL.get(facing);
         default -> throw new MatchException(null, null);
      };
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      if (!pLevel.isClientSide) {
         boolean shouldPower = pLevel.hasNeighborSignal(pPos);
         if (shouldPower != (Boolean)pState.getValue(POWERED)) {
            pLevel.setBlock(pPos, (BlockState)pState.setValue(POWERED, shouldPower), 3);
            if (shouldPower) {
               Direction facing = (Direction)pState.getValue(FACING);
               BellAttachType type = (BellAttachType)pState.getValue(ATTACHMENT);
               this.ring(pLevel, pPos, type != BellAttachType.CEILING && type != BellAttachType.FLOOR ? facing.getClockWise() : facing, null);
            }
         }
      }
   }

   public boolean onHit(Level world, BlockState state, BlockHitResult hit, @Nullable Player player, boolean flag) {
      BlockPos pos = hit.getBlockPos();
      Direction direction = hit.getDirection();
      if (direction == null) {
         direction = (Direction)world.getBlockState(pos).getValue(FACING);
      }

      return !this.canRingFrom(state, direction, hit.getLocation().y - (double)pos.getY()) ? false : this.ring(world, pos, direction, player);
   }

   protected boolean ring(Level world, BlockPos pos, Direction direction, Player player) {
      BE be = this.getBlockEntity(world, pos);
      if (world.isClientSide) {
         return true;
      } else if (be != null && be.ring(world, pos, direction)) {
         this.playSound(world, pos);
         if (player != null) {
            player.awardStat(Stats.BELL_RING);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean canRingFrom(BlockState state, Direction hitDir, double heightChange) {
      if (hitDir.getAxis() == Axis.Y) {
         return false;
      } else if (heightChange > 0.8124) {
         return false;
      } else {
         Direction direction = (Direction)state.getValue(FACING);
         BellAttachType bellAttachment = (BellAttachType)state.getValue(ATTACHMENT);
         switch (bellAttachment) {
            case CEILING:
            case FLOOR:
               return direction.getAxis() == hitDir.getAxis();
            case DOUBLE_WALL:
            case SINGLE_WALL:
               return direction.getAxis() != hitDir.getAxis();
            default:
               return false;
         }
      }
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos p_152198_, BlockState p_152199_) {
      return IBE.super.newBlockEntity(p_152198_, p_152199_);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_152194_, BlockState p_152195_, BlockEntityType<T> p_152196_) {
      return IBE.super.getTicker(p_152194_, p_152195_, p_152196_);
   }

   public abstract void playSound(Level var1, BlockPos var2);
}
