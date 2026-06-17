package com.simibubi.create.content.kinetics.deployer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BeltDeployerCallbacks {
   public static BeltProcessingBehaviour.ProcessingResult onItemReceived(
      TransportedItemStack s, TransportedItemStackHandlerBehaviour i, DeployerBlockEntity blockEntity
   ) {
      if (blockEntity.getSpeed() == 0.0F) {
         return BeltProcessingBehaviour.ProcessingResult.PASS;
      } else if (blockEntity.mode == DeployerBlockEntity.Mode.PUNCH) {
         return BeltProcessingBehaviour.ProcessingResult.PASS;
      } else {
         BlockState blockState = blockEntity.getBlockState();
         if (!blockState.hasProperty(DirectionalKineticBlock.FACING) || blockState.getValue(DirectionalKineticBlock.FACING) != Direction.DOWN) {
            return BeltProcessingBehaviour.ProcessingResult.PASS;
         } else if (blockEntity.state != DeployerBlockEntity.State.WAITING) {
            return BeltProcessingBehaviour.ProcessingResult.HOLD;
         } else if (blockEntity.redstoneLocked) {
            return BeltProcessingBehaviour.ProcessingResult.PASS;
         } else {
            DeployerFakePlayer player = blockEntity.getPlayer();
            ItemStack held = player == null ? ItemStack.EMPTY : player.getMainHandItem();
            if (held.isEmpty()) {
               return BeltProcessingBehaviour.ProcessingResult.HOLD;
            } else if (blockEntity.getRecipe(s.stack) == null) {
               return BeltProcessingBehaviour.ProcessingResult.PASS;
            } else {
               blockEntity.start();
               return BeltProcessingBehaviour.ProcessingResult.HOLD;
            }
         }
      }
   }

   public static BeltProcessingBehaviour.ProcessingResult whenItemHeld(
      TransportedItemStack s, TransportedItemStackHandlerBehaviour i, DeployerBlockEntity blockEntity
   ) {
      if (blockEntity.getSpeed() == 0.0F) {
         return BeltProcessingBehaviour.ProcessingResult.PASS;
      } else {
         BlockState blockState = blockEntity.getBlockState();
         if (blockState.hasProperty(DirectionalKineticBlock.FACING) && blockState.getValue(DirectionalKineticBlock.FACING) == Direction.DOWN) {
            DeployerFakePlayer player = blockEntity.getPlayer();
            ItemStack held = player == null ? ItemStack.EMPTY : player.getMainHandItem();
            if (held.isEmpty()) {
               return BeltProcessingBehaviour.ProcessingResult.HOLD;
            } else {
               RecipeHolder<? extends Recipe<?>> recipeHolder = blockEntity.getRecipe(s.stack);
               if (recipeHolder == null) {
                  return BeltProcessingBehaviour.ProcessingResult.PASS;
               } else if (blockEntity.state == DeployerBlockEntity.State.RETRACTING && blockEntity.timer == 1000) {
                  activate(s, i, blockEntity, recipeHolder.value());
                  return BeltProcessingBehaviour.ProcessingResult.HOLD;
               } else {
                  if (blockEntity.state == DeployerBlockEntity.State.WAITING) {
                     if (blockEntity.redstoneLocked) {
                        return BeltProcessingBehaviour.ProcessingResult.PASS;
                     }

                     blockEntity.start();
                  }

                  return BeltProcessingBehaviour.ProcessingResult.HOLD;
               }
            }
         } else {
            return BeltProcessingBehaviour.ProcessingResult.PASS;
         }
      }
   }

   public static void activate(
      TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler, DeployerBlockEntity blockEntity, Recipe<?> recipe
   ) {
      List<TransportedItemStack> collect = RecipeApplier.applyRecipeOn(blockEntity.getLevel(), transported.stack.copyWithCount(1), recipe, true)
         .stream()
         .map(stack -> {
            TransportedItemStack copy = transported.copy();
            boolean centered = BeltHelper.isItemUpright(stack);
            copy.stack = stack;
            copy.locked = true;
            copy.angle = centered ? 180 : Create.RANDOM.nextInt(360);
            return copy;
         })
         .map(t -> {
            t.locked = false;
            return (TransportedItemStack)t;
         })
         .collect(Collectors.toList());
      blockEntity.award(AllAdvancements.DEPLOYER);
      transported.clearFanProcessingData();
      TransportedItemStack left = transported.copy();
      blockEntity.player.spawnedItemEffects = transported.stack.copy();
      left.stack.shrink(1);
      ItemStack resultItem;
      if (collect.isEmpty()) {
         resultItem = left.stack.copy();
         handler.handleProcessingOnItem(transported, TransportedItemStackHandlerBehaviour.TransportedResult.convertTo(left));
      } else {
         resultItem = collect.get(0).stack.copy();
         handler.handleProcessingOnItem(transported, TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(collect, left));
      }

      ItemStack heldItem = blockEntity.player.getMainHandItem();
      boolean keepHeld = recipe instanceof ItemApplicationRecipe && ((ItemApplicationRecipe)recipe).shouldKeepHeldItem();
      if (!keepHeld) {
         if (heldItem.getMaxDamage() > 0) {
            heldItem.hurtAndBreak(1, blockEntity.player, EquipmentSlot.MAINHAND);
         } else {
            Player player = blockEntity.player;
            ItemStack leftover = heldItem.getCraftingRemainingItem();
            heldItem.shrink(1);
            if (heldItem.isEmpty()) {
               player.setItemInHand(InteractionHand.MAIN_HAND, leftover);
            } else if (!player.getInventory().add(leftover)) {
               player.drop(leftover, false);
            }
         }
      }

      if (!resultItem.isEmpty()) {
         awardAdvancements(blockEntity, resultItem);
      }

      BlockPos pos = blockEntity.getBlockPos();
      Level world = blockEntity.getLevel();
      if (heldItem.isEmpty()) {
         world.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.25F, 1.0F);
      }

      world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.25F, 0.75F);
      if (recipe instanceof SandPaperPolishingRecipe) {
         AllSoundEvents.SANDING_SHORT.playOnServer(world, pos, 0.35F, 1.0F);
      }

      blockEntity.notifyUpdate();
   }

   private static void awardAdvancements(DeployerBlockEntity blockEntity, ItemStack created) {
      CreateAdvancement advancement = null;
      if (AllBlocks.ANDESITE_CASING.isIn(created)) {
         advancement = AllAdvancements.ANDESITE_CASING;
      } else if (AllBlocks.BRASS_CASING.isIn(created)) {
         advancement = AllAdvancements.BRASS_CASING;
      } else if (AllBlocks.COPPER_CASING.isIn(created)) {
         advancement = AllAdvancements.COPPER_CASING;
      } else {
         if (!AllBlocks.RAILWAY_CASING.isIn(created)) {
            return;
         }

         advancement = AllAdvancements.TRAIN_CASING;
      }

      blockEntity.award(advancement);
   }
}
