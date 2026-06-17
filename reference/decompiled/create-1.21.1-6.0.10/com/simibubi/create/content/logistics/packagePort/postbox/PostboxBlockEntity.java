package com.simibubi.create.content.logistics.packagePort.postbox;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.trains.station.GlobalPackagePort;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.lang.ref.WeakReference;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;

public class PostboxBlockEntity extends PackagePortBlockEntity {
   public WeakReference<GlobalStation> trackedGlobalStation = new WeakReference<>(null);
   public LerpedFloat flag = LerpedFloat.linear().startWithValue(0.0);
   public boolean forceFlag;
   private boolean sendParticles;
   public AbstractComputerBehaviour computerBehaviour;

   public PostboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.PACKAGE_POSTBOX.get(), (be, context) -> be.itemHandler);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
      super.addBehaviours(behaviours);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level.isClientSide && !this.isVirtual()) {
         if (this.sendParticles) {
            this.sendData();
         }
      } else {
         float currentTarget = this.flag.getChaseTarget();
         if (currentTarget == 0.0F || this.flag.settled()) {
            int target = this.inventory.isEmpty() && !this.forceFlag ? 0 : 1;
            if ((float)target != currentTarget) {
               this.flag.chase((double)target, 0.1F, Chaser.LINEAR);
               if (target == 1) {
                  AllSoundEvents.CONTRAPTION_ASSEMBLE.playAt(this.level, this.worldPosition, 1.0F, 2.0F, true);
               }
            }
         }

         boolean settled = this.flag.getValue() > 0.15F;
         this.flag.tickChaser();
         if (currentTarget == 0.0F && settled != this.flag.getValue() > 0.15F) {
            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playAt(this.level, this.worldPosition, 0.75F, 1.5F, true);
         }

         if (this.sendParticles) {
            this.sendParticles = false;
            BoneMealItem.addGrowthParticles(this.level, this.worldPosition, 40);
         }
      }
   }

   @Override
   protected void onOpenChange(boolean open) {
      BlockState state = this.level.getBlockState(this.worldPosition);
      if (state.getBlock() instanceof PostboxBlock) {
         this.level.setBlockAndUpdate(this.worldPosition, (BlockState)state.setValue(PostboxBlock.OPEN, open));
         this.level.playSound(null, this.worldPosition, open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS);
      }
   }

   public void spawnParticles() {
      this.sendParticles = true;
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      if (clientPacket && this.sendParticles) {
         NBTHelper.putMarker(tag, "Particles");
      }

      this.sendParticles = false;
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.sendParticles = clientPacket && tag.contains("Particles");
   }

   public void setChanged() {
      this.saveOfflineBuffer();
      super.setChanged();
   }

   private void saveOfflineBuffer() {
      if (this.level != null && !this.level.isClientSide) {
         GlobalStation station = this.trackedGlobalStation.get();
         if (station != null) {
            GlobalPackagePort globalPackagePort = station.connectedPorts.get(this.worldPosition);
            if (globalPackagePort != null) {
               globalPackagePort.saveOfflineBuffer(this.inventory);
            }
         }
      }
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }
}
