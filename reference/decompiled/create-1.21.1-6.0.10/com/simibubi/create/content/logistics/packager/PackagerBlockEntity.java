package com.simibubi.create.content.logistics.packager;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.crate.BottomlessItemHandler;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlock;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import com.simibubi.create.content.logistics.packagerLink.WiFiEffectPacket;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class PackagerBlockEntity extends SmartBlockEntity implements Clearable {
   public boolean redstonePowered;
   public int buttonCooldown;
   public String signBasedAddress;
   public InvManipulationBehaviour targetInventory;
   public ItemStack heldBox;
   public ItemStack previouslyUnwrapped;
   public List<BigItemStack> queuedExitingPackages;
   public final PackagerItemHandler inventory;
   public static final int CYCLE = 20;
   public int animationTicks;
   public boolean animationInward;
   public AbstractComputerBehaviour computerBehaviour;
   public Boolean hasCustomComputerAddress;
   public String customComputerAddress;
   private InventorySummary availableItems;
   private VersionedInventoryTrackerBehaviour invVersionTracker;
   private AdvancementBehaviour advancements;

   public PackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
      this.redstonePowered = state.getOptionalValue(PackagerBlock.POWERED).orElse(false);
      this.heldBox = ItemStack.EMPTY;
      this.previouslyUnwrapped = ItemStack.EMPTY;
      this.inventory = new PackagerItemHandler(this);
      this.animationTicks = 0;
      this.animationInward = true;
      this.queuedExitingPackages = new LinkedList<>();
      this.signBasedAddress = "";
      this.customComputerAddress = "";
      this.hasCustomComputerAddress = false;
      this.buttonCooldown = 0;
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.PACKAGER.get(), (be, context) -> be.inventory);
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(), (BlockEntityType)AllBlockEntityTypes.PACKAGER.get(), (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(
         this.targetInventory = new InvManipulationBehaviour(this, CapManipulationBehaviourBase.InterfaceProvider.oppositeOfBlockFacing())
            .withFilter(this::supportsBlockEntity)
      );
      behaviours.add(this.invVersionTracker = new VersionedInventoryTrackerBehaviour(this));
      behaviours.add(this.advancements = new AdvancementBehaviour(this, AllAdvancements.PACKAGER));
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
   }

   private boolean supportsBlockEntity(BlockEntity target) {
      return target != null && !(target instanceof PortableStorageInterfaceBlockEntity);
   }

   @Override
   public void initialize() {
      super.initialize();
      this.recheckIfLinksPresent();
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }

   @Override
   public void tick() {
      super.tick();
      if (this.buttonCooldown > 0) {
         this.buttonCooldown--;
      }

      if (this.animationTicks == 0) {
         this.previouslyUnwrapped = ItemStack.EMPTY;
         if (!this.level.isClientSide() && !this.queuedExitingPackages.isEmpty() && this.heldBox.isEmpty()) {
            BigItemStack entry = this.queuedExitingPackages.get(0);
            this.heldBox = entry.stack.copy();
            entry.count--;
            if (entry.count <= 0) {
               this.queuedExitingPackages.remove(0);
            }

            this.animationInward = false;
            this.animationTicks = 20;
            this.notifyUpdate();
         }
      } else {
         if (this.level.isClientSide) {
            if (this.animationTicks == 20 - (this.animationInward ? 5 : 1)) {
               AllSoundEvents.PACKAGER.playAt(this.level, this.worldPosition, 1.0F, 1.0F, true);
            }

            if (this.animationTicks == (this.animationInward ? 1 : 5)) {
               this.level.playLocalSound(this.worldPosition, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.25F, 0.75F, true);
            }
         }

         this.animationTicks--;
         if (this.animationTicks == 0 && !this.level.isClientSide()) {
            this.wakeTheFrogs();
            this.setChanged();
         }
      }
   }

   public void triggerStockCheck() {
      this.getAvailableItems();
   }

   public InventorySummary getAvailableItems() {
      if (this.availableItems != null && this.invVersionTracker.stillWaiting(this.targetInventory.getInventory())) {
         return this.availableItems;
      } else {
         InventorySummary availableItems = new InventorySummary();
         IItemHandler targetInv = this.targetInventory.getInventory();
         if (targetInv == null || targetInv instanceof PackagerItemHandler) {
            this.availableItems = availableItems;
            return availableItems;
         } else if (targetInv instanceof BottomlessItemHandler bih) {
            availableItems.add(bih.getStackInSlot(0), 1000000000);
            this.availableItems = availableItems;
            return availableItems;
         } else {
            for (int slot = 0; slot < targetInv.getSlots(); slot++) {
               availableItems.add(targetInv.getStackInSlot(slot));
            }

            this.invVersionTracker.awaitNewVersion(this.targetInventory.getInventory());
            this.submitNewArrivals(this.availableItems, availableItems);
            this.availableItems = availableItems;
            return availableItems;
         }
      }
   }

   private void submitNewArrivals(InventorySummary before, InventorySummary after) {
      if (before != null && !after.isEmpty()) {
         Set<RequestPromiseQueue> promiseQueues = new HashSet<>();

         for (Direction d : Iterate.directions) {
            if (this.level.isLoaded(this.worldPosition.relative(d))) {
               BlockState adjacentState = this.level.getBlockState(this.worldPosition.relative(d));
               if (AllBlocks.FACTORY_GAUGE.has(adjacentState)) {
                  if (FactoryPanelBlock.connectedDirection(adjacentState) != d) {
                     continue;
                  }

                  BlockEntity freqId = this.level.getBlockEntity(this.worldPosition.relative(d));
                  if (!(freqId instanceof FactoryPanelBlockEntity)) {
                     continue;
                  }

                  FactoryPanelBlockEntity fpbe = (FactoryPanelBlockEntity)freqId;
                  if (!fpbe.restocker) {
                     continue;
                  }

                  for (FactoryPanelBehaviour behaviour : fpbe.panels.values()) {
                     if (behaviour.isActive()) {
                        promiseQueues.add(behaviour.restockerPromises);
                     }
                  }
               }

               if (AllBlocks.STOCK_LINK.has(adjacentState) && PackagerLinkBlock.getConnectedDirection(adjacentState) == d) {
                  BlockEntity var20 = this.level.getBlockEntity(this.worldPosition.relative(d));
                  if (var20 instanceof PackagerLinkBlockEntity) {
                     PackagerLinkBlockEntity plbe = (PackagerLinkBlockEntity)var20;
                     UUID freqIdx = plbe.behaviour.freqId;
                     if (Create.LOGISTICS.hasQueuedPromises(freqIdx)) {
                        promiseQueues.add(Create.LOGISTICS.getQueuedPromises(freqIdx));
                     }
                  }
               }
            }
         }

         if (!promiseQueues.isEmpty()) {
            for (BigItemStack entry : after.getStacks()) {
               before.add(entry.stack, -entry.count);
            }

            for (RequestPromiseQueue queue : promiseQueues) {
               for (BigItemStack entry : before.getStacks()) {
                  if (entry.count < 0) {
                     queue.itemEnteredSystem(entry.stack, -entry.count);
                  }
               }
            }
         }
      }
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      if (!this.level.isClientSide()) {
         this.recheckIfLinksPresent();
         if (this.redstonePowered) {
            this.redstonePowered = this.getBlockState().getOptionalValue(PackagerBlock.POWERED).orElse(false);
            if (this.redstoneModeActive()) {
               this.updateSignAddress();
               this.attemptToSend(null);
            }
         }
      }
   }

   public void recheckIfLinksPresent() {
      if (!this.level.isClientSide()) {
         BlockState blockState = this.getBlockState();
         if (blockState.hasProperty(PackagerBlock.LINKED)) {
            boolean shouldBeLinked = this.getLinkPos() != null;
            boolean isLinked = (Boolean)blockState.getValue(PackagerBlock.LINKED);
            if (shouldBeLinked != isLinked) {
               this.level.setBlockAndUpdate(this.worldPosition, (BlockState)blockState.cycle(PackagerBlock.LINKED));
            }
         }
      }
   }

   public boolean redstoneModeActive() {
      return !this.getBlockState().getOptionalValue(PackagerBlock.LINKED).orElse(false);
   }

   private BlockPos getLinkPos() {
      for (Direction d : Iterate.directions) {
         BlockState adjacentState = this.level.getBlockState(this.worldPosition.relative(d));
         if (AllBlocks.STOCK_LINK.has(adjacentState) && PackagerLinkBlock.getConnectedDirection(adjacentState) == d) {
            return this.worldPosition.relative(d);
         }
      }

      return null;
   }

   public void flashLink() {
      for (Direction d : Iterate.directions) {
         BlockState adjacentState = this.level.getBlockState(this.worldPosition.relative(d));
         if (AllBlocks.STOCK_LINK.has(adjacentState) && PackagerLinkBlock.getConnectedDirection(adjacentState) == d) {
            WiFiEffectPacket.send(this.level, this.worldPosition.relative(d));
            return;
         }
      }
   }

   public boolean isTooBusyFor(LogisticallyLinkedBehaviour.RequestType type) {
      int queue = this.queuedExitingPackages.size();

      return queue >= switch (type) {
         case PLAYER -> 50;
         case REDSTONE -> 20;
         case RESTOCK -> 10;
      };
   }

   public void activate() {
      this.redstonePowered = true;
      this.setChanged();
      this.recheckIfLinksPresent();
      if (this.redstoneModeActive()) {
         this.updateSignAddress();
         this.attemptToSend(null);
         if (this.buttonCooldown <= 0) {
            this.buttonCooldown = 40;
         }
      }
   }

   public boolean unwrapBox(ItemStack box, boolean simulate) {
      if (this.animationTicks > 0) {
         return false;
      } else {
         Objects.requireNonNull(this.level);
         ItemStackHandler contents = PackageItem.getContents(box);
         List<ItemStack> items = ItemHelper.getNonEmptyStacks(contents);
         if (items.isEmpty()) {
            return true;
         } else {
            PackageOrderWithCrafts orderContext = PackageItem.getOrderContext(box);
            Direction facing = this.getBlockState().getOptionalValue(PackagerBlock.FACING).orElse(Direction.UP);
            BlockPos target = this.worldPosition.relative(facing.getOpposite());
            BlockState targetState = this.level.getBlockState(target);
            UnpackingHandler handler = UnpackingHandler.REGISTRY.get(targetState);
            UnpackingHandler toUse = handler != null ? handler : UnpackingHandler.DEFAULT;
            boolean unpacked = toUse.unpack(this.level, target, targetState, facing, items, orderContext, simulate);
            if (unpacked && !simulate) {
               this.computerBehaviour.prepareComputerEvent(new PackageEvent(box, "package_received"));
               this.previouslyUnwrapped = box;
               this.animationInward = true;
               this.animationTicks = 20;
               this.notifyUpdate();
            }

            return unpacked;
         }
      }
   }

   public void attemptToSend(List<PackagingRequest> queuedRequests) {
      if (queuedRequests != null || this.heldBox.isEmpty() && this.animationTicks == 0 && this.buttonCooldown <= 0) {
         IItemHandler targetInv = this.targetInventory.getInventory();
         if (targetInv != null && !(targetInv instanceof PackagerItemHandler)) {
            boolean anyItemPresent = false;
            ItemStackHandler extractedItems = new ItemStackHandler(9);
            ItemStack extractedPackageItem = ItemStack.EMPTY;
            PackagingRequest nextRequest = null;
            String fixedAddress = null;
            int fixedOrderId = 0;
            int linkIndexInOrder = 0;
            boolean finalLinkInOrder = false;
            int packageIndexAtLink = 0;
            boolean finalPackageAtLink = false;
            PackageOrderWithCrafts orderContext = null;
            boolean requestQueue = queuedRequests != null;
            if (requestQueue && !queuedRequests.isEmpty()) {
               nextRequest = queuedRequests.get(0);
               fixedAddress = nextRequest.address();
               fixedOrderId = nextRequest.orderId();
               linkIndexInOrder = nextRequest.linkIndex();
               finalLinkInOrder = nextRequest.finalLink().booleanValue();
               packageIndexAtLink = nextRequest.packageCounter().getAndIncrement();
               orderContext = nextRequest.context();
            }

            label151:
            for (int i = 0; i < 9; i++) {
               boolean continuePacking = true;

               while (continuePacking) {
                  continuePacking = false;

                  for (int slot = 0; slot < targetInv.getSlots(); slot++) {
                     int initialCount = requestQueue ? Math.min(64, nextRequest.getCount()) : 64;
                     ItemStack extracted = targetInv.extractItem(slot, initialCount, true);
                     if (!extracted.isEmpty() && (!requestQueue || ItemStack.isSameItemSameComponents(extracted, nextRequest.item()))) {
                        boolean bulky = !extracted.getItem().canFitInsideContainerItems();
                        if (!bulky || !anyItemPresent) {
                           anyItemPresent = true;
                           int leftovers = ItemHandlerHelper.insertItemStacked(extractedItems, extracted.copy(), false).getCount();
                           int transferred = extracted.getCount() - leftovers;
                           targetInv.extractItem(slot, transferred, false);
                           if (extracted.getItem() instanceof PackageItem) {
                              extractedPackageItem = extracted;
                           }

                           if (!requestQueue) {
                              if (bulky) {
                                 break label151;
                              }
                           } else {
                              nextRequest.subtract(transferred);
                              if (nextRequest.isEmpty()) {
                                 finalPackageAtLink = true;
                                 queuedRequests.remove(0);
                                 if (queuedRequests.isEmpty()) {
                                    break label151;
                                 }

                                 int previousCount = nextRequest.packageCounter().intValue();
                                 nextRequest = queuedRequests.get(0);
                                 if (!fixedAddress.equals(nextRequest.address()) || fixedOrderId != nextRequest.orderId()) {
                                    break label151;
                                 }

                                 nextRequest.packageCounter().setValue(previousCount);
                                 finalPackageAtLink = false;
                                 continuePacking = true;
                                 if (nextRequest.context() != null) {
                                    orderContext = nextRequest.context();
                                 }

                                 if (bulky) {
                                    break label151;
                                 }
                                 break;
                              }

                              if (bulky) {
                                 break label151;
                              }
                           }
                        }
                     }
                  }
               }
            }

            if (!anyItemPresent) {
               if (nextRequest != null) {
                  queuedRequests.remove(0);
               }
            } else {
               ItemStack createdBox = extractedPackageItem.isEmpty() ? PackageItem.containing(extractedItems) : extractedPackageItem.copy();
               this.computerBehaviour.prepareComputerEvent(new PackageEvent(createdBox, "package_created"));
               PackageItem.clearAddress(createdBox);
               if (fixedAddress != null) {
                  PackageItem.addAddress(createdBox, fixedAddress);
               }

               if (requestQueue) {
                  PackageItem.setOrder(createdBox, fixedOrderId, linkIndexInOrder, finalLinkInOrder, packageIndexAtLink, finalPackageAtLink, orderContext);
               }

               if (!requestQueue && !this.signBasedAddress.isBlank()) {
                  PackageItem.addAddress(createdBox, this.signBasedAddress);
               }

               BlockPos linkPos = this.getLinkPos();
               if (extractedPackageItem.isEmpty() && linkPos != null && this.level.getBlockEntity(linkPos) instanceof PackagerLinkBlockEntity plbe) {
                  plbe.behaviour.deductFromAccurateSummary(extractedItems);
               }

               if (this.heldBox.isEmpty() && this.animationTicks == 0) {
                  this.heldBox = createdBox;
                  this.animationInward = false;
                  this.animationTicks = 20;
                  this.advancements.awardPlayer(AllAdvancements.PACKAGER);
                  this.triggerStockCheck();
                  this.notifyUpdate();
               } else {
                  this.queuedExitingPackages.add(new BigItemStack(createdBox, 1));
               }
            }
         }
      }
   }

   public void updateSignAddress() {
      this.signBasedAddress = "";

      for (Direction side : Iterate.directions) {
         String address = this.getSign(side);
         if (address != null && !address.isBlank()) {
            this.signBasedAddress = address;
         }
      }

      if (this.computerBehaviour.hasAttachedComputer() && this.hasCustomComputerAddress) {
         this.signBasedAddress = this.customComputerAddress;
      } else {
         this.hasCustomComputerAddress = false;
      }
   }

   protected String getSign(Direction side) {
      if (!(this.level.getBlockEntity(this.worldPosition.relative(side)) instanceof SignBlockEntity sign)) {
         return null;
      } else {
         for (boolean front : Iterate.trueAndFalse) {
            SignText text = sign.getText(front);
            String address = "";

            for (Component component : text.getMessages(false)) {
               String string = component.getString();
               if (!string.isBlank()) {
                  address = address + string.trim() + " ";
               }
            }

            if (!address.isBlank()) {
               return address.trim();
            }
         }

         return null;
      }
   }

   protected void wakeTheFrogs() {
      if (this.level.getBlockEntity(this.worldPosition.relative(Direction.UP)) instanceof FrogportBlockEntity port) {
         port.tryPullingFromOwnAndAdjacentInventories();
      }
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.redstonePowered = compound.getBoolean("Active");
      this.animationInward = compound.getBoolean("AnimationInward");
      this.animationTicks = compound.getInt("AnimationTicks");
      this.signBasedAddress = compound.getString("SignAddress");
      this.customComputerAddress = compound.getString("ComputerAddress");
      this.hasCustomComputerAddress = compound.getBoolean("HasComputerAddress");
      this.heldBox = ItemStack.parseOptional(registries, compound.getCompound("HeldBox"));
      this.previouslyUnwrapped = ItemStack.parseOptional(registries, compound.getCompound("InsertedBox"));
      if (!clientPacket) {
         this.queuedExitingPackages = NBTHelper.readCompoundList(
            compound.getList("QueuedExitingPackages", 10), c -> (BigItemStack)CatnipCodecUtils.decode(BigItemStack.CODEC, registries, c).orElseThrow()
         );
         if (compound.contains("LastSummary")) {
            this.availableItems = (InventorySummary)CatnipCodecUtils.decodeOrNull(InventorySummary.CODEC, registries, compound.getCompound("LastSummary"));
         }
      }
   }

   @Override
   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.putBoolean("Active", this.redstonePowered);
      compound.putBoolean("AnimationInward", this.animationInward);
      compound.putInt("AnimationTicks", this.animationTicks);
      compound.putString("SignAddress", this.signBasedAddress);
      compound.putString("ComputerAddress", this.customComputerAddress);
      compound.putBoolean("HasComputerAddress", this.hasCustomComputerAddress);
      compound.put("HeldBox", this.heldBox.saveOptional(registries));
      compound.put("InsertedBox", this.previouslyUnwrapped.saveOptional(registries));
      if (!clientPacket) {
         compound.put("QueuedExitingPackages", NBTHelper.writeCompoundList(this.queuedExitingPackages, bis -> {
            Object patt0$temp = CatnipCodecUtils.encode(BigItemStack.CODEC, registries, bis).orElse(new CompoundTag());
            return patt0$temp instanceof CompoundTag ? (CompoundTag)patt0$temp : new CompoundTag();
         }));
         if (this.availableItems != null) {
            compound.put("LastSummary", (Tag)CatnipCodecUtils.encode(InventorySummary.CODEC, registries, this.availableItems).orElseThrow());
         }
      }
   }

   public void clearContent() {
      this.inventory.setStackInSlot(0, ItemStack.EMPTY);
      this.queuedExitingPackages.clear();
   }

   @Override
   public void destroy() {
      super.destroy();
      ItemHelper.dropContents(this.level, this.worldPosition, this.inventory);
      this.queuedExitingPackages
         .forEach(
            bigStack -> {
               for (int i = 0; i < bigStack.count; i++) {
                  Containers.dropItemStack(
                     this.level, (double)this.worldPosition.getX(), (double)this.worldPosition.getY(), (double)this.worldPosition.getZ(), bigStack.stack.copy()
                  );
               }
            }
         );
      this.queuedExitingPackages.clear();
   }

   public float getTrayOffset(float partialTicks) {
      float tickCycle = this.animationInward ? (float)this.animationTicks - partialTicks : (float)(this.animationTicks - 5) - partialTicks;
      float progress = Mth.clamp(tickCycle / 15.0F * 2.0F - 1.0F, -1.0F, 1.0F);
      progress = 1.0F - progress * progress;
      return progress * progress;
   }

   public ItemStack getRenderedBox() {
      if (this.animationInward) {
         return this.animationTicks <= 10 ? ItemStack.EMPTY : this.previouslyUnwrapped;
      } else {
         return this.animationTicks >= 10 ? ItemStack.EMPTY : this.heldBox;
      }
   }

   public boolean isTargetingSameInventory(@Nullable IdentifiedInventory inventory) {
      if (inventory == null) {
         return false;
      } else {
         IItemHandler targetHandler = this.targetInventory.getInventory();
         if (targetHandler == null) {
            return false;
         } else if (inventory.identifier() != null) {
            BlockFace face = this.targetInventory.getTarget().getOpposite();
            return inventory.identifier().contains(face);
         } else {
            return isSameInventoryFallback(targetHandler, inventory.handler());
         }
      }
   }

   private static boolean isSameInventoryFallback(IItemHandler first, IItemHandler second) {
      if (first == second) {
         return true;
      } else {
         for (int i = 0; i < second.getSlots(); i++) {
            ItemStack stackInSlot = second.getStackInSlot(i);
            if (!stackInSlot.isEmpty()) {
               for (int j = 0; j < first.getSlots(); j++) {
                  if (stackInSlot == first.getStackInSlot(j)) {
                     return true;
                  }
               }
               break;
            }
         }

         return false;
      }
   }
}
