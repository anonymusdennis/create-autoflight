package com.simibubi.create.content.logistics.packagePort.frogport;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerItemHandler;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.TooltipHelper;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class FrogportBlockEntity extends PackagePortBlockEntity implements IHaveHoveringInformation {
   public ItemStack animatedPackage;
   public LerpedFloat manualOpenAnimationProgress;
   public LerpedFloat animationProgress;
   public LerpedFloat anticipationProgress;
   public boolean currentlyDepositing;
   public boolean goggles;
   public boolean sendAnticipate;
   public float passiveYaw;
   private boolean failedLastExport;
   private FrogportSounds sounds = new FrogportSounds();
   private ItemStack deferAnimationStart;
   private boolean deferAnimationInward;
   private AdvancementBehaviour advancements;
   public AbstractComputerBehaviour computerBehaviour;

   public FrogportBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.animationProgress = LerpedFloat.linear();
      this.anticipationProgress = LerpedFloat.linear();
      this.manualOpenAnimationProgress = LerpedFloat.linear().startWithValue(0.0).chase(0.0, 0.35, Chaser.LINEAR);
      this.goggles = false;
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.PACKAGE_FROGPORT.get(), (be, context) -> be.itemHandler);
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(),
            (BlockEntityType)AllBlockEntityTypes.PACKAGE_FROGPORT.get(),
            (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.advancements = new AdvancementBehaviour(this, AllAdvancements.FROGPORT));
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
      super.addBehaviours(behaviours);
   }

   public boolean isAnimationInProgress() {
      return this.animationProgress.getChaseTarget() == 1.0F;
   }

   @Override
   public AABB getRenderBoundingBox() {
      AABB bb = super.getRenderBoundingBox().expandTowards(0.0, 1.0, 0.0);
      if (this.target != null) {
         bb = bb.minmax(new AABB(BlockPos.containing(this.target.getExactTargetLocation(this, this.level, this.worldPosition)))).inflate(0.5);
      }

      return bb;
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      if (!this.level.isClientSide() && !this.isAnimationInProgress()) {
         boolean prevFail = this.failedLastExport;
         this.tryPushingToAdjacentInventories();
         this.tryPullingFromOwnAndAdjacentInventories();
         if (this.failedLastExport != prevFail) {
            this.sendData();
         }
      }
   }

   public void sendAnticipate() {
      if (!this.isAnimationInProgress()) {
         for (int i = 0; i < this.inventory.getSlots(); i++) {
            if (this.inventory.getStackInSlot(i).isEmpty()) {
               this.sendAnticipate = true;
               this.sendData();
               return;
            }
         }
      }
   }

   public void anticipate() {
      this.anticipationProgress.chase(1.0, 0.1, Chaser.LINEAR);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.deferAnimationStart != null) {
         this.startAnimation(this.deferAnimationStart, this.deferAnimationInward);
         this.deferAnimationStart = null;
      }

      if (this.anticipationProgress.getValue() == 1.0F) {
         this.anticipationProgress.startWithValue(0.0);
      }

      this.manualOpenAnimationProgress.updateChaseTarget(this.openTracker.openCount > 0 ? 1.0F : 0.0F);
      boolean wasOpen = this.manualOpenAnimationProgress.getValue() > 0.0F;
      this.anticipationProgress.tickChaser();
      this.manualOpenAnimationProgress.tickChaser();
      if (this.level.isClientSide() && wasOpen && this.manualOpenAnimationProgress.getValue() == 0.0F) {
         this.sounds.close(this.level, this.worldPosition);
      }

      if (this.isAnimationInProgress()) {
         this.animationProgress.tickChaser();
         float value = this.animationProgress.getValue();
         if (this.currentlyDepositing) {
            if (this.level.isClientSide() && !this.isVirtual()) {
               if ((double)value > 0.7 && this.animatedPackage != null) {
                  this.animatedPackage = null;
               }

               if ((double)this.animationProgress.getValue(0.0F) < 0.2 && (double)value > 0.2) {
                  Vec3 v = this.target.getExactTargetLocation(this, this.level, this.worldPosition);
                  this.level.playLocalSound(v.x, v.y, v.z, SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 0.25F, 1.2F, false);
               }
            } else if ((double)value > 0.5 && this.animatedPackage != null) {
               if (this.target != null && (this.target.depositImmediately() || this.target.export(this.level, this.worldPosition, this.animatedPackage, false))
                  )
                {
                  this.computerBehaviour.prepareComputerEvent(new PackageEvent(this.animatedPackage, "package_sent"));
               } else {
                  this.drop(this.animatedPackage);
               }

               this.animatedPackage = null;
            }
         }

         if (!(value < 1.0F)) {
            this.anticipationProgress.startWithValue(0.0);
            this.animationProgress.startWithValue(0.0);
            if (this.level.isClientSide()) {
               this.animatedPackage = null;
            } else {
               if (!this.currentlyDepositing) {
                  if (!ItemHandlerHelper.insertItem(this.inventory, this.animatedPackage.copy(), false).isEmpty()) {
                     this.drop(this.animatedPackage);
                  } else {
                     this.computerBehaviour.prepareComputerEvent(new PackageEvent(this.animatedPackage, "package_received"));
                  }
               }

               this.animatedPackage = null;
            }
         }
      }
   }

   public void startAnimation(ItemStack box, boolean deposit) {
      if (PackageItem.isPackage(box)) {
         if (!deposit || this.target != null && (!this.target.depositImmediately() || this.target.export(this.level, this.worldPosition, box.copy(), false))) {
            this.animationProgress.startWithValue(0.0);
            this.animationProgress.chase(1.0, 0.1, Chaser.LINEAR);
            this.animatedPackage = box;
            this.currentlyDepositing = deposit;
            if (this.level != null && !deposit && !this.level.isClientSide()) {
               this.advancements.awardPlayer(AllAdvancements.FROGPORT);
            }

            if (this.level != null && this.level.isClientSide()) {
               this.sounds.open(this.level, this.worldPosition);
               if (this.currentlyDepositing) {
                  this.sounds.depositPackage(this.level, this.worldPosition);
               } else {
                  this.sounds.catchPackage(this.level, this.worldPosition);
                  Vec3 vec = this.target.getExactTargetLocation(this, this.level, this.worldPosition);
                  if (vec != null) {
                     for (int i = 0; i < 5; i++) {
                        this.level
                           .addParticle(
                              new BlockParticleOption(ParticleTypes.BLOCK, AllBlocks.ROPE.getDefaultState()),
                              vec.x,
                              vec.y - (double)this.level.random.nextFloat() * 0.25,
                              vec.z,
                              0.0,
                              0.0,
                              0.0
                           );
                     }
                  }
               }
            }

            if (this.level != null && !this.level.isClientSide()) {
               this.level.blockEntityChanged(this.worldPosition);
               this.sendData();
            }
         }
      }
   }

   protected void tryPushingToAdjacentInventories() {
      this.failedLastExport = false;
      if (this.itemHandler != null) {
         boolean empty = true;

         for (int i = 0; i < this.itemHandler.getSlots(); i++) {
            if (!this.itemHandler.getStackInSlot(i).isEmpty()) {
               empty = false;
            }
         }

         if (!empty) {
            IItemHandler handler = this.getAdjacentInventory(Direction.DOWN);
            if (handler != null) {
               for (int ix = 0; ix < this.itemHandler.getSlots(); ix++) {
                  ItemStack stackInSlot = this.itemHandler.extractItem(ix, 1, true);
                  if (!stackInSlot.isEmpty()) {
                     ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stackInSlot, false);
                     if (remainder.isEmpty()) {
                        this.itemHandler.extractItem(ix, 1, false);
                        this.level.blockEntityChanged(this.worldPosition);
                     } else {
                        this.failedLastExport = true;
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   protected void onOpenChange(boolean open) {
   }

   public void tryPullingFromOwnAndAdjacentInventories() {
      if (!this.isAnimationInProgress()) {
         if (this.target != null && this.target.export(this.level, this.worldPosition, PackageStyles.getDefaultBox(), true)) {
            if (!this.tryPullingFrom(this.inventory)) {
               for (Direction side : Iterate.directions) {
                  if (side == Direction.DOWN) {
                     IItemHandler handler = this.getAdjacentInventory(side);
                     if (handler != null && this.tryPullingFrom(handler)) {
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   public boolean tryPullingFrom(IItemHandler handler) {
      ItemStack extract = ItemHelper.extract(handler, stack -> {
         if (!PackageItem.isPackage(stack)) {
            return false;
         } else {
            String filterString = this.getFilterString();
            return filterString == null || handler instanceof PackagerItemHandler || !PackageItem.matchAddress(stack, filterString);
         }
      }, false);
      if (extract.isEmpty()) {
         return false;
      } else {
         this.startAnimation(extract, true);
         return true;
      }
   }

   protected IItemHandler getAdjacentInventory(Direction side) {
      BlockEntity blockEntity = this.level.getBlockEntity(this.worldPosition.relative(side));
      return blockEntity != null && !(blockEntity instanceof FrogportBlockEntity)
         ? (IItemHandler)this.level.getCapability(ItemHandler.BLOCK, blockEntity.getBlockPos(), side.getOpposite())
         : null;
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putFloat("PlacedYaw", this.passiveYaw);
      if (this.animatedPackage != null && this.isAnimationInProgress()) {
         tag.put("AnimatedPackage", this.animatedPackage.saveOptional(registries));
         tag.putBoolean("Deposit", this.currentlyDepositing);
      }

      if (this.sendAnticipate) {
         this.sendAnticipate = false;
         tag.putBoolean("Anticipate", true);
      }

      if (this.failedLastExport) {
         NBTHelper.putMarker(tag, "FailedLastExport");
      }

      if (this.goggles) {
         NBTHelper.putMarker(tag, "Goggles");
      }
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.passiveYaw = tag.getFloat("PlacedYaw");
      this.failedLastExport = tag.getBoolean("FailedLastExport");
      this.goggles = tag.getBoolean("Goggles");
      if (!clientPacket) {
         this.animatedPackage = null;
      }

      if (tag.contains("AnimatedPackage")) {
         this.deferAnimationInward = tag.getBoolean("Deposit");
         this.deferAnimationStart = ItemStack.parseOptional(registries, tag.getCompound("AnimatedPackage"));
      }

      if (clientPacket && tag.contains("Anticipate")) {
         this.anticipate();
      }
   }

   public float getYaw() {
      if (this.target == null) {
         return this.passiveYaw;
      } else {
         Vec3 diff = this.target.getExactTargetLocation(this, this.level, this.worldPosition).subtract(Vec3.atCenterOf(this.worldPosition));
         return (float)(Mth.atan2(diff.x, diff.z) * 180.0F / (float)Math.PI) + 180.0F;
      }
   }

   @Override
   public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      boolean superTip = IHaveHoveringInformation.super.addToTooltip(tooltip, isPlayerSneaking);
      if (!this.failedLastExport) {
         return superTip;
      } else {
         TooltipHelper.addHint(tooltip, "hint.blocked_frogport");
         return true;
      }
   }

   @Override
   protected void onOpenedManually() {
      if (this.level.isClientSide()) {
         this.sounds.open(this.level, this.worldPosition);
      }
   }

   @Override
   public ItemInteractionResult use(Player player) {
      if (player == null) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         ItemStack mainHandItem = player.getMainHandItem();
         if (!this.goggles && AllItems.GOGGLES.isIn(mainHandItem)) {
            this.goggles = true;
            if (!this.level.isClientSide()) {
               this.notifyUpdate();
               this.level.playSound(null, this.worldPosition, (SoundEvent)SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.BLOCKS, 0.5F, 1.0F);
            }

            return ItemInteractionResult.SUCCESS;
         } else {
            return super.use(player);
         }
      }
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }
}
