package com.simibubi.create.content.logistics.tunnel;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BeltTunnelBlock extends Block implements IBE<BeltTunnelBlockEntity>, IWrenchable {
   public static final Property<BeltTunnelBlock.Shape> SHAPE = EnumProperty.create("shape", BeltTunnelBlock.Shape.class);
   public static final Property<Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;

   public BeltTunnelBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(SHAPE, BeltTunnelBlock.Shape.STRAIGHT));
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return BeltTunnelShapes.getShape(state);
   }

   public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
      BlockState blockState = worldIn.getBlockState(pos.below());
      return !this.isValidPositionForPlacement(state, worldIn, pos) ? false : (Boolean)blockState.getValue(BeltBlock.CASING);
   }

   public boolean isValidPositionForPlacement(BlockState state, LevelReader worldIn, BlockPos pos) {
      BlockState blockState = worldIn.getBlockState(pos.below());
      return !AllBlocks.BELT.has(blockState) ? false : blockState.getValue(BeltBlock.SLOPE) == BeltSlope.HORIZONTAL;
   }

   public static boolean hasWindow(BlockState state) {
      return state.getValue(SHAPE) == BeltTunnelBlock.Shape.WINDOW || state.getValue(SHAPE) == BeltTunnelBlock.Shape.CLOSED;
   }

   public static boolean isStraight(BlockState state) {
      return hasWindow(state) || state.getValue(SHAPE) == BeltTunnelBlock.Shape.STRAIGHT;
   }

   public static boolean isJunction(BlockState state) {
      BeltTunnelBlock.Shape shape = (BeltTunnelBlock.Shape)state.getValue(SHAPE);
      return shape == BeltTunnelBlock.Shape.CROSS || shape == BeltTunnelBlock.Shape.T_LEFT || shape == BeltTunnelBlock.Shape.T_RIGHT;
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return this.getTunnelState(context.getLevel(), context.getClickedPos());
   }

   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState p_220082_4_, boolean p_220082_5_) {
      if (!(world instanceof WrappedLevel) && !world.isClientSide()) {
         this.withBlockEntityDo(world, pos, BeltTunnelBlockEntity::updateTunnelConnections);
      }
   }

   public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
      if (facing.getAxis().isVertical()) {
         return state;
      } else {
         if (!(worldIn instanceof WrappedLevel) && !worldIn.isClientSide()) {
            this.withBlockEntityDo(worldIn, currentPos, BeltTunnelBlockEntity::updateTunnelConnections);
         }

         BlockState tunnelState = this.getTunnelState(worldIn, currentPos);
         return tunnelState.getValue(HORIZONTAL_AXIS) == state.getValue(HORIZONTAL_AXIS) && hasWindow(tunnelState) == hasWindow(state) ? state : tunnelState;
      }
   }

   public void updateTunnel(LevelAccessor world, BlockPos pos) {
      BlockState tunnel = world.getBlockState(pos);
      BlockState newTunnel = this.getTunnelState(world, pos);
      if (tunnel != newTunnel && !world.isClientSide()) {
         world.setBlock(pos, newTunnel, 3);
         BlockEntity be = world.getBlockEntity(pos);
         if (be != null && be instanceof BeltTunnelBlockEntity) {
            ((BeltTunnelBlockEntity)be).updateTunnelConnections();
         }
      }
   }

   private BlockState getTunnelState(BlockGetter reader, BlockPos pos) {
      BlockState state = this.defaultBlockState();
      BlockState belt = reader.getBlockState(pos.below());
      if (AllBlocks.BELT.has(belt)) {
         state = (BlockState)state.setValue(HORIZONTAL_AXIS, ((Direction)belt.getValue(BeltBlock.HORIZONTAL_FACING)).getAxis());
      }

      Axis axis = (Axis)state.getValue(HORIZONTAL_AXIS);
      Direction left = Direction.get(AxisDirection.POSITIVE, axis).getClockWise();
      boolean onLeft = this.hasValidOutput(reader, pos.below(), left);
      boolean onRight = this.hasValidOutput(reader, pos.below(), left.getOpposite());
      if (onLeft && onRight) {
         state = (BlockState)state.setValue(SHAPE, BeltTunnelBlock.Shape.CROSS);
      } else if (onLeft) {
         state = (BlockState)state.setValue(SHAPE, BeltTunnelBlock.Shape.T_LEFT);
      } else if (onRight) {
         state = (BlockState)state.setValue(SHAPE, BeltTunnelBlock.Shape.T_RIGHT);
      }

      if (state.getValue(SHAPE) == BeltTunnelBlock.Shape.STRAIGHT) {
         boolean canHaveWindow = this.canHaveWindow(reader, pos, axis);
         if (canHaveWindow) {
            state = (BlockState)state.setValue(SHAPE, BeltTunnelBlock.Shape.WINDOW);
         }
      }

      return state;
   }

   protected boolean canHaveWindow(BlockGetter reader, BlockPos pos, Axis axis) {
      Direction fw = Direction.get(AxisDirection.POSITIVE, axis);
      BlockState blockState1 = reader.getBlockState(pos.relative(fw));
      BlockState blockState2 = reader.getBlockState(pos.relative(fw.getOpposite()));
      boolean funnel1 = blockState1.getBlock() instanceof BeltFunnelBlock
         && blockState1.getValue(BeltFunnelBlock.SHAPE) == BeltFunnelBlock.Shape.EXTENDED
         && blockState1.getValue(BeltFunnelBlock.HORIZONTAL_FACING) == fw.getOpposite();
      boolean funnel2 = blockState2.getBlock() instanceof BeltFunnelBlock
         && blockState2.getValue(BeltFunnelBlock.SHAPE) == BeltFunnelBlock.Shape.EXTENDED
         && blockState2.getValue(BeltFunnelBlock.HORIZONTAL_FACING) == fw;
      boolean valid1 = blockState1.getBlock() instanceof BeltTunnelBlock || funnel1;
      boolean valid2 = blockState2.getBlock() instanceof BeltTunnelBlock || funnel2;
      return valid1 && valid2 && (!funnel1 || !funnel2);
   }

   private boolean hasValidOutput(BlockGetter world, BlockPos pos, Direction side) {
      BlockState blockState = world.getBlockState(pos.relative(side));
      if (AllBlocks.BELT.has(blockState)) {
         return ((Direction)blockState.getValue(BeltBlock.HORIZONTAL_FACING)).getAxis() == side.getAxis();
      } else {
         DirectBeltInputBehaviour behaviour = BlockEntityBehaviour.get(world, pos.relative(side), DirectBeltInputBehaviour.TYPE);
         return behaviour != null && behaviour.canInsertFromSide(side);
      }
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      if (!hasWindow(state)) {
         return InteractionResult.PASS;
      } else {
         BeltTunnelBlock.Shape shape = (BeltTunnelBlock.Shape)state.getValue(SHAPE);
         shape = shape == BeltTunnelBlock.Shape.CLOSED ? BeltTunnelBlock.Shape.WINDOW : BeltTunnelBlock.Shape.CLOSED;
         Level world = context.getLevel();
         if (!world.isClientSide) {
            world.setBlock(context.getClickedPos(), (BlockState)state.setValue(SHAPE, shape), 2);
         }

         return InteractionResult.SUCCESS;
      }
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      Direction fromAxis = Direction.get(AxisDirection.POSITIVE, (Axis)state.getValue(HORIZONTAL_AXIS));
      Direction rotated = rotation.rotate(fromAxis);
      return (BlockState)state.setValue(HORIZONTAL_AXIS, rotated.getAxis());
   }

   public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      if (!worldIn.isClientSide) {
         if (fromPos.equals(pos.below()) && !this.canSurvive(state, worldIn, pos)) {
            worldIn.destroyBlock(pos, true);
         }
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{HORIZONTAL_AXIS, SHAPE});
      super.createBlockStateDefinition(builder);
   }

   @Override
   public Class<BeltTunnelBlockEntity> getBlockEntityClass() {
      return BeltTunnelBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends BeltTunnelBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends BeltTunnelBlockEntity>)AllBlockEntityTypes.ANDESITE_TUNNEL.get();
   }

   public static enum Shape implements StringRepresentable {
      STRAIGHT,
      WINDOW,
      CLOSED,
      T_LEFT,
      T_RIGHT,
      CROSS;

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}
