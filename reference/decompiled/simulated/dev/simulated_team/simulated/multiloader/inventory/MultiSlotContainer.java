package dev.simulated_team.simulated.multiloader.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MultiSlotContainer implements AbstractContainer {
   private final List<ContainerSlot> inventory;
   private final Set<ContainerSlot> populatedSlots;
   private final Map<Item, Set<ContainerSlot>> filteredSlots;
   public int maxStackSize;
   public int storedItemCount;

   public MultiSlotContainer(int size) {
      this(size, 64);
   }

   public MultiSlotContainer(int size, int maxStackSize) {
      this.maxStackSize = maxStackSize;
      this.inventory = new ArrayList<>(size);
      this.populatedSlots = new HashSet<>();
      this.filteredSlots = new HashMap<>();

      for (int i = 0; i < size; i++) {
         this.inventory.add(i, ContainerSlot.of(i, ItemStack.EMPTY, this));
      }

      this.maxStackSize = maxStackSize;
   }

   @Override
   public int insertGeneral(ItemInfoWrapper info, int amountToInsert, boolean simulate) {
      int inserted = 0;

      for (ContainerSlot slot : this.getInsertableSlotsFor(info.type(), true)) {
         inserted += this.commonInsert(info, slot, amountToInsert - inserted, simulate);
         if (inserted >= amountToInsert) {
            break;
         }
      }

      return inserted;
   }

   @Override
   public ItemStack insertSlot(ItemStack stack, int slot, boolean simulate) {
      int amountInserted = this.commonInsert(ItemInfoWrapper.generateFromStack(stack), this.inventory.get(slot), stack.getCount(), simulate);
      if (amountInserted > 0) {
         ItemStack copyIncoming = stack.copy();
         copyIncoming.shrink(amountInserted);
         return copyIncoming;
      } else {
         return stack;
      }
   }

   @Override
   public int extractGeneral(ItemInfoWrapper info, int amountToExtract, boolean simulate) {
      Set<ContainerSlot> populatedSlots = this.getFilteredSlots(info.type());
      if (populatedSlots.isEmpty()) {
         return 0;
      } else {
         int extracted = 0;

         for (ContainerSlot slot : populatedSlots) {
            extracted += this.commonExtract(info, slot, amountToExtract - extracted, simulate);
            if (extracted >= amountToExtract) {
               break;
            }
         }

         return extracted;
      }
   }

   @Override
   public ItemStack extractSlot(int index, int amountToExtract, boolean simulate) {
      ContainerSlot slot = this.getSlot(index);
      if (slot.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack toExtract = slot.getStack().copy();
         long extracted = (long)this.commonExtract(ItemInfoWrapper.generateFromStack(toExtract), slot, amountToExtract, simulate);
         if (extracted > 0L) {
            toExtract.setCount((int)extracted);
            return toExtract;
         } else {
            return ItemStack.EMPTY;
         }
      }
   }

   public Set<ItemStack> extractAny(int amountToExtract, boolean simulated) {
      HashSet<ItemStack> extracted = new HashSet<>();

      for (ContainerSlot slot : this.populatedSlots) {
         if (amountToExtract <= 0) {
            break;
         }

         int amountExtracted = slot.extractStack(null, amountToExtract, simulated);
         if (amountExtracted > 0) {
            amountToExtract -= amountExtracted;
            extracted.add(new ItemStack(slot.getType(), amountExtracted));
         }
      }

      return extracted;
   }

   public ItemStack extractSingle(boolean entireStack, boolean simulated) {
      Optional<ContainerSlot> first = this.populatedSlots.stream().findFirst();
      if (first.isPresent()) {
         ContainerSlot slot = first.get();
         Item beforeType = slot.getType();
         long extracted = (long)slot.extractStack(null, entireStack ? slot.getStack().getCount() : 1, simulated);
         if (extracted > 0L) {
            return new ItemStack(beforeType, (int)extracted);
         }
      }

      return ItemStack.EMPTY;
   }

   public void shiftSlots(int shiftBy, BiFunction<ContainerSlot, Integer, Boolean> onEnd) {
      if (shiftBy != 0) {
         List<MultiSlotContainer.SlotAndItemHolder> holders = new ArrayList<>();
         int direction = Mth.sign((double)shiftBy);

         for (ContainerSlot slot : this.inventory) {
            if (!slot.isEmpty()) {
               int newIndex = slot.getIndex() + shiftBy;
               if (newIndex > this.getContainerSize() - 1 || newIndex < 0) {
                  if (onEnd.apply(slot, direction)) {
                     continue;
                  }

                  newIndex = direction == 1 ? this.getContainerSize() - 1 : 0;
               }

               holders.add(new MultiSlotContainer.SlotAndItemHolder(slot.getIndex(), newIndex, slot.getStack()));
            }
         }

         if (!holders.isEmpty() && holders.size() != this.getContainerSize()) {
            this.processHolders(holders, direction);
         }
      }
   }

   private void processHolders(List<MultiSlotContainer.SlotAndItemHolder> holders, int direction) {
      if (direction == 1) {
         for (int i = holders.size() - 1; i >= 0; i--) {
            this.shiftSlot(holders.get(i));
         }
      } else {
         for (MultiSlotContainer.SlotAndItemHolder holder : holders) {
            this.shiftSlot(holder);
         }
      }
   }

   private void shiftSlot(MultiSlotContainer.SlotAndItemHolder holder) {
      ContainerSlot next = this.getSlot(holder.nextIndex());
      if (next.isEmpty()) {
         next.setStack(holder.stack());
         this.getSlot(holder.currentIndex()).setStack(ItemStack.EMPTY);
      }
   }

   public static void setOtherAndEmptyCurrent(ContainerSlot current, ContainerSlot other) {
      other.setStack(current.getStack());
      current.setStack(ItemStack.EMPTY);
   }

   @NotNull
   public Set<ContainerSlot> getFilteredSlots(@Nullable Item type) {
      if (type == null) {
         return this.populatedSlots;
      } else {
         return this.filteredSlots.containsKey(type) ? new HashSet<>(this.filteredSlots.get(type)) : new HashSet<>();
      }
   }

   public Set<ContainerSlot> getInsertableSlotsFor(Item type, boolean shouldIncludeEmpty) {
      Set<ContainerSlot> filteredSlots = this.getFilteredSlots(type);
      if (shouldIncludeEmpty) {
         filteredSlots.addAll(this.getFilteredSlots(Items.AIR));
      }

      return filteredSlots;
   }

   @Override
   public int getContainerSize() {
      return this.inventory.size();
   }

   @Override
   public boolean isEmpty() {
      return this.populatedSlots.isEmpty();
   }

   @NotNull
   @Override
   public ItemStack getItem(int slot) {
      return this.inventory.get(slot).getStack();
   }

   public ContainerSlot getSlot(int slot) {
      return this.inventory.get(slot);
   }

   @Override
   public void setItem(int slot, @NotNull ItemStack stack) {
      this.inventory.get(slot).setStack(stack);
   }

   public void clearAndDropContents(Level level, BlockPos dropPos) {
      for (ContainerSlot slot : this.populatedSlots) {
         Containers.dropItemStack(level, (double)dropPos.getX(), (double)dropPos.getY(), (double)dropPos.getZ(), slot.getStack());
      }

      this.clearContent();
   }

   @Override
   public void clearContent() {
      Collections.fill(this.inventory, ContainerSlot.EMPTY);
      this.populatedSlots.clear();
      this.filteredSlots.clear();
      this.setChanged();
   }

   @Override
   public void onStackItemChange(ContainerSlot slot, ItemStack oldSlotStack, ItemStack newSlotStack) {
      int oldcount = oldSlotStack.getCount();
      int newCount = newSlotStack.getCount();
      this.storedItemCount += newCount - oldcount;
      if (!ItemStack.isSameItem(oldSlotStack, newSlotStack)) {
         Item newItem = newSlotStack.getItem();
         Item oldItem = oldSlotStack.getItem();
         this.filteredSlots.computeIfAbsent(newItem, $ -> new HashSet<>()).add(slot);
         if (this.filteredSlots.containsKey(oldItem)) {
            Set<ContainerSlot> oldFilteredSlot = this.filteredSlots.get(oldItem);
            oldFilteredSlot.remove(slot);
            if (oldFilteredSlot.isEmpty()) {
               this.filteredSlots.remove(oldItem);
            }
         }

         if (newSlotStack.isEmpty()) {
            this.populatedSlots.remove(slot);
         } else if (oldSlotStack.isEmpty() != newSlotStack.isEmpty()) {
            this.populatedSlots.add(slot);
         }
      }
   }

   @Override
   public CompoundTag write(Provider provider) {
      CompoundTag invCompound = new CompoundTag();
      invCompound.putInt("Stored Count", this.storedItemCount);
      ListTag inv = new ListTag();

      for (ContainerSlot slot : this.inventory) {
         inv.add(slot.write(provider));
      }

      invCompound.put("Items", inv);
      return invCompound;
   }

   @Override
   public void read(Provider provider, CompoundTag nbt) {
      this.storedItemCount = nbt.getInt("Stored Count");

      for (Tag tag : nbt.getList("Items", 10)) {
         CompoundTag itemTag = (CompoundTag)tag;
         ContainerSlot slot = this.inventory.get(itemTag.getInt("index"));
         slot.read(provider, itemTag);
         this.populatedSlots.add(slot);
      }
   }

   @Override
   public void populateFields(ContainerSlot slot) {
      this.filteredSlots.computeIfAbsent(slot.getType(), $ -> new HashSet<>()).add(slot);
      if (!slot.getStack().isEmpty()) {
         this.populatedSlots.add(slot);
      }
   }

   @Override
   public int getMaxStackSize() {
      return this.maxStackSize;
   }

   @Override
   public List<ContainerSlot> getInventoryAsList() {
      return this.inventory;
   }

   @Override
   public Set<ContainerSlot> getPopulatedSlots() {
      return this.populatedSlots;
   }

   public float getFillLevel() {
      return (float)(this.getContainerSize() * this.getMaxStackSize()) / (float)this.storedItemCount;
   }

   public boolean isFull() {
      return this.getFillLevel() == 1.0F;
   }

   public ContainerSlot getFirst() {
      return this.getSlot(0);
   }

   public ContainerSlot getLast() {
      return this.getSlot(this.getContainerSize() - 1);
   }

   public static record SlotAndItemHolder(int currentIndex, int nextIndex, ItemStack stack) {
   }
}
