package dev.simulated_team.simulated.content.blocks.auger_shaft;

import dev.simulated_team.simulated.content.blocks.auger_shaft.auger_groups.AugerDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public interface BlockHarvester {
   AugerDistributor simulated$getAssociatedDistributor();

   void simulated$setDistributor(AugerDistributor var1);

   default ItemStack depositItemStack(BlockPos fromPos, ItemStack stack) {
      if (stack.isEmpty()) {
         return stack;
      } else {
         AugerDistributor group = this.simulated$getAssociatedDistributor();
         return group != null ? group.distributeItem(stack, fromPos) : stack;
      }
   }
}
