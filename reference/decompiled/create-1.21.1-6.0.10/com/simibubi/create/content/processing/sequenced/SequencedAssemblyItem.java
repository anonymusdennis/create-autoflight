package com.simibubi.create.content.processing.sequenced;

import com.simibubi.create.AllDataComponents;
import net.createmod.catnip.theme.Color;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

public class SequencedAssemblyItem extends Item {
   public SequencedAssemblyItem(Properties p_i48487_1_) {
      super(p_i48487_1_.stacksTo(1));
   }

   public float getProgress(ItemStack stack) {
      return !stack.has(AllDataComponents.SEQUENCED_ASSEMBLY)
         ? 0.0F
         : ((SequencedAssemblyRecipe.SequencedAssembly)stack.get(AllDataComponents.SEQUENCED_ASSEMBLY)).progress();
   }

   public boolean isBarVisible(ItemStack stack) {
      return true;
   }

   public int getBarWidth(ItemStack stack) {
      return Math.round(this.getProgress(stack) * 13.0F);
   }

   public int getBarColor(ItemStack stack) {
      return Color.mixColors(-16268, -12124192, this.getProgress(stack));
   }
}
