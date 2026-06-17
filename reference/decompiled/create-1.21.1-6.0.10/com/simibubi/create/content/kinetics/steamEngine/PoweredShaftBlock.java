package com.simibubi.create.content.kinetics.steamEngine;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PoweredShaftBlock extends AbstractShaftBlock {
   public PoweredShaftBlock(Properties properties) {
      super(properties);
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.EIGHT_VOXEL_POLE.get((Axis)pState.getValue(AXIS));
   }

   @Override
   public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends KineticBlockEntity>)AllBlockEntityTypes.POWERED_SHAFT.get();
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.isShiftKeyDown() && player.mayBuild()) {
         IPlacementHelper helper = PlacementHelpers.get(ShaftBlock.placementHelperId);
         return helper.matchesItem(stack)
            ? helper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem)stack.getItem(), player, hand, hitResult)
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public RenderShape getRenderShape(BlockState pState) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (!stillValid(pState, pLevel, pPos)) {
         pLevel.setBlock(
            pPos,
            (BlockState)((BlockState)AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, (Axis)pState.getValue(AXIS)))
               .setValue(WATERLOGGED, (Boolean)pState.getValue(WATERLOGGED)),
            3
         );
      }
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
      return AllBlocks.SHAFT.asStack();
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return stillValid(pState, pLevel, pPos);
   }

   public static boolean stillValid(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      for (Direction d : Iterate.directions) {
         if (d.getAxis() != pState.getValue(AXIS)) {
            BlockPos enginePos = pPos.relative(d, 2);
            BlockState engineState = pLevel.getBlockState(enginePos);
            if (engineState.getBlock() instanceof SteamEngineBlock engine
               && SteamEngineBlock.getShaftPos(engineState, enginePos).equals(pPos)
               && SteamEngineBlock.isShaftValid(engineState, pState)) {
               return true;
            }
         }
      }

      return false;
   }

   public static BlockState getEquivalent(BlockState stateForPlacement) {
      return (BlockState)((BlockState)AllBlocks.POWERED_SHAFT.getDefaultState().setValue(AXIS, (Axis)stateForPlacement.getValue(ShaftBlock.AXIS)))
         .setValue(WATERLOGGED, (Boolean)stateForPlacement.getValue(WATERLOGGED));
   }
}
