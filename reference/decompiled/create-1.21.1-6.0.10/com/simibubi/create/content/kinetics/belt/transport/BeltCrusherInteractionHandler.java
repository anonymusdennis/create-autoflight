package com.simibubi.create.content.kinetics.belt.transport;

import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class BeltCrusherInteractionHandler {
   public static boolean checkForCrushers(BeltInventory beltInventory, TransportedItemStack currentItem, float nextOffset) {
      boolean beltMovementPositive = beltInventory.beltMovementPositive;
      int firstUpcomingSegment = (int)Math.floor((double)currentItem.beltPosition);
      int step = beltMovementPositive ? 1 : -1;
      firstUpcomingSegment = Mth.clamp(firstUpcomingSegment, 0, beltInventory.belt.beltLength - 1);

      for (int segment = firstUpcomingSegment; beltMovementPositive ? (float)segment <= nextOffset : (float)(segment + 1) >= nextOffset; segment += step) {
         BlockPos crusherPos = BeltHelper.getPositionForOffset(beltInventory.belt, segment).above();
         Level world = beltInventory.belt.getLevel();
         BlockState crusherState = world.getBlockState(crusherPos);
         if (crusherState.getBlock() instanceof CrushingWheelControllerBlock) {
            Direction crusherFacing = (Direction)crusherState.getValue(CrushingWheelControllerBlock.FACING);
            Direction movementFacing = beltInventory.belt.getMovementFacing();
            if (crusherFacing == movementFacing) {
               float crusherEntry = (float)segment + 0.5F;
               crusherEntry += 0.399F * (float)(beltMovementPositive ? -1 : 1);
               float postCrusherEntry = crusherEntry + 0.799F * (float)(!beltMovementPositive ? -1 : 1);
               boolean hasCrossed = nextOffset > crusherEntry && nextOffset < postCrusherEntry && beltMovementPositive
                  || nextOffset < crusherEntry && nextOffset > postCrusherEntry && !beltMovementPositive;
               if (!hasCrossed) {
                  return false;
               }

               currentItem.beltPosition = crusherEntry;
               if (world.getBlockEntity(crusherPos) instanceof CrushingWheelControllerBlockEntity crusherBE) {
                  ItemStack toInsert = currentItem.stack.copy();
                  ItemStack remainder = ItemHandlerHelper.insertItemStacked(crusherBE.inventory, toInsert, false);
                  if (ItemStack.matches(toInsert, remainder)) {
                     return true;
                  }

                  int notFilled = currentItem.stack.getCount() - toInsert.getCount();
                  if (!remainder.isEmpty()) {
                     remainder.grow(notFilled);
                  } else if (notFilled > 0) {
                     remainder = currentItem.stack.copyWithCount(notFilled);
                  }

                  currentItem.stack = remainder;
                  beltInventory.belt.notifyUpdate();
                  return true;
               }

               return true;
            }
         }
      }

      return false;
   }
}
