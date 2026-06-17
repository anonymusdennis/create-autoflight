package dev.simulated_team.simulated.content.blocks.auger_shaft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public interface ItemReciever {
   ItemStack onRecieveItem(ItemStack var1, BlockPos var2);

   boolean removed();

   boolean isActive();
}
