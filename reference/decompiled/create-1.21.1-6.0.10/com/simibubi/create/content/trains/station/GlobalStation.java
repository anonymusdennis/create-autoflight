package com.simibubi.create.content.trains.station;

import com.simibubi.create.Create;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.postbox.PostboxBlockEntity;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.SingleBlockEntityEdgePoint;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

public class GlobalStation extends SingleBlockEntityEdgePoint {
   public String name = "Track Station";
   public WeakReference<Train> nearestTrain = new WeakReference<>(null);
   public boolean assembling;
   public Map<BlockPos, GlobalPackagePort> connectedPorts = new HashMap<>();

   @Override
   public void blockEntityAdded(BlockEntity blockEntity, boolean front) {
      super.blockEntityAdded(blockEntity, front);
      BlockState state = blockEntity.getBlockState();
      this.assembling = state != null && state.hasProperty(StationBlock.ASSEMBLING) && (Boolean)state.getValue(StationBlock.ASSEMBLING);
   }

   @Override
   public void read(CompoundTag nbt, Provider registries, boolean migration, DimensionPalette dimensions) {
      super.read(nbt, registries, migration, dimensions);
      this.name = nbt.getString("Name");
      this.assembling = nbt.getBoolean("Assembling");
      this.nearestTrain = new WeakReference<>(null);
      this.connectedPorts.clear();
      ListTag portList = nbt.getList("Ports", 10);
      NBTHelper.iterateCompoundList(portList, c -> {
         GlobalPackagePort port = new GlobalPackagePort();
         port.address = c.getString("Address");
         port.offlineBuffer.deserializeNBT(registries, c.getCompound("OfflineBuffer"));
         port.primed = c.getBoolean("Primed");
         this.connectedPorts.put(NBTHelper.readBlockPos(c, "Pos"), port);
      });
   }

   @Override
   public void read(FriendlyByteBuf buffer, DimensionPalette dimensions) {
      super.read(buffer, dimensions);
      this.name = buffer.readUtf();
      this.assembling = buffer.readBoolean();
      if (buffer.readBoolean()) {
         this.blockEntityPos = buffer.readBlockPos();
      }
   }

   @Override
   public void write(CompoundTag nbt, Provider registries, DimensionPalette dimensions) {
      super.write(nbt, registries, dimensions);
      nbt.putString("Name", this.name);
      nbt.putBoolean("Assembling", this.assembling);
      nbt.put("Ports", NBTHelper.writeCompoundList(this.connectedPorts.entrySet(), e -> {
         CompoundTag c = new CompoundTag();
         c.putString("Address", ((GlobalPackagePort)e.getValue()).address);
         c.put("OfflineBuffer", ((GlobalPackagePort)e.getValue()).offlineBuffer.serializeNBT(registries));
         c.putBoolean("Primed", ((GlobalPackagePort)e.getValue()).primed);
         c.put("Pos", NbtUtils.writeBlockPos((BlockPos)e.getKey()));
         return c;
      }));
   }

   @Override
   public void write(FriendlyByteBuf buffer, DimensionPalette dimensions) {
      super.write(buffer, dimensions);
      buffer.writeUtf(this.name);
      buffer.writeBoolean(this.assembling);
      buffer.writeBoolean(this.blockEntityPos != null);
      if (this.blockEntityPos != null) {
         buffer.writeBlockPos(this.blockEntityPos);
      }
   }

   public boolean canApproachFrom(TrackNode side) {
      return this.isPrimary(side) && !this.assembling;
   }

   @Override
   public boolean canNavigateVia(TrackNode side) {
      return super.canNavigateVia(side) && !this.assembling;
   }

   public void reserveFor(Train train) {
      Train nearestTrain = this.getNearestTrain();
      if (nearestTrain == null || nearestTrain.navigation.distanceToDestination > train.navigation.distanceToDestination) {
         this.nearestTrain = new WeakReference<>(train);
      }
   }

