package com.simibubi.create.content.logistics.chute;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import java.util.HashMap;
import java.util.Map;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;

public class ChuteBlock extends AbstractChuteBlock implements ProperWaterloggedBlock {
   public static final Property<ChuteBlock.Shape> SHAPE = EnumProperty.create("shape", ChuteBlock.Shape.class);
   public static final DirectionProperty FACING = BlockStateProperties.FACING_HOPPER;

   public ChuteBlock(Properties p_i48440_1_) {
      super(p_i48440_1_);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(SHAPE, ChuteBlock.Shape.NORMAL)).setValue(FACING, Direction.DOWN))
            .setValue(WATERLOGGED, false)
      );
   }

   @Override
   public Direction getFacing(BlockState state) {
      return (Direction)state.getValue(FACING);
   }

   @Override
   public boolean isOpen(BlockState state) {
      return state.getValue(FACING) == Direction.DOWN || state.getValue(SHAPE) == ChuteBlock.Shape.INTERSECTION;
   }

   @Override
   public boolean isTransparent(BlockState state) {
      return state.getValue(SHAPE) == ChuteBlock.Shape.WINDOW;
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      ChuteBlock.Shape shape = (ChuteBlock.Shape)state.getValue(SHAPE);
      boolean down = state.getValue(FACING) == Direction.DOWN;
      if (shape == ChuteBlock.Shape.INTERSECTION) {
         return InteractionResult.PASS;
      } else {
         Level level = context.getLevel();
         if (level.isClientSide) {
            return InteractionResult.SUCCESS;
         } else if (shape == ChuteBlock.Shape.ENCASED) {
            level.setBlockAndUpdate(context.getClickedPos(), (BlockState)state.setValue(SHAPE, ChuteBlock.Shape.NORMAL));
            level.levelEvent(2001, context.getClickedPos(), Block.getId(AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState()));
            return InteractionResult.SUCCESS;
         } else {
            if (down) {
               level.setBlockAndUpdate(
                  context.getClickedPos(),
                  (BlockState)state.setValue(SHAPE, shape != ChuteBlock.Shape.NORMAL ? ChuteBlock.Shape.NORMAL : ChuteBlock.Shape.WINDOW)
               );
            }

            return InteractionResult.SUCCESS;
         }
      }
   }

   @Override
   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      ChuteBlock.Shape shape = (ChuteBlock.Shape)state.getValue(SHAPE);
      if (!AllBlocks.INDUSTRIAL_IRON_BLOCK.isIn(stack)) {
         return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
      } else if (shape == ChuteBlock.Shape.INTERSECTION || shape == ChuteBlock.Shape.ENCASED) {
         return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
      } else if (player != null && !level.isClientSide) {
         level.setBlockAndUpdate(pos, (BlockState)state.setValue(SHAPE, ChuteBlock.Shape.ENCASED));
         level.playSound(null, pos, SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.BLOCKS, 0.5F, 1.05F);
         return ItemInteractionResult.SUCCESS;
      } else {
         return ItemInteractionResult.SUCCESS;
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      BlockState state = this.withWater(super.getStateForPlacement(ctx), ctx);
      Direction face = ctx.getClickedFace();
      if (face.getAxis().isHorizontal() && !ctx.isSecondaryUseActive()) {
         Level world = ctx.getLevel();
         BlockPos pos = ctx.getClickedPos();
         return this.updateChuteState((BlockState)state.setValue(FACING, face), world.getBlockState(pos.above()), world, pos);
      } else {
         return state;
      }
   }

   @Override
   public BlockState updateShape(BlockState state, Direction direction, BlockState above, LevelAccessor world, BlockPos pos, BlockPos p_196271_6_) {
      this.updateWater(world, state, pos);
      return super.updateShape(state, direction, above, world, pos, p_196271_6_);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> p_206840_1_) {
      super.createBlockStateDefinition(p_206840_1_.add(new Property[]{SHAPE, FACING, WATERLOGGED}));
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      BlockState above = world.getBlockState(pos.above());
      return !isChute(above) || getChuteFacing(above) == Direction.DOWN;
   }

   @Override
   public BlockState updateChuteState(BlockState state, BlockState above, BlockGetter world, BlockPos pos) {
      if (!(state.getBlock() instanceof ChuteBlock)) {
         return state;
      } else {
         Map<Direction, Boolean> connections = new HashMap<>();
         int amtConnections = 0;
         Direction facing = (Direction)state.getValue(FACING);
         boolean vertical = facing == Direction.DOWN;
         if (!vertical) {
            BlockState target = world.getBlockState(pos.below().relative(facing.getOpposite()));
            if (!isChute(target)) {
               return (BlockState)((BlockState)state.setValue(FACING, Direction.DOWN)).setValue(SHAPE, ChuteBlock.Shape.NORMAL);
            }
         }

         for (Direction direction : Iterate.horizontalDirections) {
            BlockState diagonalInputChute = world.getBlockState(pos.above().relative(direction));
            boolean value = diagonalInputChute.getBlock() instanceof ChuteBlock && diagonalInputChute.getValue(FACING) == direction;
            connections.put(direction, value);
            if (value) {
               amtConnections++;
            }
         }

         boolean noConnections = amtConnections == 0;
         if (vertical) {
            return (BlockState)state.setValue(
               SHAPE,
               noConnections
                  ? (state.getValue(SHAPE) == ChuteBlock.Shape.INTERSECTION ? ChuteBlock.Shape.NORMAL : (ChuteBlock.Shape)state.getValue(SHAPE))
                  : ChuteBlock.Shape.INTERSECTION
            );
         } else if (noConnections) {
            return (BlockState)state.setValue(SHAPE, ChuteBlock.Shape.INTERSECTION);
         } else if (connections.get(Direction.NORTH) && connections.get(Direction.SOUTH)) {
            return (BlockState)state.setValue(SHAPE, ChuteBlock.Shape.INTERSECTION);
         } else if (connections.get(Direction.EAST) && connections.get(Direction.WEST)) {
            return (BlockState)state.setValue(SHAPE, ChuteBlock.Shape.INTERSECTION);
         } else {
            return amtConnections == 1
                  && connections.get(facing)
                  && getChuteFacing(above) != Direction.DOWN
                  && (!(above.getBlock() instanceof FunnelBlock) || FunnelBlock.getFunnelFacing(above) != Direction.DOWN)
               ? (BlockState)state.setValue(SHAPE, state.getValue(SHAPE) == ChuteBlock.Shape.ENCASED ? ChuteBlock.Shape.ENCASED : ChuteBlock.Shape.NORMAL)
               : (BlockState)state.setValue(SHAPE, ChuteBlock.Shape.INTERSECTION);
         }
      }
   }

   public BlockState rotate(BlockState pState, Rotation pRot) {
      return (BlockState)pState.setValue(FACING, pRot.rotate((Direction)pState.getValue(FACING)));
   }

   public BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState.rotate(pMirror.getRotation((Direction)pState.getValue(FACING)));
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @Override
   public BlockEntityType<? extends ChuteBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends ChuteBlockEntity>)AllBlockEntityTypes.CHUTE.get();
   }

   public static enum Shape implements StringRepresentable {
      INTERSECTION,
      WINDOW,
      NORMAL,
      ENCASED;

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}
