package com.simibubi.create.content.decoration.slidingDoor;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.contraptions.ContraptionWorld;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.IHaveBigOutline;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.BlockSetType.PressurePlateSensitivity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SlidingDoorBlock extends DoorBlock implements IWrenchable, IBE<SlidingDoorBlockEntity>, IHaveBigOutline {
   public static final Supplier<BlockSetType> TRAIN_SET_TYPE = () -> new BlockSetType(
         "create:train",
         true,
         true,
         true,
         PressurePlateSensitivity.EVERYTHING,
         SoundType.NETHERITE_BLOCK,
         SoundEvents.IRON_DOOR_CLOSE,
         SoundEvents.IRON_DOOR_OPEN,
         SoundEvents.IRON_TRAPDOOR_CLOSE,
         SoundEvents.IRON_TRAPDOOR_OPEN,
         SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF,
         SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
         SoundEvents.STONE_BUTTON_CLICK_OFF,
         SoundEvents.STONE_BUTTON_CLICK_ON
      );
   public static final Supplier<BlockSetType> GLASS_SET_TYPE = () -> new BlockSetType(
         "create:glass",
         true,
         true,
         true,
         PressurePlateSensitivity.EVERYTHING,
         SoundType.GLASS,
         SoundEvents.IRON_DOOR_CLOSE,
         SoundEvents.IRON_DOOR_OPEN,
         SoundEvents.IRON_TRAPDOOR_CLOSE,
         SoundEvents.IRON_TRAPDOOR_OPEN,
         SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF,
         SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
         SoundEvents.STONE_BUTTON_CLICK_OFF,
         SoundEvents.STONE_BUTTON_CLICK_ON
      );
   public static final Supplier<BlockSetType> STONE_SET_TYPE = () -> new BlockSetType(
         "create:stone",
         true,
         true,
         true,
         PressurePlateSensitivity.EVERYTHING,
         SoundType.STONE,
         SoundEvents.IRON_DOOR_CLOSE,
         SoundEvents.IRON_DOOR_OPEN,
         SoundEvents.IRON_TRAPDOOR_CLOSE,
         SoundEvents.IRON_TRAPDOOR_OPEN,
         SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF,
         SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
         SoundEvents.STONE_BUTTON_CLICK_OFF,
         SoundEvents.STONE_BUTTON_CLICK_ON
      );
   public static final BooleanProperty VISIBLE = BooleanProperty.create("visible");
   private final boolean folds;

   public static SlidingDoorBlock metal(Properties properties, boolean folds) {
      return new SlidingDoorBlock(properties, TRAIN_SET_TYPE.get(), folds);
   }

   public static SlidingDoorBlock glass(Properties properties, boolean folds) {
      return new SlidingDoorBlock(properties, GLASS_SET_TYPE.get(), folds);
   }

   public static SlidingDoorBlock stone(Properties properties, boolean folds) {
      return new SlidingDoorBlock(properties, STONE_SET_TYPE.get(), folds);
   }

   public SlidingDoorBlock(Properties properties, BlockSetType type, boolean folds) {
      super(type, properties);
      this.folds = folds;
   }

   public boolean isFoldingDoor() {
      return this.folds;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{VISIBLE}));
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      if ((Boolean)pState.getValue(OPEN) || !(Boolean)pState.getValue(VISIBLE) && !(pLevel instanceof ContraptionWorld)) {
         Direction direction = (Direction)pState.getValue(FACING);
         boolean hinge = pState.getValue(HINGE) == DoorHingeSide.RIGHT;
         return SlidingDoorShapes.get(direction, hinge, this.isFoldingDoor());
      } else {
         return super.getShape(pState, pLevel, pPos, pContext);
      }
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return pState.getValue(HALF) == DoubleBlockHalf.LOWER || pLevel.getBlockState(pPos.below()).is(this);
   }

   public VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return this.getShape(pState, pLevel, pPos, CollisionContext.empty());
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState stateForPlacement = super.getStateForPlacement(pContext);
      return stateForPlacement != null && stateForPlacement.getValue(OPEN)
         ? (BlockState)((BlockState)stateForPlacement.setValue(OPEN, false)).setValue(POWERED, false)
         : stateForPlacement;
   }

   public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      if (!pOldState.is(this)) {
         this.deferUpdate(pLevel, pPos);
      }
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      BlockState blockState = super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
      if (blockState.isAir()) {
         return blockState;
      } else {
         DoubleBlockHalf doubleblockhalf = (DoubleBlockHalf)blockState.getValue(HALF);
         if (pFacing.getAxis() == Axis.Y && doubleblockhalf == DoubleBlockHalf.LOWER == (pFacing == Direction.UP)) {
            return pFacingState.is(this) && pFacingState.getValue(HALF) != doubleblockhalf
               ? (BlockState)blockState.setValue(VISIBLE, (Boolean)pFacingState.getValue(VISIBLE))
               : Blocks.AIR.defaultBlockState();
         } else {
            return blockState;
         }
      }
   }

   public void setOpen(@Nullable Entity entity, Level level, BlockState state, BlockPos pos, boolean open) {
      if (state.is(this)) {
         if ((Boolean)state.getValue(OPEN) != open) {
            BlockState changedState = (BlockState)state.setValue(OPEN, open);
            if (open) {
               changedState = (BlockState)changedState.setValue(VISIBLE, false);
            }

            level.setBlock(pos, changedState, 10);
            DoorHingeSide hinge = (DoorHingeSide)changedState.getValue(HINGE);
            Direction facing = (Direction)changedState.getValue(FACING);
            BlockPos otherPos = pos.relative(hinge == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
            BlockState otherDoor = level.getBlockState(otherPos);
            if (isDoubleDoor(changedState, hinge, facing, otherDoor)) {
               this.setOpen(entity, level, otherDoor, otherPos, open);
            }

            this.playSound(entity, level, pos, open);
            level.gameEvent(entity, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
         }
      }
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      boolean lower = pState.getValue(HALF) == DoubleBlockHalf.LOWER;
      boolean isPowered = isDoorPowered(pLevel, pPos, pState);
      if (!this.defaultBlockState().is(pBlock)) {
         if (isPowered != (Boolean)pState.getValue(POWERED)) {
            SlidingDoorBlockEntity be = this.getBlockEntity(pLevel, lower ? pPos : pPos.below());
            if (be == null || !be.deferUpdate) {
               BlockState changedState = (BlockState)((BlockState)pState.setValue(POWERED, isPowered)).setValue(OPEN, isPowered);
               if (isPowered) {
                  changedState = (BlockState)changedState.setValue(VISIBLE, false);
               }

               if (isPowered != (Boolean)pState.getValue(OPEN)) {
                  this.playSound(null, pLevel, pPos, isPowered);
                  pLevel.gameEvent(null, isPowered ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);
                  DoorHingeSide hinge = (DoorHingeSide)changedState.getValue(HINGE);
                  Direction facing = (Direction)changedState.getValue(FACING);
                  BlockPos otherPos = pPos.relative(hinge == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
                  BlockState otherDoor = pLevel.getBlockState(otherPos);
                  if (isDoubleDoor(changedState, hinge, facing, otherDoor)) {
                     otherDoor = (BlockState)((BlockState)otherDoor.setValue(POWERED, isPowered)).setValue(OPEN, isPowered);
                     if (isPowered) {
                        otherDoor = (BlockState)otherDoor.setValue(VISIBLE, false);
                     }

                     pLevel.setBlock(otherPos, otherDoor, 2);
                  }
               }

               pLevel.setBlock(pPos, changedState, 2);
            }
         }
      }
   }

   public static boolean isDoorPowered(Level pLevel, BlockPos pPos, BlockState state) {
      boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
      DoorHingeSide hinge = (DoorHingeSide)state.getValue(HINGE);
      Direction facing = (Direction)state.getValue(FACING);
      BlockPos otherPos = pPos.relative(hinge == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
      BlockState otherDoor = pLevel.getBlockState(otherPos);
      return !isDoubleDoor((BlockState)state.cycle(OPEN), hinge, facing, otherDoor)
            || !pLevel.hasNeighborSignal(otherPos) && !pLevel.hasNeighborSignal(otherPos.relative(lower ? Direction.UP : Direction.DOWN))
         ? pLevel.hasNeighborSignal(pPos) || pLevel.hasNeighborSignal(pPos.relative(lower ? Direction.UP : Direction.DOWN))
         : true;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      state = (BlockState)state.cycle(OPEN);
      boolean isOpen = (Boolean)state.getValue(OPEN);
      if (isOpen) {
         state = (BlockState)state.setValue(VISIBLE, false);
      }

      level.setBlock(pos, state, 10);
      level.gameEvent(player, this.isOpen(state) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
      DoorHingeSide hinge = (DoorHingeSide)state.getValue(HINGE);
      Direction facing = (Direction)state.getValue(FACING);
      BlockPos otherPos = pos.relative(hinge == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
      BlockState otherDoor = level.getBlockState(otherPos);
      if (isDoubleDoor(state, hinge, facing, otherDoor)) {
         this.useWithoutItem(otherDoor, level, otherPos, player, hitResult);
      } else if (isOpen) {
         this.playSound(player, level, pos, true);
         level.gameEvent(player, GameEvent.BLOCK_OPEN, pos);
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public void deferUpdate(LevelAccessor level, BlockPos pos) {
      this.withBlockEntityDo(level, pos, sdte -> sdte.deferUpdate = true);
   }

   public static boolean isDoubleDoor(BlockState pState, DoorHingeSide hinge, Direction facing, BlockState otherDoor) {
      return otherDoor.getBlock() == pState.getBlock()
         && otherDoor.getValue(HINGE) != hinge
         && otherDoor.getValue(FACING) == facing
         && otherDoor.getValue(OPEN) != pState.getValue(OPEN)
         && otherDoor.getValue(HALF) == pState.getValue(HALF);
   }

   public RenderShape getRenderShape(BlockState pState) {
      return pState.getValue(VISIBLE) ? RenderShape.MODEL : RenderShape.ENTITYBLOCK_ANIMATED;
   }

   private void playSound(@Nullable Entity pSource, Level pLevel, BlockPos pPos, boolean pIsOpening) {
      pLevel.playSound(
         pSource,
         pPos,
         pIsOpening ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
         SoundSource.BLOCKS,
         1.0F,
         pLevel.getRandom().nextFloat() * 0.1F + 0.9F
      );
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return state.getValue(HALF) == DoubleBlockHalf.UPPER ? null : IBE.super.newBlockEntity(pos, state);
   }

   @Override
   public Class<SlidingDoorBlockEntity> getBlockEntityClass() {
      return SlidingDoorBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends SlidingDoorBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SlidingDoorBlockEntity>)AllBlockEntityTypes.SLIDING_DOOR.get();
   }
}
