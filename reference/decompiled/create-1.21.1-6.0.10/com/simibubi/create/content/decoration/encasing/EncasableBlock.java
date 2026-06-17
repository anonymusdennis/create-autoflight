package com.simibubi.create.content.decoration.encasing;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public interface EncasableBlock {
   default ItemInteractionResult tryEncase(
      BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player, InteractionHand hand, BlockHitResult ray
   ) {
      for (Block block : EncasingRegistry.getVariants(state.getBlock())) {
         if (block instanceof EncasedBlock encased && encased.getCasing().asItem() == heldItem.getItem()) {
            if (level.isClientSide) {
               return ItemInteractionResult.SUCCESS;
            }

            encased.handleEncasing(state, level, pos, heldItem, player, hand, ray);
            this.playEncaseSound(level, pos);
            return ItemInteractionResult.SUCCESS;
         }
      }

      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   default void playEncaseSound(Level level, BlockPos pos) {
      BlockState newState = level.getBlockState(pos);
      SoundType soundType = newState.getSoundType();
      level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
   }
}
