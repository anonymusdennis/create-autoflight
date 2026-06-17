package com.simibubi.create.content.equipment;

import net.createmod.catnip.levelWrappers.PlacementSimulationServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class TreeFertilizerItem extends Item {
   public TreeFertilizerItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      BlockState state = context.getLevel().getBlockState(context.getClickedPos());
      Block block = state.getBlock();
      if (!(block instanceof BonemealableBlock bonemealableBlock) || !state.is(BlockTags.SAPLINGS)) {
         return super.useOn(context);
      }

      if (state.getOptionalValue(MangrovePropaguleBlock.HANGING).orElse(false)) {
         return InteractionResult.PASS;
      } else if (context.getLevel().isClientSide) {
         BoneMealItem.addGrowthParticles(context.getLevel(), context.getClickedPos(), 100);
         return InteractionResult.SUCCESS;
      } else {
         BlockPos saplingPos = context.getClickedPos();
         TreeFertilizerItem.TreesDreamWorld world = new TreeFertilizerItem.TreesDreamWorld((ServerLevel)context.getLevel(), saplingPos);

         for (BlockPos pos : BlockPos.betweenClosed(-1, 0, -1, 1, 0, 1)) {
            if (context.getLevel().getBlockState(saplingPos.offset(pos)).getBlock() == block) {
               world.setBlockAndUpdate(pos.above(10), this.withStage(state, 1));
            }
         }

         bonemealableBlock.performBonemeal(world, world.getRandom(), BlockPos.ZERO.above(10), this.withStage(state, 1));

         for (BlockPos posx : world.blocksAdded.keySet()) {
            BlockPos actualPos = posx.offset(saplingPos).below(10);
            BlockState newState = (BlockState)world.blocksAdded.get(posx);
            if (context.getLevel().getBlockState(actualPos).getDestroySpeed(context.getLevel(), actualPos) != -1.0F
               && (
                  newState.isRedstoneConductor(world, posx)
                     || context.getLevel().getBlockState(actualPos).getCollisionShape(context.getLevel(), actualPos).isEmpty()
               )) {
               context.getLevel().destroyBlock(actualPos, true);
               context.getLevel().setBlockAndUpdate(actualPos, newState);
            }
         }

         if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
         }

         return InteractionResult.SUCCESS;
      }
   }

   private BlockState withStage(BlockState original, int stage) {
      return !original.hasProperty(BlockStateProperties.STAGE) ? original : (BlockState)original.setValue(BlockStateProperties.STAGE, 1);
   }

   private static class TreesDreamWorld extends PlacementSimulationServerLevel {
      private final BlockState soil;

      protected TreesDreamWorld(ServerLevel wrapped, BlockPos saplingPos) {
         super(wrapped);
         BlockState stateUnderSapling = wrapped.getBlockState(saplingPos.below());
         if (stateUnderSapling.is(BlockTags.DIRT)) {
            stateUnderSapling = Blocks.DIRT.defaultBlockState();
         }

         this.soil = stateUnderSapling;
      }

      public BlockState getBlockState(BlockPos pos) {
         return pos.getY() <= 9 ? this.soil : super.getBlockState(pos);
      }

      public boolean setBlock(BlockPos pos, BlockState newState, int flags) {
         return newState.getBlock() == Blocks.PODZOL ? true : super.setBlock(pos, newState, flags);
      }
   }
}
