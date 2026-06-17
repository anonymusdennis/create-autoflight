package com.simibubi.create.content.logistics.packager.repackager;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import com.simibubi.create.compat.computercraft.events.RepackageEvent;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.crate.BottomlessItemHandler;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerItemHandler;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;

public class RepackagerBlockEntity extends PackagerBlockEntity {
   public PackageRepackageHelper repackageHelper = new PackageRepackageHelper();

   public RepackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
   }

   @Override
   public boolean unwrapBox(ItemStack box, boolean simulate) {
      if (this.animationTicks > 0) {
         return false;
      } else {
         IItemHandler targetInv = this.targetInventory.getInventory();
         if (targetInv != null && !(targetInv instanceof PackagerItemHandler)) {
            boolean targetIsCreativeCrate = targetInv instanceof BottomlessItemHandler;
            boolean anySpace = false;

            for (int slot = 0; slot < targetInv.getSlots(); slot++) {
               ItemStack remainder = targetInv.insertItem(slot, box, simulate);
               if (remainder.isEmpty()) {
                  anySpace = true;
                  break;
               }
            }

            if (!targetIsCreativeCrate && !anySpace) {
               return false;
            } else if (simulate) {
               return true;
            } else {
               this.computerBehaviour.prepareComputerEvent(new PackageEvent(box, "package_received"));
               this.previouslyUnwrapped = box;
               this.animationInward = true;
               this.animationTicks = 20;
               this.notifyUpdate();
               return true;
            }
         } else {
            return false;
         }
      }
   }

   @Override
   public void recheckIfLinksPresent() {
   }

   @Override
   public boolean redstoneModeActive() {
      return true;
   }

   @Override
   public void attemptToSend(List<PackagingRequest> queuedRequests) {
      if (this.heldBox.isEmpty() && this.animationTicks == 0 && this.buttonCooldown <= 0) {
         if (this.queuedExitingPackages.isEmpty()) {
            IItemHandler targetInv = this.targetInventory.getInventory();
            if (targetInv != null && !(targetInv instanceof PackagerItemHandler)) {
               this.attemptToRepackage(targetInv);
               if (!this.heldBox.isEmpty()) {
                  this.updateSignAddress();
                  if (!this.signBasedAddress.isBlank()) {
                     PackageItem.addAddress(this.heldBox, this.signBasedAddress);
                  }
               }
            }
         }
      }
   }

   protected void attemptToRepackage(IItemHandler targetInv) {
      this.repackageHelper.clear();
      int completedOrderId = -1;

      for (int slot = 0; slot < targetInv.getSlots(); slot++) {
         ItemStack extracted = targetInv.extractItem(slot, 1, true);
         if (!extracted.isEmpty() && PackageItem.isPackage(extracted)) {
            if (!this.repackageHelper.isFragmented(extracted)) {
               targetInv.extractItem(slot, 1, false);
               this.heldBox = extracted.copy();
               this.animationInward = false;
               this.animationTicks = 20;
               this.notifyUpdate();
               return;
            }

            completedOrderId = this.repackageHelper.addPackageFragment(extracted);
            if (completedOrderId != -1) {
               break;
            }
         }
      }

      if (completedOrderId != -1) {
         List<BigItemStack> boxesToExport = this.repackageHelper.repack(completedOrderId, this.level.getRandom());

         for (int slotx = 0; slotx < targetInv.getSlots(); slotx++) {
            ItemStack extracted = targetInv.extractItem(slotx, 1, true);
            if (!extracted.isEmpty() && PackageItem.isPackage(extracted) && PackageItem.getOrderId(extracted) == completedOrderId) {
               targetInv.extractItem(slotx, 1, false);
            }
         }

         if (!boxesToExport.isEmpty()) {
            if (this.computerBehaviour.hasAttachedComputer()) {
               for (BigItemStack box : boxesToExport) {
                  this.computerBehaviour.prepareComputerEvent(new RepackageEvent(box.stack, box.count));
               }
            }

            this.queuedExitingPackages.addAll(boxesToExport);
            this.notifyUpdate();
         }
      }
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.REPACKAGER.get(), (be, context) -> be.inventory);
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(), (BlockEntityType)AllBlockEntityTypes.REPACKAGER.get(), (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }
}
