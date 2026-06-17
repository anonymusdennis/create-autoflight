package dev.simulated_team.simulated.multiloader.inventory;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ContainerSlot implements NBTSerializable {
   public static final ContainerSlot EMPTY = new ContainerSlot();
   private final int index;
   private final AbstractContainer parent;
   private Item type;
   private ItemStack stack;

   public ContainerSlot() {
      this.index = -1;
      this.parent = null;
      this.type = ItemStack.EMPTY.getItem();
      this.stack = ItemStack.EMPTY;
   }

   public ContainerSlot(int slot, ItemStack stack, Item type, AbstractContainer parent) {
      this.index = slot;
      this.stack = stack;
      this.type = type;
      this.parent = parent;
      parent.populateFields(this);
   }

   public static ContainerSlot of(int slot, Item type, AbstractContainer parent) {
      return of(slot, new ItemStack(type), parent);
   }

   public static ContainerSlot of(int slot, ItemStack stack, AbstractContainer parent) {
      return new ContainerSlot(slot, stack, stack.getItem(), parent);
   }

   public int insertStack(ItemInfoWrapper info, int maxAmount, boolean simulate) {
      if (this.canInsert(info) && (this.getStack().isEmpty() || this.getStack().getItem() == info.type())) {
         int insertedAmount = Math.min(Math.min(this.parent.getMaxStackSize(), info.type().getDefaultMaxStackSize()) - this.getStack().getCount(), maxAmount);
         if (!simulate && insertedAmount > 0) {
            ItemStack newstack = this.getStack().copy();
            if (newstack.isEmpty()) {
               newstack = ItemInfoWrapper.generateFromInfo(info);
               newstack.setCount(insertedAmount);
            } else {
               newstack.grow(insertedAmount);
            }

            this.setStack(newstack);
         }

         return insertedAmount;
      } else {
         return 0;
      }
   }

   public int extractStack(ItemInfoWrapper info, int maxAmount, boolean simulate) {
      if (this.canExtract() && !this.getStack().isEmpty()) {
         if (info != null && this.getStack().getItem() != info.type()) {
            return 0;
         } else {
            int extracted = Math.min(this.getStack().getCount(), maxAmount);
            if (!simulate && extracted > 0) {
               ItemStack newStack = this.getStack().copy();
               newStack.shrink(extracted);
               this.setStack(newStack);
            }

            return extracted;
         }
      } else {
         return 0;
      }
   }

   public void shrink(long amountToShrink) {
      ItemStack copied = this.getStack().copy();
      if ((long)copied.getCount() - amountToShrink <= 0L) {
         copied = ItemStack.EMPTY;
      } else {
         copied.shrink((int)amountToShrink);
      }

      this.setStack(copied);
   }

   public boolean canInsert(ItemInfoWrapper info) {
      return this.parent.canInsertItem(info, this);
   }

   public boolean canExtract() {
      return this.parent.canExtractFromSlot(this);
   }

   public boolean isEmpty() {
      return this == EMPTY || this.getStack().isEmpty();
   }

   public void setStack(ItemStack stack) {
      this.parent.onStackItemChange(this, this.getStack(), stack);
      this.stack = stack;
      this.type = stack.getItem();
      this.parent.setChanged();
   }

   public int getIndex() {
      return this.index;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public Item getType() {
      return this.type;
   }

   public AbstractContainer getParent() {
      return this.parent;
   }

   public void clear() {
      this.stack = ItemStack.EMPTY;
      this.type = Items.AIR;
   }

   @Override
   public CompoundTag write(Provider provider) {
      CompoundTag slotTag = new CompoundTag();
      slotTag.putInt("index", this.getIndex());
      if (!this.getStack().isEmpty()) {
         slotTag.put("item", this.getStack().save(provider));
      }

      return slotTag;
   }

   @Override
   public void read(Provider provider, CompoundTag nbt) {
      this.stack = ItemStack.EMPTY;
      if (nbt.contains("item")) {
         this.stack = ItemStack.parseOptional(provider, nbt.getCompound("item"));
      }

      this.type = this.stack.getItem();
   }
}
