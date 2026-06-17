package dev.simulated_team.simulated.content.items.rope.RopeItem;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class RopeItem extends Item {
   public RopeItem(Properties properties) {
      super(properties);
   }

   public static boolean isValidRopeAttachment(Level level, BlockPos blockPos) {
      boolean validLocation = false;
      if (level.getBlockEntity(blockPos) instanceof SmartBlockEntity smartBlockEntity) {
         RopeStrandHolderBehavior behavior = (RopeStrandHolderBehavior)smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
         if (behavior != null && !behavior.isAttached()) {
            validLocation = true;
         }
      }

      return validLocation;
   }

   public static RopeStrandHolderBehavior getRopeHolder(Level level, BlockPos blockPos) {
      RopeStrandHolderBehavior holder = null;
      if (level.getBlockEntity(blockPos) instanceof SmartBlockEntity smartBlockEntity) {
         RopeStrandHolderBehavior behavior = (RopeStrandHolderBehavior)smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
         if (behavior != null) {
            holder = behavior;
         }
      }

      return holder;
   }

   public InteractionResult useOn(UseOnContext context) {
      BlockPos clickedPos = context.getClickedPos();
      Level level = context.getLevel();
      ItemStack heldStack = context.getItemInHand();
      Player player = context.getPlayer();
      boolean validLocation = isValidRopeAttachment(level, clickedPos);
      if (player != null && player.isShiftKeyDown()) {
         heldStack.remove(SimDataComponents.ROPE_FIRST_CONNECTION);
         return InteractionResult.SUCCESS;
      } else if (validLocation) {
         if (heldStack.has(SimDataComponents.ROPE_FIRST_CONNECTION)) {
            if (!level.isClientSide) {
               if (!this.attachRope(level, (BlockPos)heldStack.get(SimDataComponents.ROPE_FIRST_CONNECTION), clickedPos, !player.hasInfiniteMaterials())) {
                  heldStack.remove(SimDataComponents.ROPE_FIRST_CONNECTION);
                  return InteractionResult.SUCCESS;
               }

               SimAdvancements.LEARNING_THE_ROPES.awardTo(player);
            }

            heldStack.remove(SimDataComponents.ROPE_FIRST_CONNECTION);
            if (!player.hasInfiniteMaterials()) {
               context.getItemInHand().shrink(1);
            }

            return InteractionResult.SUCCESS;
         } else {
            heldStack.set(SimDataComponents.ROPE_FIRST_CONNECTION, clickedPos);
            return InteractionResult.SUCCESS;
         }
      } else {
         return super.useOn(context);
      }
   }

   private boolean attachRope(Level level, BlockPos posA, BlockPos posB, boolean dropItem) {
      RopeStrandHolderBehavior ropeHolderA = getRopeHolder(level, posA);
      if (ropeHolderA == null) {
         return false;
      } else {
         RopeStrandHolderBehavior ropeHolderB = getRopeHolder(level, posB);
         if (ropeHolderB == null) {
            return false;
         } else {
            if (ropeHolderB.blockEntity instanceof RopeWinchBlockEntity && !(ropeHolderA.blockEntity instanceof RopeWinchBlockEntity)) {
               RopeStrandHolderBehavior temp = ropeHolderA;
               ropeHolderA = ropeHolderB;
               ropeHolderB = temp;
            }

            if (ropeHolderA.blockEntity instanceof RopeWinchBlockEntity && ropeHolderB.blockEntity instanceof RopeWinchBlockEntity) {
               return false;
            } else if (ropeHolderA.createRope(ropeHolderB, dropItem)) {
               level.playSound(null, posA, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.5F, 1.0F);
               level.playSound(null, posB, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.5F, 1.0F);
               return true;
            } else {
               return false;
            }
         }
      }
   }
}
