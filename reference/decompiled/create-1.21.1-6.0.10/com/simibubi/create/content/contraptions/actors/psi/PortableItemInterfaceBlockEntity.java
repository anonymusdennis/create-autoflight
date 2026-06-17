package com.simibubi.create.content.contraptions.actors.psi;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.item.ItemHandlerWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

public class PortableItemInterfaceBlockEntity extends PortableStorageInterfaceBlockEntity {
   protected IItemHandlerModifiable capability = this.createEmptyHandler();

   public PortableItemInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.PORTABLE_STORAGE_INTERFACE.get(), (be, context) -> be.capability);
   }

   @Override
   public void startTransferringTo(Contraption contraption, float distance) {
      this.capability = new PortableItemInterfaceBlockEntity.InterfaceItemHandler(contraption.getStorage().getAllItems());
      this.invalidateCapability();
      if (this.level != null && !this.level.isClientSide) {
         this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
      }

      super.startTransferringTo(contraption, distance);
   }

   @Override
   protected void stopTransferring() {
      this.capability = this.createEmptyHandler();
      this.invalidateCapability();
      if (this.level != null && !this.level.isClientSide) {
         this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
      }

      super.stopTransferring();
   }

   private IItemHandlerModifiable createEmptyHandler() {
      return new PortableItemInterfaceBlockEntity.InterfaceItemHandler(new ItemStackHandler(0));
   }

   @Override
   protected void invalidateCapability() {
      this.invalidateCapabilities();
   }

   class InterfaceItemHandler extends ItemHandlerWrapper {
      public InterfaceItemHandler(IItemHandlerModifiable wrapped) {
         super(wrapped);
      }

      @Override
      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         if (!PortableItemInterfaceBlockEntity.this.canTransfer()) {
            return ItemStack.EMPTY;
         } else {
            ItemStack extractItem = super.extractItem(slot, amount, simulate);
            if (!simulate && !extractItem.isEmpty()) {
               PortableItemInterfaceBlockEntity.this.onContentTransferred();
            }

            return extractItem;
         }
      }

      @Override
      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         if (!PortableItemInterfaceBlockEntity.this.canTransfer()) {
            return stack;
         } else {
            ItemStack insertItem = super.insertItem(slot, stack, simulate);
            if (!simulate && !ItemStack.matches(insertItem, stack)) {
               PortableItemInterfaceBlockEntity.this.onContentTransferred();
            }

            return insertItem;
         }
      }
   }
}
