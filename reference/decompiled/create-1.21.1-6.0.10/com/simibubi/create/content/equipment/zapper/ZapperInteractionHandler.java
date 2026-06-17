package com.simibubi.create.content.equipment.zapper;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.utility.BlockHelper;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;

@EventBusSubscriber
public class ZapperInteractionHandler {
   @SubscribeEvent
   public static void leftClickingBlocksWithTheZapperSelectsTheBlock(LeftClickBlock event) {
      if (!event.getLevel().isClientSide) {
         ItemStack heldItem = event.getEntity().getMainHandItem();
         if (heldItem.getItem() instanceof ZapperItem && trySelect(heldItem, event.getEntity())) {
            event.setCanceled(true);
         }
      }
   }

   public static boolean trySelect(ItemStack stack, Player player) {
      if (player.isShiftKeyDown()) {
         return false;
      } else {
         Vec3 start = player.position().add(0.0, (double)player.getEyeHeight(), 0.0);
         Vec3 range = player.getLookAngle().scale((double)getRange(stack));
         BlockHitResult raytrace = player.level().clip(new ClipContext(start, start.add(range), Block.OUTLINE, Fluid.NONE, player));
         BlockPos pos = raytrace.getBlockPos();
         if (pos == null) {
            return false;
         } else {
            player.level().destroyBlockProgress(player.getId(), pos, -1);
            BlockState newState = player.level().getBlockState(pos);
            if (BlockHelper.getRequiredItem(newState).isEmpty()) {
               return false;
            } else if (newState.hasBlockEntity() && !AllTags.AllBlockTags.SAFE_NBT.matches(newState)) {
               return false;
            } else if (newState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
               return false;
            } else if (newState.hasProperty(BlockStateProperties.ATTACHED)) {
               return false;
            } else if (newState.hasProperty(BlockStateProperties.HANGING)) {
               return false;
            } else if (newState.hasProperty(BlockStateProperties.BED_PART)) {
               return false;
            } else {
               if (newState.hasProperty(BlockStateProperties.STAIRS_SHAPE)) {
                  newState = (BlockState)newState.setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.STRAIGHT);
               }

               if (newState.hasProperty(BlockStateProperties.PERSISTENT)) {
                  newState = (BlockState)newState.setValue(BlockStateProperties.PERSISTENT, true);
               }

               if (newState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                  newState = (BlockState)newState.setValue(BlockStateProperties.WATERLOGGED, false);
               }

               CompoundTag data = null;
               BlockEntity blockEntity = player.level().getBlockEntity(pos);
               if (blockEntity != null) {
                  data = blockEntity.saveWithFullMetadata(player.registryAccess());
                  data.remove("x");
                  data.remove("y");
                  data.remove("z");
                  data.remove("id");
               }

               if (stack.has(AllDataComponents.SHAPER_BLOCK_USED)
                  && stack.get(AllDataComponents.SHAPER_BLOCK_USED) == newState
                  && Objects.equals(data, stack.get(AllDataComponents.SHAPER_BLOCK_DATA))) {
                  return false;
               } else {
                  stack.set(AllDataComponents.SHAPER_BLOCK_USED, newState);
                  if (data == null) {
                     stack.remove(AllDataComponents.SHAPER_BLOCK_DATA);
                  } else {
                     stack.set(AllDataComponents.SHAPER_BLOCK_DATA, data);
                  }

                  AllSoundEvents.CONFIRM.playOnServer(player.level(), player.blockPosition());
                  return true;
               }
            }
         }
      }
   }

   public static int getRange(ItemStack stack) {
      return stack.getItem() instanceof ZapperItem ? ((ZapperItem)stack.getItem()).getZappingRange(stack) : 0;
   }
}
