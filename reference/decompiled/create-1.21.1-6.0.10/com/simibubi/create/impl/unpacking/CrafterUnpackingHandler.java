package com.simibubi.create.impl.unpacking;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public enum CrafterUnpackingHandler implements UnpackingHandler {
   INSTANCE;

   @Override
   public boolean unpack(
      Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext, boolean simulate
   ) {
      if (!PackageOrderWithCrafts.hasCraftingInformation(orderContext)) {
         return DEFAULT.unpack(level, pos, state, side, items, null, simulate);
      } else {
         List<BigItemStack> craftingContext = orderContext.getCraftingInformation();
         if (!(level.getBlockEntity(pos) instanceof MechanicalCrafterBlockEntity crafter)) {
            return false;
         } else {
            ConnectedInputHandler.ConnectedInput input = crafter.getInput();
            List<MechanicalCrafterBlockEntity.Inventory> inventories = input.getInventories(level, pos);
            if (inventories.isEmpty()) {
               return false;
            } else {
               int max = Math.min(inventories.size(), craftingContext.size());

               for (int i = 0; i < max; i++) {
                  BigItemStack targetStack = craftingContext.get(i);
                  if (!targetStack.stack.isEmpty()) {
                     MechanicalCrafterBlockEntity.Inventory inventory = inventories.get(i);
                     if (inventory.getStackInSlot(0).isEmpty()) {
                        for (ItemStack stack : items) {
                           if (ItemStack.isSameItemSameComponents(stack, targetStack.stack)) {
                              ItemStack toInsert = stack.copyWithCount(1);
                              if (inventory.insertItem(0, toInsert, simulate).isEmpty()) {
                                 stack.shrink(1);
                                 break;
                              }
                           }
                        }
                     }
                  }
               }

               for (ItemStack item : items) {
                  if (!item.isEmpty()) {
                     return false;
                  }
               }

               if (!simulate) {
                  crafter.checkCompletedRecipe(true);
               }

               return true;
            }
         }
      }
   }
}
