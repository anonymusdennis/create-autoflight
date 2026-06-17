package com.simibubi.create.content.decoration.steamWhistle;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WhistleBlock extends Block implements IBE<WhistleBlockEntity>, IWrenchable {
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final BooleanProperty WALL = BooleanProperty.create("wall");
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final EnumProperty<WhistleBlock.WhistleSize> SIZE = EnumProperty.create("size", WhistleBlock.WhistleSize.class);

   public WhistleBlock(Properties p_49795_) {
      super(p_49795_);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(POWERED, false)).setValue(WALL, false))
            .setValue(SIZE, WhistleBlock.WhistleSize.MEDIUM)
      );
   }

   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
      AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return FluidTankBlock.isTank(pLevel.getBlockState(pPos.relative(getAttachedDirection(pState))));
   }

   @Override
   public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
      return (BlockState)originalState.cycle(SIZE);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{FACING, POWERED, SIZE, WALL}));
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Level level = pContext.getLevel();
      BlockPos clickedPos = pContext.getClickedPos();
      Direction face = pContext.getClickedFace();
      boolean wall = true;
      if (face.getAxis() == Axis.Y) {
         face = pContext.getHorizontalDirection().getOpposite();
         wall = false;
      }

      BlockState state = (BlockState)((BlockState)((BlockState)super.getStateForPlacement(pContext).setValue(FACING, face.getOpposite()))
            .setValue(POWERED, level.hasNeighborSignal(clickedPos)))
         .setValue(WALL, wall);
      return !this.canSurvive(state, level, clickedPos) ? null : state;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (player == null) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (AllBlocks.STEAM_WHISTLE.isIn(stack)) {
         incrementSize(level, pos);
         return ItemInteractionResult.SUCCESS;
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public static void incrementSize(LevelAccessor pLevel, BlockPos pPos) {
      BlockState base = pLevel.getBlockState(pPos);
      if (base.hasProperty(SIZE)) {
         WhistleBlock.WhistleSize size = (WhistleBlock.WhistleSize)base.getValue(SIZE);
         SoundType soundtype = base.getSoundType();
         BlockPos currentPos = pPos.above();

         for (int i = 1; i <= 6; i++) {
            BlockState blockState = pLevel.getBlockState(currentPos);
            float pVolume = (soundtype.getVolume() + 1.0F) / 2.0F;
            SoundEvent growSound = (SoundEvent)SoundEvents.NOTE_BLOCK_XYLOPHONE.value();
            SoundEvent hitSound = soundtype.getHitSound();
            if (!AllBlocks.STEAM_WHISTLE_EXTENSION.has(blockState)) {
               if (!blockState.canBeReplaced()) {
                  return;
               }

               pLevel.setBlock(currentPos, (BlockState)AllBlocks.STEAM_WHISTLE_EXTENSION.getDefaultState().setValue(SIZE, size), 3);
               if (soundtype != null) {
                  float pPitch = (float)Math.pow(2.0, (double)(-(i * 2 - 1)) / 12.0);
                  pLevel.playSound(null, currentPos, growSound, SoundSource.BLOCKS, pVolume / 4.0F, pPitch);
                  pLevel.playSound(null, currentPos, hitSound, SoundSource.BLOCKS, pVolume, pPitch);
               }

               return;
            }

            if (blockState.getValue(WhistleExtenderBlock.SHAPE) == WhistleExtenderBlock.WhistleExtenderShape.SINGLE) {
               pLevel.setBlock(currentPos, (BlockState)blockState.setValue(WhistleExtenderBlock.SHAPE, WhistleExtenderBlock.WhistleExtenderShape.DOUBLE), 3);
               if (soundtype != null) {
                  float pPitch = (float)Math.pow(2.0, (double)(-(i * 2)) / 12.0);
                  pLevel.playSound(null, currentPos, growSound, SoundSource.BLOCKS, pVolume / 4.0F, pPitch);
                  pLevel.playSound(null, currentPos, hitSound, SoundSource.BLOCKS, pVolume, pPitch);
               }

               return;
            }

            currentPos = currentPos.above();
         }
      }
   }

   public static void queuePitchUpdate(LevelAccessor level, BlockPos pos) {
      BlockState blockState = level.getBlockState(pos);
      if (blockState.getBlock() instanceof WhistleBlock whistle && !level.getBlockTicks().hasScheduledTick(pos, whistle)) {
         level.scheduleTick(pos, whistle, 1);
      }
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      this.withBlockEntityDo(pLevel, pPos, WhistleBlockEntity::updatePitch);
   }

   public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      FluidTankBlock.updateBoilerState(pState, pLevel, pPos.relative(getAttachedDirection(pState)));
      if (pOldState.getBlock() != this || pOldState.getValue(SIZE) != pState.getValue(SIZE)) {
         queuePitchUpdate(pLevel, pPos);
      }
   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      IBE.onRemove(pState, pLevel, pPos, pNewState);
      FluidTankBlock.updateBoilerState(pState, pLevel, pPos.relative(getAttachedDirection(pState)));
   }

   public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      if (!worldIn.isClientSide) {
         boolean previouslyPowered = (Boolean)state.getValue(POWERED);
         if (previouslyPowered != worldIn.hasNeighborSignal(pos)) {
            worldIn.setBlock(pos, (BlockState)state.cycle(POWERED), 2);
         }
      }
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      return getAttachedDirection(pState) == pFacing && !pState.canSurvive(pLevel, pCurrentPos) ? Blocks.AIR.defaultBlockState() : pState;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      WhistleBlock.WhistleSize size = (WhistleBlock.WhistleSize)pState.getValue(SIZE);
      if (!(Boolean)pState.getValue(WALL)) {
         return size == WhistleBlock.WhistleSize.SMALL
            ? AllShapes.WHISTLE_SMALL_FLOOR
            : (size == WhistleBlock.WhistleSize.MEDIUM ? AllShapes.WHISTLE_MEDIUM_FLOOR : AllShapes.WHISTLE_LARGE_FLOOR);
      } else {
         Direction direction = (Direction)pState.getValue(FACING);
         return (size == WhistleBlock.WhistleSize.SMALL
               ? AllShapes.WHISTLE_SMALL_WALL
               : (size == WhistleBlock.WhistleSize.MEDIUM ? AllShapes.WHISTLE_MEDIUM_WALL : AllShapes.WHISTLE_LARGE_WALL))
            .get(direction);
      }
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   public static Direction getAttachedDirection(BlockState state) {
      return state.getValue(WALL) ? (Direction)state.getValue(FACING) : Direction.DOWN;
   }

   @Override
   public Class<WhistleBlockEntity> getBlockEntityClass() {
      return WhistleBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends WhistleBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends WhistleBlockEntity>)AllBlockEntityTypes.STEAM_WHISTLE.get();
   }

   public BlockState rotate(BlockState pState, Rotation pRotation) {
      return (BlockState)pState.setValue(FACING, pRotation.rotate((Direction)pState.getValue(FACING)));
   }

   public BlockState mirror(BlockState pState, Mirror pMirror) {
      return pMirror == Mirror.NONE ? pState : pState.rotate(pMirror.getRotation((Direction)pState.getValue(FACING)));
   }

   public static enum WhistleSize implements StringRepresentable {
      SMALL,
      MEDIUM,
      LARGE;

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}