   public void cancelReservation(Train train) {
      if (this.nearestTrain.get() == train) {
         this.nearestTrain = new WeakReference<>(null);
      }
   }

   public void trainDeparted(Train train) {
      this.cancelReservation(train);
   }

   @Nullable
   public Train getPresentTrain() {
      Train nearestTrain = this.getNearestTrain();
      return nearestTrain != null && nearestTrain.getCurrentStation() == this ? nearestTrain : null;
   }

   @Nullable
   public Train getImminentTrain() {
      Train nearestTrain = this.getNearestTrain();
      if (nearestTrain == null) {
         return nearestTrain;
      } else if (nearestTrain.getCurrentStation() == this) {
         return nearestTrain;
      } else if (!nearestTrain.navigation.isActive()) {
         return null;
      } else {
         return nearestTrain.navigation.distanceToDestination > 30.0 ? null : nearestTrain;
      }
   }

   @Nullable
   public Train getNearestTrain() {
      return this.nearestTrain.get();
   }

   public void runMailTransfer() {
      Train train = this.getPresentTrain();
      if (train != null && !this.connectedPorts.isEmpty()) {
         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
         Level level = server.getLevel(this.getBlockEntityDimension());

         for (Carriage carriage : train.carriages) {
            IItemHandlerModifiable carriageInventory = carriage.storage.getAllItems();
            if (carriageInventory != null) {
               for (Entry<BlockPos, GlobalPackagePort> entry : this.connectedPorts.entrySet()) {
                  GlobalPackagePort port = entry.getValue();
                  BlockPos pos = entry.getKey();
                  PostboxBlockEntity box = null;
                  IItemHandlerModifiable postboxInventory = port.offlineBuffer;
                  if (level != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PostboxBlockEntity ppbe) {
                     postboxInventory = ppbe.inventory;
                     box = ppbe;
                  }

                  for (int slot = 0; slot < postboxInventory.getSlots(); slot++) {
                     ItemStack stack = postboxInventory.getStackInSlot(slot);
                     if (PackageItem.isPackage(stack) && !PackageItem.matchAddress(stack, port.address)) {
                        ItemStack result = ItemHandlerHelper.insertItemStacked(carriageInventory, stack, false);
                        if (box != null) {
                           box.computerBehaviour.prepareComputerEvent(new PackageEvent(stack, "package_sent"));
                        }

                        if (result.isEmpty()) {
                           postboxInventory.setStackInSlot(slot, ItemStack.EMPTY);
                           if (box == null) {
                              port.primed = true;
                           } else {
                              box.spawnParticles();
                           }

                           Create.RAILWAYS.markTracksDirty();
                        }
                     }
                  }
               }

               for (int slotx = 0; slotx < carriageInventory.getSlots(); slotx++) {
                  ItemStack stack = carriageInventory.getStackInSlot(slotx);
                  if (PackageItem.isPackage(stack)) {
                     for (Entry<BlockPos, GlobalPackagePort> entry : this.connectedPorts.entrySet()) {
                        GlobalPackagePort port = entry.getValue();
                        BlockPos pos = entry.getKey();
                        PostboxBlockEntity box = null;
                        if (PackageItem.matchAddress(stack, port.address)) {
                           IItemHandler postboxInventory = port.offlineBuffer;
                           if (level != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PostboxBlockEntity ppbe) {
                              postboxInventory = ppbe.inventory;
                              box = ppbe;
                           }

                           ItemStack resultx = ItemHandlerHelper.insertItemStacked(postboxInventory, stack, false);
                           if (box != null) {
                              box.computerBehaviour.prepareComputerEvent(new PackageEvent(stack, "package_received"));
                           }

                           if (resultx.isEmpty()) {
                              carriageInventory.setStackInSlot(slotx, ItemStack.EMPTY);
                              if (box == null) {
                                 port.primed = true;
                              } else {
                                 box.spawnParticles();
                              }

                              Create.RAILWAYS.markTracksDirty();
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
