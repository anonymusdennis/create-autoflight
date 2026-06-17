package com.simibubi.create.content.equipment.toolbox;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ToolboxSlot extends SlotItemHandler {
   private ToolboxMenu toolboxMenu;
   private boolean isVisible;

   public ToolboxSlot(ToolboxMenu menu, IItemHandler itemHandler, int index, int xPosition, int yPosition, boolean isVisible) {
      super(itemHandler, index, xPosition, yPosition);
      this.toolboxMenu = menu;
      this.isVisible = isVisible;
   }

   public boolean isActive() {
      return !this.toolboxMenu.renderPass && super.isActive() && this.isVisible;
   }

   public int getMaxStackSize(ItemStack stack) {
      ItemStack maxAdd = stack.copy();
      int maxInput = stack.getMaxStackSize();
      maxAdd.setCount(maxInput);
      IItemHandler handler = this.getItemHandler();
      ItemStack currentStack = handler.getStackInSlot(this.index);
      ItemStack remainder = handler.insertItem(this.index, maxAdd, true);
      int current = currentStack.getCount();
      int added = maxInput - remainder.getCount();
      return current + added;
   }
}
