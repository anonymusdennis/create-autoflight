package com.simibubi.create.content.redstone.rail;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ControllerRailBlock extends BaseRailBlock implements IWrenchable {
   public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
   public static final BooleanProperty BACKWARDS = BooleanProperty.create("backwards");
   public static final IntegerProperty POWER = BlockStateProperties.POWER;
   public static final MapCodec<ControllerRailBlock> CODEC = simpleCodec(ControllerRailBlock::new);

   public ControllerRailBlock(Properties properties) {
      super(true, properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(POWER, 0)).setValue(BACKWARDS, false))
               .setValue(SHAPE, RailShape.NORTH_SOUTH))
            .setValue(WATERLOGGED, false)
      );
   }

   public static Vec3i getAccelerationVector(BlockState state) {
      Direction pointingTo = getPointingTowards(state);
      return (isStateBackwards(state) ? pointingTo.getOpposite() : pointingTo).getNormal();
   }

   private static Direction getPointingTowards(BlockState state) {
      switch ((RailShape)state.getValue(SHAPE)) {
         case ASCENDING_WEST:
         case EAST_WEST:
            return Direction.WEST;
         case ASCENDING_EAST:
            return Direction.EAST;
         case ASCENDING_SOUTH:
            return Direction.SOUTH;
         default:
            return Direction.NORTH;
      }
   }

   protected BlockState updateDir(Level world, BlockPos pos, BlockState state, boolean p_208489_4_) {
      BlockState updatedState = super.updateDir(world, pos, state, p_208489_4_);
      if (updatedState.getValue(SHAPE) == state.getValue(SHAPE)) {
         return updatedState;
      } else {
         BlockState reversedUpdatedState = updatedState;
         if (getPointingTowards(state).getAxis() != getPointingTowards(updatedState).getAxis()) {
            for (boolean opposite : Iterate.trueAndFalse) {
               Direction offset = getPointingTowards(updatedState);
               if (opposite) {
                  offset = offset.getOpposite();
               }

               for (BlockPos adjPos : Iterate.hereBelowAndAbove(pos.relative(offset))) {
                  BlockState adjState = world.getBlockState(adjPos);
                  if (AllBlocks.CONTROLLER_RAIL.has(adjState)
                     && getPointingTowards(adjState).getAxis() == offset.getAxis()
                     && adjState.getValue(BACKWARDS) != reversedUpdatedState.getValue(BACKWARDS)) {
                     reversedUpdatedState = (BlockState)reversedUpdatedState.cycle(BACKWARDS);
                  }
               }
            }
         }

         if (reversedUpdatedState != updatedState) {
            world.setBlockAndUpdate(pos, reversedUpdatedState);
         }

         return reversedUpdatedState;
      }
   }

   private static void decelerateCart(BlockPos pos, AbstractMinecart cart) {
      Vec3 diff = VecHelper.getCenterOf(pos).subtract(cart.position());
      cart.setDeltaMovement(diff.x / 16.0, 0.0, diff.z / 16.0);
      if (cart instanceof MinecartFurnace fme) {
         fme.xPush = fme.zPush = 0.0;
      }
   }

   private static boolean isStableWith(BlockState testState, BlockGetter world, BlockPos pos) {
      return canSupportRigidBlock(world, pos.below())
         && (!((RailShape)testState.getValue(SHAPE)).isAscending() || canSupportRigidBlock(world, pos.relative(getPointingTowards(testState))));
   }

   public BlockState getStateForPlacement(BlockPlaceContext p_196258_1_) {
      Direction direction = p_196258_1_.getHorizontalDirection();
      BlockState base = super.getStateForPlacement(p_196258_1_);
      return (BlockState)(base == null ? this.defaultBlockState() : base).setValue(BACKWARDS, direction.getAxisDirection() == AxisDirection.POSITIVE);
   }

   public Property<RailShape> getShapeProperty() {
      return SHAPE;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> p_206840_1_) {
      p_206840_1_.add(new Property[]{SHAPE, POWER, BACKWARDS, WATERLOGGED});
   }

   public void onMinecartPass(BlockState state, Level world, BlockPos pos, AbstractMinecart cart) {
      if (!world.isClientSide) {
         Vec3 accelerationVec = Vec3.atLowerCornerOf(getAccelerationVector(state));
         double targetSpeed = cart.getMaxSpeedWithRail() * (double)((Integer)state.getValue(POWER)).intValue() / 15.0;
         if (cart instanceof MinecartFurnace fme) {
            fme.xPush = accelerationVec.x;
            fme.zPush = accelerationVec.z;
         }

         Vec3 motion = cart.getDeltaMovement();
         if ((motion.dot(accelerationVec) >= 0.0 || motion.lengthSqr() < 1.0E-4) && targetSpeed > 0.0) {
            cart.setDeltaMovement(accelerationVec.scale(targetSpeed));
         } else {
            decelerateCart(pos, cart);
         }
      }
   }

   protected void updateState(BlockState state, Level world, BlockPos pos, Block block) {
      int newPower = this.calculatePower(world, pos);
      if ((Integer)state.getValue(POWER) != newPower) {
         this.placeAndNotify((BlockState)state.setValue(POWER, newPower), pos, world);
      }
   }

   private int calculatePower(Level world, BlockPos pos) {
      int newPower = world.getBestNeighborSignal(pos);
      if (newPower != 0) {
         return newPower;
      } else {
         int forwardDistance = 0;
         int backwardsDistance = 0;
         BlockPos lastForwardRail = pos;
         BlockPos lastBackwardsRail = pos;
         int forwardPower = 0;
         int backwardsPower = 0;

         for (int i = 0; i < 15; i++) {
            BlockPos testPos = this.findNextRail(lastForwardRail, world, false);
            if (testPos == null) {
               break;
            }

            forwardDistance++;
            lastForwardRail = testPos;
            forwardPower = world.getBestNeighborSignal(testPos);
            if (forwardPower != 0) {
               break;
            }
         }

         for (int i = 0; i < 15; i++) {
            BlockPos testPosx = this.findNextRail(lastBackwardsRail, world, true);
            if (testPosx == null) {
               break;
            }

            backwardsDistance++;
            lastBackwardsRail = testPosx;
            backwardsPower = world.getBestNeighborSignal(testPosx);
            if (backwardsPower != 0) {
               break;
            }
         }

         if (forwardDistance > 8 && backwardsDistance > 8) {
            return 0;
         } else if (backwardsPower == 0 && forwardDistance <= 8) {
            return forwardPower;
         } else if (forwardPower == 0 && backwardsDistance <= 8) {
            return backwardsPower;
         } else {
            return backwardsPower != 0 && forwardPower != 0
               ? Mth.ceil((double)(backwardsPower * forwardDistance + forwardPower * backwardsDistance) / (double)(forwardDistance + backwardsDistance))
               : 0;
         }
      }
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      Level world = context.getLevel();
      if (world.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         BlockPos pos = context.getClickedPos();

         for (Rotation testRotation : new Rotation[]{Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90}) {
            BlockState testState = this.rotate(state, testRotation);
            if (isStableWith(testState, world, pos)) {
               this.placeAndNotify(testState, pos, world);
               return InteractionResult.SUCCESS;
            }
         }

         BlockState testState = (BlockState)state.setValue(BACKWARDS, !(Boolean)state.getValue(BACKWARDS));
         if (isStableWith(testState, world, pos)) {
            this.placeAndNotify(testState, pos, world);
         }

         return InteractionResult.SUCCESS;
      }
   }

   private void placeAndNotify(BlockState state, BlockPos pos, Level world) {
      world.setBlock(pos, state, 3);
      world.updateNeighborsAt(pos.below(), this);
      if (((RailShape)state.getValue(SHAPE)).isAscending()) {
         world.updateNeighborsAt(pos.above(), this);
      }
   }

   @Nullable
   private BlockPos findNextRail(BlockPos from, BlockGetter world, boolean reversed) {
      BlockState current = world.getBlockState(from);
      if (!(current.getBlock() instanceof ControllerRailBlock)) {
         return null;
      } else {
         Vec3i accelerationVec = getAccelerationVector(current);
         BlockPos baseTestPos = reversed ? from.subtract(accelerationVec) : from.offset(accelerationVec);

         for (BlockPos testPos : Iterate.hereBelowAndAbove(baseTestPos)) {
            if (testPos.getY() <= from.getY() || ((RailShape)current.getValue(SHAPE)).isAscending()) {
               BlockState testState = world.getBlockState(testPos);
               if (testState.getBlock() instanceof ControllerRailBlock && getAccelerationVector(testState).equals(accelerationVec)) {
                  return testPos;
               }
            }
         }

         return null;
      }
   }

   public boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
      return (Integer)state.getValue(POWER);
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      if (rotation == Rotation.NONE) {
         return state;
      } else {
         RailShape railshape = (RailShape)((BlockState)Blocks.POWERED_RAIL.defaultBlockState().setValue(SHAPE, (RailShape)state.getValue(SHAPE)))
            .rotate(rotation)
            .getValue(SHAPE);
         state = (BlockState)state.setValue(SHAPE, railshape);
         return rotation != Rotation.CLOCKWISE_180 && getPointingTowards(state).getAxis() == Axis.Z != (rotation == Rotation.COUNTERCLOCKWISE_90)
            ? state
            : (BlockState)state.cycle(BACKWARDS);
      }
   }

   public BlockState mirror(BlockState state, Mirror mirror) {
      if (mirror == Mirror.NONE) {
         return state;
      } else {
         RailShape railshape = (RailShape)((BlockState)Blocks.POWERED_RAIL.defaultBlockState().setValue(SHAPE, (RailShape)state.getValue(SHAPE)))
            .mirror(mirror)
            .getValue(SHAPE);
         state = (BlockState)state.setValue(SHAPE, railshape);
         return getPointingTowards(state).getAxis() == Axis.Z == (mirror == Mirror.LEFT_RIGHT) ? (BlockState)state.cycle(BACKWARDS) : state;
      }
   }

   public static boolean isStateBackwards(BlockState state) {
      return (Boolean)state.getValue(BACKWARDS) ^ isReversedSlope(state);
   }

   public static boolean isReversedSlope(BlockState state) {
      return state.getValue(SHAPE) == RailShape.ASCENDING_SOUTH || state.getValue(SHAPE) == RailShape.ASCENDING_EAST;
   }

   protected MapCodec<? extends BaseRailBlock> codec() {
      return CODEC;
   }
}
