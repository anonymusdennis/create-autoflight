package com.simibubi.create.content.decoration.steamWhistle;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WhistleExtenderBlock extends Block implements IWrenchable {
   public static final EnumProperty<WhistleExtenderBlock.WhistleExtenderShape> SHAPE = EnumProperty.create(
      "shape", WhistleExtenderBlock.WhistleExtenderShape.class
   );
   public static final EnumProperty<WhistleBlock.WhistleSize> SIZE = WhistleBlock.SIZE;

   public WhistleExtenderBlock(Properties p_49795_) {
      super(p_49795_);
      this.registerDefaultState(
         (BlockState)((BlockState)this.defaultBlockState().setValue(SHAPE, WhistleExtenderBlock.WhistleExtenderShape.SINGLE))
            .setValue(SIZE, WhistleBlock.WhistleSize.MEDIUM)
      );
   }

   @Override
   public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
      Level world = context.getLevel();
      BlockPos pos = context.getClickedPos();
      if (context.getClickLocation().y < (double)((float)context.getClickedPos().getY() + 0.5F)
         || state.getValue(SHAPE) == WhistleExtenderBlock.WhistleExtenderShape.SINGLE) {
         return IWrenchable.super.onSneakWrenched(state, context);
      } else if (!(world instanceof ServerLevel)) {
         return InteractionResult.SUCCESS;
      } else {
         world.setBlock(pos, (BlockState)state.setValue(SHAPE, WhistleExtenderBlock.WhistleExtenderShape.SINGLE), 3);
         IWrenchable.playRemoveSound(world, pos);
         return InteractionResult.SUCCESS;
      }
   }

   protected UseOnContext relocateContext(UseOnContext context, BlockPos target) {
      return new UseOnContext(
         context.getPlayer(), context.getHand(), new BlockHitResult(context.getClickLocation(), context.getClickedFace(), target, context.isInside())
      );
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (player != null && AllBlocks.STEAM_WHISTLE.isIn(stack)) {
         BlockPos findRoot = findRoot(level, pos);
         BlockState blockState = level.getBlockState(findRoot);
         return blockState.getBlock() instanceof WhistleBlock whistle
            ? whistle.useItemOn(
               stack,
               blockState,
               level,
               findRoot,
               player,
               hand,
               new BlockHitResult(hitResult.getLocation(), hitResult.getDirection(), findRoot, hitResult.isInside())
            )
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      Level level = context.getLevel();
      BlockPos findRoot = findRoot(level, context.getClickedPos());
      BlockState blockState = level.getBlockState(findRoot);
      return blockState.getBlock() instanceof WhistleBlock whistle
         ? whistle.onWrenched(blockState, this.relocateContext(context, findRoot))
         : IWrenchable.super.onWrenched(state, context);
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
      return AllBlocks.STEAM_WHISTLE.asStack();
   }

   public static BlockPos findRoot(LevelAccessor pLevel, BlockPos pPos) {
      BlockPos currentPos = pPos.below();

      while (true) {
         BlockState blockState = pLevel.getBlockState(currentPos);
         if (!AllBlocks.STEAM_WHISTLE_EXTENSION.has(blockState)) {
            return currentPos;
         }

         currentPos = currentPos.below();
      }
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      BlockState below = pLevel.getBlockState(pPos.below());
      return below.is(this) && below.getValue(SHAPE) != WhistleExtenderBlock.WhistleExtenderShape.SINGLE || AllBlocks.STEAM_WHISTLE.has(below);
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      if (pFacing.getAxis() != Axis.Y) {
         return pState;
      } else if (pFacing == Direction.UP) {
         boolean connected = pState.getValue(SHAPE) == WhistleExtenderBlock.WhistleExtenderShape.DOUBLE_CONNECTED;
         boolean shouldConnect = pLevel.getBlockState(pCurrentPos.above()).is(this);
         if (!connected && shouldConnect) {
            return (BlockState)pState.setValue(SHAPE, WhistleExtenderBlock.WhistleExtenderShape.DOUBLE_CONNECTED);
         } else {
            return connected && !shouldConnect ? (BlockState)pState.setValue(SHAPE, WhistleExtenderBlock.WhistleExtenderShape.DOUBLE) : pState;
         }
      } else {
         return !pState.canSurvive(pLevel, pCurrentPos)
            ? Blocks.AIR.defaultBlockState()
            : (BlockState)pState.setValue(SIZE, (WhistleBlock.WhistleSize)pLevel.getBlockState(pCurrentPos.below()).getValue(SIZE));
      }
   }

   public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      if (pOldState.getBlock() != this || pOldState.getValue(SHAPE) != pState.getValue(SHAPE)) {
         WhistleBlock.queuePitchUpdate(pLevel, findRoot(pLevel, pPos));
      }
   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      if (pNewState.getBlock() != this) {
         WhistleBlock.queuePitchUpdate(pLevel, findRoot(pLevel, pPos));
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{SHAPE, SIZE}));
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      WhistleBlock.WhistleSize size = (WhistleBlock.WhistleSize)pState.getValue(SIZE);
      switch ((WhistleExtenderBlock.WhistleExtenderShape)pState.getValue(SHAPE)) {
         case SINGLE:
         default:
            return size == WhistleBlock.WhistleSize.LARGE
               ? AllShapes.WHISTLE_EXTENDER_LARGE
               : (size == WhistleBlock.WhistleSize.MEDIUM ? AllShapes.WHISTLE_EXTENDER_MEDIUM : AllShapes.WHISTLE_EXTENDER_SMALL);
         case DOUBLE:
            return size == WhistleBlock.WhistleSize.LARGE
               ? AllShapes.WHISTLE_EXTENDER_LARGE_DOUBLE
               : (size == WhistleBlock.WhistleSize.MEDIUM ? AllShapes.WHISTLE_EXTENDER_MEDIUM_DOUBLE : AllShapes.WHISTLE_EXTENDER_SMALL_DOUBLE);
         case DOUBLE_CONNECTED:
            return size == WhistleBlock.WhistleSize.LARGE
               ? AllShapes.WHISTLE_EXTENDER_LARGE_DOUBLE_CONNECTED
               : (
                  size == WhistleBlock.WhistleSize.MEDIUM
                     ? AllShapes.WHISTLE_EXTENDER_MEDIUM_DOUBLE_CONNECTED
                     : AllShapes.WHISTLE_EXTENDER_SMALL_DOUBLE_CONNECTED
               );
      }
   }

   public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir) {
      return AllBlocks.STEAM_WHISTLE.has(neighborState) && dir == Direction.DOWN;
   }

   public static enum WhistleExtenderShape implements StringRepresentable {
      SINGLE,
      DOUBLE,
      DOUBLE_CONNECTED;

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}
