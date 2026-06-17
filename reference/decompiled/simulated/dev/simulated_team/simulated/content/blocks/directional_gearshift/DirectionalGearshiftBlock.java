package dev.simulated_team.simulated.content.blocks.directional_gearshift;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.Nullable;

public class DirectionalGearshiftBlock extends DirectionalAxisKineticBlock implements IBE<SplitShaftBlockEntity>, IRotate {
   public static final BooleanProperty LEFT_POWERED = BooleanProperty.create("left_powered");
   public static final BooleanProperty RIGHT_POWERED = BooleanProperty.create("right_powered");

   public DirectionalGearshiftBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(LEFT_POWERED, false));
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(RIGHT_POWERED, false));
   }

   public BlockState updateAfterWrenched(BlockState newState, UseOnContext context) {
      return super.updateAfterWrenched(this.getPoweredState(context.getLevel(), newState, context.getClickedPos()), context);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{LEFT_POWERED}).add(new Property[]{RIGHT_POWERED}));
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Direction lookingDirection = pContext.getNearestLookingDirection();
      boolean shiftKeyDown = pContext.getPlayer().isShiftKeyDown();
      Axis preferredAxis = RotatedPillarKineticBlock.getPreferredAxis(pContext);
      boolean axisAlongFirst = false;
      Direction darkDirection;
      if (preferredAxis != null && preferredAxis != lookingDirection.getAxis() && !shiftKeyDown) {
         darkDirection = lookingDirection;
         if (preferredAxis == Axis.X) {
            axisAlongFirst = true;
         } else if (preferredAxis == Axis.Y && lookingDirection.getAxis() == Axis.X) {
            axisAlongFirst = true;
         }
      } else if (lookingDirection.getAxis().isHorizontal()) {
         darkDirection = lookingDirection.getCounterClockWise();
         if (lookingDirection.getAxis() == Axis.X) {
            axisAlongFirst = true;
         }
      } else {
         darkDirection = pContext.getHorizontalDirection().getCounterClockWise();
         if (pContext.getHorizontalDirection().getAxis() == Axis.Z) {
            axisAlongFirst = true;
         }
      }

      if (shiftKeyDown) {
         darkDirection = darkDirection.getOpposite();
      }

      BlockState state = (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, darkDirection))
         .setValue(AXIS_ALONG_FIRST_COORDINATE, axisAlongFirst);
      return this.getPoweredState(pContext.getLevel(), state, pContext.getClickedPos());
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      if (!level.isClientSide) {
         boolean previouslyLeftPowered = (Boolean)state.getValue(LEFT_POWERED);
         boolean previouslyRightPowered = (Boolean)state.getValue(RIGHT_POWERED);
         BlockState newState = this.getPoweredState(level, state, pos);
         if (previouslyLeftPowered != (Boolean)newState.getValue(LEFT_POWERED) || previouslyRightPowered != (Boolean)newState.getValue(RIGHT_POWERED)) {
            this.detachKinetics(level, pos, true);
            level.setBlock(pos, newState, 2);
         }
      }
   }

   public BlockState getPoweredState(Level level, BlockState state, BlockPos pos) {
      Direction leftDirection = this.getLeftDirection(state);
      Direction rightDirection = this.getRightDirection(state);
      int leftSignal = level.getSignal(pos.offset(leftDirection.getNormal()), leftDirection);
      int rightSignal = level.getSignal(pos.offset(rightDirection.getNormal()), rightDirection);
      boolean previouslyLeftPowered = (Boolean)state.getValue(LEFT_POWERED);
      if (previouslyLeftPowered != leftSignal > 0) {
         state = (BlockState)state.cycle(LEFT_POWERED);
      }

      boolean previouslyRightPowered = (Boolean)state.getValue(RIGHT_POWERED);
      if (previouslyRightPowered != rightSignal > 0) {
         state = (BlockState)state.cycle(RIGHT_POWERED);
      }

      return state;
   }

   public void detachKinetics(Level worldIn, BlockPos pos, boolean reAttachNextTick) {
      BlockEntity be = worldIn.getBlockEntity(pos);
      if (be instanceof KineticBlockEntity) {
         RotationPropagator.handleRemoved(worldIn, pos, (KineticBlockEntity)be);
         if (reAttachNextTick) {
            worldIn.scheduleTick(pos, this, 1, TickPriority.EXTREMELY_HIGH);
         }
      }
   }

   public Direction getLeftDirection(BlockState state) {
      return (Direction)state.getValue(FACING);
   }

   public Direction getRightDirection(BlockState state) {
      return this.getLeftDirection(state).getOpposite();
   }

   public Class<SplitShaftBlockEntity> getBlockEntityClass() {
      return SplitShaftBlockEntity.class;
   }

   public BlockEntityType<? extends SplitShaftBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SplitShaftBlockEntity>)SimBlockEntityTypes.DIRECTIONAL_GEARSHIFT.get();
   }

   public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource random) {
      if (worldIn.getBlockEntity(pos) instanceof KineticBlockEntity kte) {
         RotationPropagator.handleAdded(worldIn, pos, kte);
      }
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      InteractionResult interactionResult = super.onWrenched(state, context);
      if (interactionResult.consumesAction() && !context.getLevel().isClientSide) {
         this.detachKinetics(context.getLevel(), context.getClickedPos(), true);
      }

      return interactionResult;
   }
}
