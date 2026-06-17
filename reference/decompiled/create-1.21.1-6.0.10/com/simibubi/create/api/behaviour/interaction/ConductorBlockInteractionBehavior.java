package com.simibubi.create.api.behaviour.interaction;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleItem;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.function.Consumer;
import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public abstract class ConductorBlockInteractionBehavior extends MovingInteractionBehaviour {
   public abstract boolean isValidConductor(BlockState var1);

   protected void onScheduleUpdate(boolean hasSchedule, BlockState currentBlockState, Consumer<BlockState> blockStateSetter) {
   }

   @Override
   public final boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
      ItemStack itemInHand = player.getItemInHand(activeHand);
      if (contraptionEntity instanceof CarriageContraptionEntity carriageEntity) {
         if (activeHand == InteractionHand.OFF_HAND) {
            return false;
         } else if (carriageEntity.getContraption() instanceof CarriageContraption carriageContraption) {
            StructureBlockInfo info = carriageContraption.getBlocks().get(localPos);
            if (info != null && this.isValidConductor(info.state())) {
               Direction assemblyDirection = carriageContraption.getAssemblyDirection();

               for (Direction direction : Iterate.directionsInAxis(assemblyDirection.getAxis())) {
                  if (carriageContraption.inControl(localPos, direction)) {
                     Train train = carriageEntity.getCarriage().train;
                     if (train == null) {
                        return false;
                     }

                     if (player.level().isClientSide) {
                        return true;
                     }

                     if (train.runtime.getSchedule() != null) {
                        if (train.runtime.paused && !train.runtime.completed) {
                           train.runtime.paused = false;
                           AllSoundEvents.CONFIRM.playOnServer(player.level(), player.blockPosition(), 1.0F, 1.0F);
                           player.displayClientMessage(CreateLang.translateDirect("schedule.continued"), true);
                           return true;
                        }

                        if (!itemInHand.isEmpty()) {
                           AllSoundEvents.DENY.playOnServer(player.level(), player.blockPosition(), 1.0F, 1.0F);
                           player.displayClientMessage(CreateLang.translateDirect("schedule.remove_with_empty_hand"), true);
                           return true;
                        }

                        AllSoundEvents.playItemPickup(player);
                        player.displayClientMessage(
                           CreateLang.translateDirect(train.runtime.isAutoSchedule ? "schedule.auto_removed_from_train" : "schedule.removed_from_train"), true
                        );
                        player.setItemInHand(activeHand, train.runtime.returnSchedule(player.registryAccess()));
                        this.onScheduleUpdate(false, info.state(), newBlockState -> this.setBlockState(localPos, contraptionEntity, newBlockState));
                        return true;
                     }

                     if (!AllItems.SCHEDULE.isIn(itemInHand)) {
                        return true;
                     }

                     Schedule schedule = ScheduleItem.getSchedule(player.registryAccess(), itemInHand);
                     if (schedule == null) {
                        return false;
                     }

                     if (schedule.entries.isEmpty()) {
                        AllSoundEvents.DENY.playOnServer(player.level(), player.blockPosition(), 1.0F, 1.0F);
                        player.displayClientMessage(CreateLang.translateDirect("schedule.no_stops"), true);
                        return true;
                     }

                     this.onScheduleUpdate(true, info.state(), newBlockState -> this.setBlockState(localPos, contraptionEntity, newBlockState));
                     train.runtime.setSchedule(schedule, false);
                     AllAdvancements.CONDUCTOR.awardTo(player);
                     AllSoundEvents.CONFIRM.playOnServer(player.level(), player.blockPosition(), 1.0F, 1.0F);
                     player.displayClientMessage(CreateLang.translateDirect("schedule.applied_to_train").withStyle(ChatFormatting.GREEN), true);
                     itemInHand.shrink(1);
                     player.setItemInHand(activeHand, itemInHand.isEmpty() ? ItemStack.EMPTY : itemInHand);
                     return true;
                  }
               }

               player.displayClientMessage(CreateLang.translateDirect("schedule.non_controlling_seat"), true);
               AllSoundEvents.DENY.playOnServer(player.level(), player.blockPosition(), 1.0F, 1.0F);
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void setBlockState(BlockPos localPos, AbstractContraptionEntity contraption, BlockState newState) {
      StructureBlockInfo info = contraption.getContraption().getBlocks().get(localPos);
      if (info != null) {
         this.setContraptionBlockData(contraption, localPos, new StructureBlockInfo(info.pos(), newState, info.nbt()));
      }
   }

   public static class BlazeBurner extends ConductorBlockInteractionBehavior {
      @Override
      public boolean isValidConductor(BlockState state) {
         return state.getValue(BlazeBurnerBlock.HEAT_LEVEL) != BlazeBurnerBlock.HeatLevel.NONE;
      }
   }
}
