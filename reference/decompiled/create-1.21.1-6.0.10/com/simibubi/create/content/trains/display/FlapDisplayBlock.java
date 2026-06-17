package com.simibubi.create.content.trains.display;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import java.util.List;
import java.util.function.Predicate;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.LevelTickAccess;

public class FlapDisplayBlock extends HorizontalKineticBlock implements IBE<FlapDisplayBlockEntity>, IWrenchable, ICogWheel, SimpleWaterloggedBlock {
   public static final BooleanProperty UP = BooleanProperty.create("up");
   public static final BooleanProperty DOWN = BooleanProperty.create("down");
   private static final int placementHelperId = PlacementHelpers.register(new FlapDisplayBlock.PlacementHelper());

   public FlapDisplayBlock(Properties p_49795_) {
      super(p_49795_);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(UP, false)).setValue(DOWN, false))
            .setValue(BlockStateProperties.WATERLOGGED, false)
      );
   }

   @Override
   protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
      return super.areStatesKineticallyEquivalent(oldState, newState);
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(HORIZONTAL_FACING)).getAxis();
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{UP, DOWN, BlockStateProperties.WATERLOGGED}));
   }

   @Override
   public IRotate.SpeedLevel getMinimumRequiredSpeedLevel() {
      return IRotate.SpeedLevel.MEDIUM;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction face = context.getClickedFace();
      BlockPos clickedPos = context.getClickedPos();
      BlockPos placedOnPos = clickedPos.relative(face.getOpposite());
      Level level = context.getLevel();
      BlockState blockState = level.getBlockState(placedOnPos);
      BlockState stateForPlacement = this.defaultBlockState();
      FluidState ifluidstate = context.getLevel().getFluidState(context.getClickedPos());
      if (blockState.getBlock() == this && (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown())) {
         Direction otherFacing = (Direction)blockState.getValue(HORIZONTAL_FACING);
         stateForPlacement = (BlockState)stateForPlacement.setValue(HORIZONTAL_FACING, otherFacing);
      } else {
         stateForPlacement = super.getStateForPlacement(context);
      }

      return this.updateColumn(
         level, clickedPos, (BlockState)stateForPlacement.setValue(BlockStateProperties.WATERLOGGED, ifluidstate.getType() == Fluids.WATER), true
      );
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (player.isShiftKeyDown()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
         if (placementHelper.matchesItem(stack)) {
            return placementHelper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem)stack.getItem(), player, hand, hitResult);
         } else {
            FlapDisplayBlockEntity flapBE = this.getBlockEntity(level, pos);
            if (flapBE == null) {
               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            } else {
               flapBE = flapBE.getController();
               if (flapBE == null) {
                  return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
               } else {
                  double yCoord = hitResult.getLocation().add(Vec3.atLowerCornerOf(hitResult.getDirection().getOpposite().getNormal()).scale(0.125)).y;
                  int lineIndex = flapBE.getLineIndexAt(yCoord);
                  if (stack.isEmpty()) {
                     if (!flapBE.isSpeedRequirementFulfilled()) {
                        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                     } else {
                        flapBE.applyTextManually(lineIndex, null);
                        return ItemInteractionResult.SUCCESS;
                     }
                  } else if (stack.getItem() == Items.GLOW_INK_SAC) {
                     if (!level.isClientSide) {
                        level.playSound(null, pos, SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        flapBE.setGlowing(lineIndex);
                     }

                     return ItemInteractionResult.SUCCESS;
                  } else {
                     boolean display = stack.getItem() == Items.NAME_TAG && stack.has(DataComponents.CUSTOM_NAME) || AllBlocks.CLIPBOARD.isIn(stack);
                     DyeColor dye = DyeColor.getColor(stack);
                     if (!display && dye == null) {
                        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                     } else if (dye == null && !flapBE.isSpeedRequirementFulfilled()) {
                        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                     } else if (level.isClientSide) {
                        return ItemInteractionResult.SUCCESS;
                     } else {
                        Component customName = (Component)stack.get(DataComponents.CUSTOM_NAME);
                        if (display) {
                           if (AllBlocks.CLIPBOARD.isIn(stack)) {
                              List<ClipboardEntry> entries = ClipboardEntry.getLastViewedEntries(stack);
                              int line = lineIndex;

                              for (ClipboardEntry entry : entries) {
                                 for (String string : entry.text.getString().split("\n")) {
                                    flapBE.applyTextManually(line++, Component.literal(string));
                                 }
                              }

                              return ItemInteractionResult.SUCCESS;
                           }

                           flapBE.applyTextManually(lineIndex, customName);
                        }

                        if (dye != null) {
                           level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                           flapBE.setColour(lineIndex, dye);
                        }

                        return ItemInteractionResult.SUCCESS;
                     }
                  }
               }
            }
         }
      }
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.FLAP_DISPLAY.get((Direction)pState.getValue(HORIZONTAL_FACING));
   }

   @Override
   public Class<FlapDisplayBlockEntity> getBlockEntityClass() {
      return FlapDisplayBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends FlapDisplayBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends FlapDisplayBlockEntity>)AllBlockEntityTypes.FLAP_DISPLAY.get();
   }

   @Override
   public float getParticleTargetRadius() {
      return 0.85F;
   }

   @Override
   public float getParticleInitialRadius() {
      return 0.75F;
   }

   private BlockState updateColumn(Level level, BlockPos pos, BlockState state, boolean present) {
      MutableBlockPos currentPos = new MutableBlockPos();
      Axis axis = this.getConnectionAxis(state);

      for (Direction connection : Iterate.directionsInAxis(Axis.Y)) {
         boolean connect = true;

         label48:
         for (Direction movement : Iterate.directionsInAxis(axis)) {
            currentPos.set(pos);
            int i = 0;

            while (true) {
               if (i < 1000 && level.isLoaded(currentPos)) {
                  BlockState other1 = currentPos.equals(pos) ? state : level.getBlockState(currentPos);
                  BlockState other2 = level.getBlockState(currentPos.relative(connection));
                  boolean col1 = this.canConnect(state, other1);
                  boolean col2 = this.canConnect(state, other2);
                  currentPos.move(movement);
                  if (col1 || col2) {
                     if (!col1 || !col2) {
                        connect = false;
                        break label48;
                     }

                     i++;
                     continue;
                  }
               }
            }
         }

         state = setConnection(state, connection, connect);
      }

      return state;
   }

   @Override
   public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
      if (pOldState.getBlock() != this) {
         LevelTickAccess<Block> blockTicks = pLevel.getBlockTicks();
         if (!blockTicks.hasScheduledTick(pPos, this)) {
            pLevel.scheduleTick(pPos, this, 1);
         }
      }
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pState.getBlock() == this) {
         BlockPos belowPos = pPos.relative(Direction.fromAxisAndDirection(this.getConnectionAxis(pState), AxisDirection.NEGATIVE));
         BlockState belowState = pLevel.getBlockState(belowPos);
         if (!this.canConnect(pState, belowState)) {
            KineticBlockEntity.switchToBlockState(pLevel, pPos, this.updateColumn(pLevel, pPos, pState, true));
         }

         this.withBlockEntityDo(pLevel, pPos, FlapDisplayBlockEntity::updateControllerStatus);
      }
   }

   public BlockState updateShape(
      BlockState state, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos
   ) {
      return this.updatedShapeInner(state, pDirection, pNeighborState, pLevel, pCurrentPos);
   }

   private BlockState updatedShapeInner(BlockState state, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos) {
      if ((Boolean)state.getValue(BlockStateProperties.WATERLOGGED)) {
         pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      if (!this.canConnect(state, pNeighborState)) {
         return setConnection(state, pDirection, false);
      } else {
         return pDirection.getAxis() == this.getConnectionAxis(state)
            ? (BlockState)this.withPropertiesOf(pNeighborState)
               .setValue(BlockStateProperties.WATERLOGGED, (Boolean)state.getValue(BlockStateProperties.WATERLOGGED))
            : setConnection(state, pDirection, getConnection(pNeighborState, pDirection.getOpposite()));
      }
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
   }

   protected boolean canConnect(BlockState state, BlockState other) {
      return other.getBlock() == this && state.getValue(HORIZONTAL_FACING) == other.getValue(HORIZONTAL_FACING);
   }

   protected Axis getConnectionAxis(BlockState state) {
      return ((Direction)state.getValue(HORIZONTAL_FACING)).getClockWise().getAxis();
   }

   public static boolean getConnection(BlockState state, Direction side) {
      BooleanProperty property = side == Direction.DOWN ? DOWN : (side == Direction.UP ? UP : null);
      return property != null && (Boolean)state.getValue(property);
   }

   public static BlockState setConnection(BlockState state, Direction side, boolean connect) {
      BooleanProperty property = side == Direction.DOWN ? DOWN : (side == Direction.UP ? UP : null);
      if (property != null) {
         state = (BlockState)state.setValue(property, connect);
      }

      return state;
   }

   @Override
   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
      if (!pIsMoving && pNewState.getBlock() != this) {
         for (Direction d : Iterate.directionsInAxis(this.getConnectionAxis(pState))) {
            BlockPos relative = pPos.relative(d);
            BlockState adjacent = pLevel.getBlockState(relative);
            if (this.canConnect(pState, adjacent)) {
               KineticBlockEntity.switchToBlockState(pLevel, relative, this.updateColumn(pLevel, relative, adjacent, false));
            }
         }
      }
   }

   @MethodsReturnNonnullByDefault
   private static class PlacementHelper implements IPlacementHelper {
      public Predicate<ItemStack> getItemPredicate() {
         return AllBlocks.DISPLAY_BOARD::isIn;
      }

      public Predicate<BlockState> getStatePredicate() {
         return AllBlocks.DISPLAY_BOARD::has;
      }

      public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
         List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(
            pos,
            ray.getLocation(),
            ((Direction)state.getValue(FlapDisplayBlock.HORIZONTAL_FACING)).getAxis(),
            dir -> world.getBlockState(pos.relative(dir)).canBeReplaced()
         );
         return directions.isEmpty()
            ? PlacementOffset.fail()
            : PlacementOffset.success(
               pos.relative(directions.get(0)),
               s -> ((FlapDisplayBlock)AllBlocks.DISPLAY_BOARD.get())
                     .updateColumn(
                        world,
                        pos.relative(directions.get(0)),
                        (BlockState)s.setValue(HorizontalKineticBlock.HORIZONTAL_FACING, (Direction)state.getValue(FlapDisplayBlock.HORIZONTAL_FACING)),
                        true
                     )
            );
      }
   }
}
