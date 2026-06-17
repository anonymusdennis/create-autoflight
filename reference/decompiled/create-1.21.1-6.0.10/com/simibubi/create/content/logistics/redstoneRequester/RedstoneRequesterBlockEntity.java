package com.simibubi.create.content.logistics.redstoneRequester;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.WiFiParticle;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockCheckingBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.util.List;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.FakePlayer;

public class RedstoneRequesterBlockEntity extends StockCheckingBlockEntity implements MenuProvider {
   public AbstractComputerBehaviour computerBehaviour;
   public boolean allowPartialRequests;
   public PackageOrderWithCrafts encodedRequest = PackageOrderWithCrafts.empty();
   public String encodedTargetAdress = "";
   public boolean lastRequestSucceeded;
   protected boolean redstonePowered;

   public RedstoneRequesterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.allowPartialRequests = false;
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(),
            (BlockEntityType)AllBlockEntityTypes.REDSTONE_REQUESTER.get(),
            (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }

   protected void onRedstonePowerChanged() {
      boolean hasNeighborSignal = this.level.hasNeighborSignal(this.worldPosition);
      if (this.redstonePowered != hasNeighborSignal) {
         this.lastRequestSucceeded = false;
         if (hasNeighborSignal) {
            this.triggerRequest();
         }

         this.redstonePowered = hasNeighborSignal;
         this.notifyUpdate();
      }
   }

   public void triggerRequest() {
      if (!this.encodedRequest.isEmpty()) {
         boolean anySucceeded = false;
         InventorySummary summaryOfOrder = new InventorySummary();
         this.encodedRequest.stacks().forEach(summaryOfOrder::add);
         InventorySummary summary = this.getAccurateSummary();

         for (BigItemStack entry : summaryOfOrder.getStacks()) {
            if (summary.getCountOf(entry.stack) >= entry.count) {
               anySucceeded = true;
            } else if (!this.allowPartialRequests && this.level instanceof ServerLevel serverLevel) {
               CatnipServices.NETWORK.sendToClientsAround(serverLevel, this.worldPosition, 32.0, new RedstoneRequesterEffectPacket(this.worldPosition, false));
               return;
            }
         }

         this.broadcastPackageRequest(LogisticallyLinkedBehaviour.RequestType.REDSTONE, this.encodedRequest, null, this.encodedTargetAdress);
         if (this.level instanceof ServerLevel serverLevel) {
            CatnipServices.NETWORK
               .sendToClientsAround(serverLevel, this.worldPosition, 32.0, new RedstoneRequesterEffectPacket(this.worldPosition, anySucceeded));
         }

         this.lastRequestSucceeded = true;
      }
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.redstonePowered = tag.getBoolean("Powered");
      this.lastRequestSucceeded = tag.getBoolean("Success");
      this.allowPartialRequests = tag.getBoolean("AllowPartial");
      this.encodedRequest = CatnipCodecUtils.decode(PackageOrderWithCrafts.CODEC, registries, tag.getCompound("EncodedRequest"))
         .orElse(PackageOrderWithCrafts.empty());
      this.encodedTargetAdress = tag.getString("EncodedAddress");
   }

   @Override
   public void writeSafe(CompoundTag tag, Provider registries) {
      super.writeSafe(tag, registries);
      tag.putBoolean("AllowPartial", this.allowPartialRequests);
      tag.putString("EncodedAddress", this.encodedTargetAdress);
      tag.put("EncodedRequest", (Tag)CatnipCodecUtils.encode(PackageOrderWithCrafts.CODEC, registries, this.encodedRequest).orElseThrow());
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putBoolean("Powered", this.redstonePowered);
      tag.putBoolean("Success", this.lastRequestSucceeded);
      tag.putBoolean("AllowPartial", this.allowPartialRequests);
      tag.putString("EncodedAddress", this.encodedTargetAdress);
      tag.put("EncodedRequest", (Tag)CatnipCodecUtils.encode(PackageOrderWithCrafts.CODEC, registries, this.encodedRequest).orElseThrow());
   }

   public InteractionResult use(Player player) {
      if (player == null || player.isCrouching()) {
         return InteractionResult.PASS;
      } else if (player instanceof FakePlayer) {
         return InteractionResult.PASS;
      } else if (this.level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else if (!this.behaviour.mayInteractMessage(player)) {
         return InteractionResult.SUCCESS;
      } else {
         player.openMenu(this, this.worldPosition);
         return InteractionResult.SUCCESS;
      }
   }

   public Component getDisplayName() {
      return Component.empty();
   }

   public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
      return RedstoneRequesterMenu.create(pContainerId, pPlayerInventory, this);
   }

   public void playEffect(boolean success) {
      Vec3 vec3 = Vec3.atCenterOf(this.worldPosition);
      if (success) {
         AllSoundEvents.CONFIRM.playAt(this.level, this.worldPosition, 0.5F, 1.5F, false);
         AllSoundEvents.STOCK_LINK.playAt(this.level, this.worldPosition, 1.0F, 1.0F, false);
         this.level.addParticle(new WiFiParticle.Data(), vec3.x, vec3.y, vec3.z, 1.0, 1.0, 1.0);
      } else {
         AllSoundEvents.DENY.playAt(this.level, this.worldPosition, 0.5F, 1.0F, false);
         this.level.addParticle(ParticleTypes.ENCHANTED_HIT, vec3.x, vec3.y + 1.0, vec3.z, 0.0, 0.0, 0.0);
      }
   }
}
