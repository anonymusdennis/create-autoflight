package com.simibubi.create.content.fluids.pipes;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchableWithBracket;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.Arrays;
import java.util.Optional;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidPipeBlock
   extends PipeBlock
   implements SimpleWaterloggedBlock,
   IWrenchableWithBracket,
   IBE<FluidPipeBlockEntity>,
   EncasableBlock,
   TransformableBlock {
   private static final VoxelShape OCCLUSION_BOX = Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0);
   public static final MapCodec<FluidPipeBlock> CODEC = simpleCodec(FluidPipeBlock::new);

   public FluidPipeBlock(Properties properties) {
      super(0.25F, properties);
      this.registerDefaultState((BlockState)super.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      if (this.tryRemoveBracket(context)) {
         return InteractionResult.SUCCESS;
      } else {
         Level world = context.getLevel();
         BlockPos pos = context.getClickedPos();
         Direction clickedFace = context.getClickedFace();
         Axis axis = this.getAxis(world, pos, state);
         if (axis == null) {
            Vec3 clickLocation = context.getClickLocation().subtract((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
            double closest = Float.MAX_VALUE;
            Direction argClosest = Direction.UP;

            for (Direction direction : Iterate.directions) {
               if (clickedFace.getAxis() != direction.getAxis()) {
                  Vec3 centerOf = Vec3.atCenterOf(direction.getNormal());
                  double distance = centerOf.distanceToSqr(clickLocation);
                  if (distance < closest) {
                     closest = distance;
                     argClosest = direction;
                  }
               }
            }

            axis = argClosest.getAxis();
         }

         if (clickedFace.getAxis() == axis) {
            return InteractionResult.PASS;
         } else {
            if (!world.isClientSide) {
               this.withBlockEntityDo(
                  world,
                  pos,
                  fpte -> fpte.getBehaviour(FluidTransportBehaviour.TYPE)
                        .interfaces
                        .values()
                        .stream()
                        .filter(pc -> pc != null && pc.hasFlow())
                        .findAny()
                        .ifPresent($ -> AllAdvancements.GLASS_PIPE.awardTo(context.getPlayer()))
               );
               FluidTransportBehaviour.cacheFlows(world, pos);
               world.setBlockAndUpdate(
                  pos,
                  (BlockState)((BlockState)AllBlocks.GLASS_FLUID_PIPE.getDefaultState().setValue(GlassFluidPipeBlock.AXIS, axis))
                     .setValue(BlockStateProperties.WATERLOGGED, (Boolean)state.getValue(BlockStateProperties.WATERLOGGED))
               );
               FluidTransportBehaviour.loadFlows(world, pos);
            }

            return InteractionResult.SUCCESS;
         }
      }
   }

   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
      AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      ItemInteractionResult result = this.tryEncase(state, level, pos, stack, player, hand, hitResult);
      return result.consumesAction() ? result : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   public BlockState getAxisState(Axis axis) {
      BlockState defaultState = this.defaultBlockState();

      for (Direction d : Iterate.directions) {
         defaultState = (BlockState)defaultState.setValue((Property)PROPERTY_BY_DIRECTION.get(d), d.getAxis() == axis);
      }

      return defaultState;
   }

   @Nullable
   private Axis getAxis(BlockGetter world, BlockPos pos, BlockState state) {
      return FluidPropagator.getStraightPipeAxis(state);
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      boolean blockTypeChanged = state.getBlock() != newState.getBlock();
      if (blockTypeChanged && !world.isClientSide) {
         FluidPropagator.propagateChangedPipe(world, pos, state);
      }

      if (state != newState && !isMoving) {
         this.removeBracket(world, pos, true).ifPresent(stack -> Block.popResource(world, pos, stack));
      }

      if (state.hasBlockEntity() && (blockTypeChanged || !newState.hasBlockEntity())) {
         world.removeBlockEntity(pos);
      }
   }

   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
      if (!world.isClientSide) {
         if (state != oldState) {
            world.scheduleTick(pos, this, 1, TickPriority.HIGH);
         }
      }
   }

   public void neighborChanged(BlockState state, Level world, BlockPos pos, Block otherBlock, BlockPos neighborPos, boolean isMoving) {
      DebugPackets.sendNeighborsUpdatePacket(world, pos);
      Direction d = FluidPropagator.validateNeighbourChange(state, world, pos, otherBlock, neighborPos, isMoving);
      if (d != null) {
         if (isOpenAt(state, d)) {
            world.scheduleTick(pos, this, 1, TickPriority.HIGH);
         }
      }
   }

   public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource r) {
      FluidPropagator.propagateChangedPipe(world, pos, state);
   }

   public static boolean isPipe(BlockState state) {
      return state.getBlock() instanceof FluidPipeBlock;
   }

   public static boolean canConnectTo(BlockAndTintGetter world, BlockPos neighbourPos, BlockState neighbour, Direction direction) {
      if (FluidPropagator.hasFluidCapability(world, neighbourPos, direction.getOpposite())) {
         return true;
      } else if (VanillaFluidTargets.canProvideFluidWithoutCapability(neighbour)) {
         return true;
      } else {
         FluidTransportBehaviour transport = BlockEntityBehaviour.get(world, neighbourPos, FluidTransportBehaviour.TYPE);
         BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(world, neighbourPos, BracketedBlockEntityBehaviour.TYPE);
         if (!isPipe(neighbour)) {
            return transport == null ? false : transport.canHaveFlowToward(neighbour, direction.getOpposite());
         } else {
            return bracket == null || !bracket.isBracketPresent() || FluidPropagator.getStraightPipeAxis(neighbour) == direction.getAxis();
         }
      }
   }

   public static boolean shouldDrawRim(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
      BlockPos offsetPos = pos.relative(direction);
      BlockState facingState = world.getBlockState(offsetPos);
      if (facingState.getBlock() instanceof EncasedPipeBlock) {
         return true;
      } else {
         return !isPipe(facingState) ? true : !canConnectTo(world, offsetPos, facingState, direction);
      }
   }

   public static boolean isOpenAt(BlockState state, Direction direction) {
      return (Boolean)state.getValue((Property)PROPERTY_BY_DIRECTION.get(direction));
   }

   public static boolean isCornerOrEndPipe(BlockAndTintGetter world, BlockPos pos, BlockState state) {
      return isPipe(state) && FluidPropagator.getStraightPipeAxis(state) == null && !shouldDrawCasing(world, pos, state);
   }

   public static boolean shouldDrawCasing(BlockAndTintGetter world, BlockPos pos, BlockState state) {
      if (!isPipe(state)) {
         return false;
      } else {
         for (Axis axis : Iterate.axes) {
            int connections = 0;

            for (Direction direction : Iterate.directions) {
               if (direction.getAxis() != axis && isOpenAt(state, direction)) {
                  connections++;
               }
            }

            if (connections > 2) {
               return true;
            }
         }

         return false;
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{NORTH, EAST, SOUTH, WEST, UP, DOWN, BlockStateProperties.WATERLOGGED});
      super.createBlockStateDefinition(builder);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      FluidState FluidState = context.getLevel().getFluidState(context.getClickedPos());
      return (BlockState)this.updateBlockState(
            this.defaultBlockState(), context.getNearestLookingDirection(), null, context.getLevel(), context.getClickedPos()
         )
         .setValue(BlockStateProperties.WATERLOGGED, FluidState.getType() == Fluids.WATER);
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor world, BlockPos pos, BlockPos neighbourPos) {
      if ((Boolean)state.getValue(BlockStateProperties.WATERLOGGED)) {
         world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
      }

      if (isOpenAt(state, direction) && neighbourState.hasProperty(BlockStateProperties.WATERLOGGED)) {
         world.scheduleTick(pos, this, 1, TickPriority.HIGH);
      }

      return this.updateBlockState(state, direction, direction.getOpposite(), world, pos);
   }

   public BlockState updateBlockState(BlockState state, Direction preferredDirection, @Nullable Direction ignore, BlockAndTintGetter world, BlockPos pos) {
      BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);
      if (bracket != null && bracket.isBracketPresent()) {
         return state;
      } else {
         BlockState prevState = state;
         int prevStateSides = (int)Arrays.stream(Iterate.directions).map(PROPERTY_BY_DIRECTION::get).filter(state::getValue).count();

         for (Direction d : Iterate.directions) {
            if (d != ignore) {
               boolean shouldConnect = canConnectTo(world, pos.relative(d), world.getBlockState(pos.relative(d)), d);
               state = (BlockState)state.setValue((Property)PROPERTY_BY_DIRECTION.get(d), shouldConnect);
            }
         }

         Direction connectedDirection = null;

         for (Direction dx : Iterate.directions) {
            if (isOpenAt(state, dx)) {
               if (connectedDirection != null) {
                  return state;
               }

               connectedDirection = dx;
            }
         }

         if (connectedDirection != null) {
            return (BlockState)state.setValue((Property)PROPERTY_BY_DIRECTION.get(connectedDirection.getOpposite()), true);
         } else {
            return prevStateSides == 2
               ? prevState
               : (BlockState)((BlockState)state.setValue((Property)PROPERTY_BY_DIRECTION.get(preferredDirection), true))
                  .setValue((Property)PROPERTY_BY_DIRECTION.get(preferredDirection.getOpposite()), true);
         }
      }
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
   }

   @Override
   public Optional<ItemStack> removeBracket(BlockGetter world, BlockPos pos, boolean inOnReplacedContext) {
      BracketedBlockEntityBehaviour behaviour = BracketedBlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);
      if (behaviour == null) {
         return Optional.empty();
      } else {
         BlockState bracket = behaviour.removeBracket(inOnReplacedContext);
         return bracket == null ? Optional.empty() : Optional.of(new ItemStack(bracket.getBlock()));
      }
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @Override
   public Class<FluidPipeBlockEntity> getBlockEntityClass() {
      return FluidPipeBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends FluidPipeBlockEntity>)AllBlockEntityTypes.FLUID_PIPE.get();
   }

   public boolean supportsExternalFaceHiding(BlockState state) {
      return false;
   }

   public VoxelShape getOcclusionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return OCCLUSION_BOX;
   }

   public BlockState rotate(BlockState pState, Rotation pRotation) {
      return FluidPipeBlockRotation.rotate(pState, pRotation);
   }

   public BlockState mirror(BlockState pState, Mirror pMirror) {
      return FluidPipeBlockRotation.mirror(pState, pMirror);
   }

   @Override
   public BlockState transform(BlockState state, StructureTransform transform) {
      return FluidPipeBlockRotation.transform(state, transform);
   }

   @NotNull
   protected MapCodec<? extends PipeBlock> codec() {
      return CODEC;
   }
}
