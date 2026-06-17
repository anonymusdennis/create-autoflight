package com.simibubi.create.content.contraptions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Sets.SetView;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageWrapper;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageWrapper;
import com.simibubi.create.content.equipment.toolbox.ToolboxMountedStorage;
import com.simibubi.create.content.fluids.tank.storage.FluidTankMountedStorage;
import com.simibubi.create.content.fluids.tank.storage.creative.CreativeFluidTankMountedStorage;
import com.simibubi.create.content.logistics.crate.CreativeCrateMountedStorage;
import com.simibubi.create.content.logistics.depot.storage.DepotMountedStorage;
import com.simibubi.create.content.logistics.vault.ItemVaultMountedStorage;
import com.simibubi.create.impl.contraption.storage.FallbackMountedStorage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.Nullable;

public class MountedStorageManager {
   private Map<BlockPos, MountedItemStorage> itemsBuilder;
   private Map<BlockPos, MountedFluidStorage> fluidsBuilder;
   private Map<BlockPos, SyncedMountedStorage> syncedItemsBuilder;
   private Map<BlockPos, SyncedMountedStorage> syncedFluidsBuilder;
   private ImmutableMap<BlockPos, MountedItemStorage> allItemStorages;
   protected MountedItemStorageWrapper items;
   @Nullable
   protected MountedItemStorageWrapper fuelItems;
   protected MountedFluidStorageWrapper fluids;
   private ImmutableMap<BlockPos, SyncedMountedStorage> syncedItems;
   private ImmutableMap<BlockPos, SyncedMountedStorage> syncedFluids;
   private List<IItemHandlerModifiable> externalHandlers;
   protected CombinedInvWrapper allItems;
   private int syncCooldown;
   private Set<BlockPos> interactablePositions;

   public MountedStorageManager() {
      this.reset();
   }

   public void initialize() {
      if (!this.isInitialized()) {
         this.allItemStorages = ImmutableMap.copyOf(this.itemsBuilder);
         this.items = new MountedItemStorageWrapper(subMap(this.allItemStorages, this::isExposed));
         this.allItems = this.items;
         this.itemsBuilder = null;
         ImmutableMap<BlockPos, MountedItemStorage> fuelMap = subMap(this.allItemStorages, this::canUseForFuel);
         this.fuelItems = fuelMap.isEmpty() ? null : new MountedItemStorageWrapper(fuelMap);
         ImmutableMap<BlockPos, MountedFluidStorage> fluids = ImmutableMap.copyOf(this.fluidsBuilder);
         this.fluids = new MountedFluidStorageWrapper(fluids);
         this.fluidsBuilder = null;
         this.syncedItems = ImmutableMap.copyOf(this.syncedItemsBuilder);
         this.syncedItemsBuilder = null;
         this.syncedFluids = ImmutableMap.copyOf(this.syncedFluidsBuilder);
         this.syncedFluidsBuilder = null;
      }
   }

   private boolean isExposed(MountedItemStorage storage) {
      return !AllTags.AllMountedItemStorageTypeTags.INTERNAL.matches(storage);
   }

   private boolean canUseForFuel(MountedItemStorage storage) {
      return this.isExposed(storage) && !AllTags.AllMountedItemStorageTypeTags.FUEL_BLACKLIST.matches(storage);
   }

   private boolean isInitialized() {
      return this.itemsBuilder == null;
   }

   private void assertInitialized() {
      if (!this.isInitialized()) {
         throw new IllegalStateException("MountedStorageManager is uninitialized");
      }
   }

   protected void reset() {
      this.allItemStorages = null;
      this.items = null;
      this.fuelItems = null;
      this.fluids = null;
      this.externalHandlers = new ArrayList<>();
      this.allItems = null;
      this.itemsBuilder = new HashMap<>();
      this.fluidsBuilder = new HashMap<>();
      this.syncedItemsBuilder = new HashMap<>();
      this.syncedFluidsBuilder = new HashMap<>();
   }

   public void addBlock(Level level, BlockState state, BlockPos globalPos, BlockPos localPos, @Nullable BlockEntity be) {
      MountedItemStorageType<?> itemType = MountedItemStorageType.REGISTRY.get(state.getBlock());
      if (itemType != null) {
         MountedItemStorage storage = itemType.mount(level, state, globalPos, be);
         if (storage != null) {
            this.addStorage(storage, localPos);
         }
      }

      MountedFluidStorageType<?> fluidType = MountedFluidStorageType.REGISTRY.get(state.getBlock());
      if (fluidType != null) {
         MountedFluidStorage storage = fluidType.mount(level, state, globalPos, be);
         if (storage != null) {
            this.addStorage(storage, localPos);
         }
      }
   }

   public void unmount(Level level, StructureBlockInfo info, BlockPos globalPos, @Nullable BlockEntity be) {
      BlockPos localPos = info.pos();
      BlockState state = info.state();
      MountedItemStorage itemStorage = (MountedItemStorage)this.getAllItemStorages().get(localPos);
      if (itemStorage != null) {
         MountedItemStorageType<?> expectedType = MountedItemStorageType.REGISTRY.get(state.getBlock());
         if (itemStorage.type == expectedType) {
            itemStorage.unmount(level, state, globalPos, be);
         }
      }

      MountedFluidStorage fluidStorage = (MountedFluidStorage)this.getFluids().storages.get(localPos);
      if (fluidStorage != null) {
         MountedFluidStorageType<?> expectedType = MountedFluidStorageType.REGISTRY.get(state.getBlock());
         if (fluidStorage.type == expectedType) {
            fluidStorage.unmount(level, state, globalPos, be);
         }
      }
   }

   public void tick(AbstractContraptionEntity entity) {
      if (this.syncCooldown > 0) {
         this.syncCooldown--;
      } else {
         Map<BlockPos, MountedItemStorage> items = new HashMap<>();
         Map<BlockPos, MountedFluidStorage> fluids = new HashMap<>();
         this.syncedItems.forEach((pos, storage) -> {
            if (storage.isDirty()) {
               items.put(pos, (MountedItemStorage)storage);
               storage.markClean();
            }
         });
         this.syncedFluids.forEach((pos, storage) -> {
            if (storage.isDirty()) {
               fluids.put(pos, (MountedFluidStorage)storage);
               storage.markClean();
            }
         });
         if (!items.isEmpty() || !fluids.isEmpty()) {
            MountedStorageSyncPacket packet = new MountedStorageSyncPacket(entity.getId(), items, fluids);
            CatnipServices.NETWORK.sendToClientsTrackingEntity(entity, packet);
            this.syncCooldown = 8;
         }
      }
   }

   public void handleSync(MountedStorageSyncPacket packet, AbstractContraptionEntity entity) {
      ImmutableMap<BlockPos, MountedItemStorage> items = this.getAllItemStorages();
      MountedFluidStorageWrapper fluids = this.getFluids();
      this.reset();
      Map<SyncedMountedStorage, BlockPos> syncedStorages = new IdentityHashMap<>();

      try {
         this.itemsBuilder.putAll(items);
         this.fluidsBuilder.putAll(fluids.storages);
         packet.items().forEach((pos, storage) -> {
            this.itemsBuilder.put(pos, storage);
            syncedStorages.put((SyncedMountedStorage)storage, pos);
         });
         packet.fluids().forEach((pos, storage) -> {
            this.fluidsBuilder.put(pos, storage);
            syncedStorages.put((SyncedMountedStorage)storage, pos);
         });
      } catch (Throwable var7) {
         Create.LOGGER.error("An error occurred while syncing a MountedStorageManager", var7);
      }

      this.initialize();
      Contraption contraption = entity.getContraption();
      syncedStorages.forEach((storage, pos) -> storage.afterSync(contraption, pos));
   }

   public void read(CompoundTag nbt, Provider registries, boolean clientPacket, @Nullable Contraption contraption) {
      RegistryOps<Tag> registryOps = registries.createSerializationContext(NbtOps.INSTANCE);
      this.reset();

      try {
         NBTHelper.iterateCompoundList(
            nbt.getList("items", 10),
            tag -> {
               BlockPos pos = NBTHelper.readBlockPos(tag, "pos");
               CompoundTag data = tag.getCompound("storage");
               MountedItemStorage.CODEC
                  .decode(registryOps, data)
                  .resultOrPartial(err -> Create.LOGGER.error("Failed to deserialize mounted item storage: {}", err))
                  .<MountedItemStorage>map(Pair::getFirst)
                  .ifPresent(storage -> this.addStorage(storage, pos));
            }
         );
         NBTHelper.iterateCompoundList(
            nbt.getList("fluids", 10),
            tag -> {
               BlockPos pos = NBTHelper.readBlockPos(tag, "pos");
               CompoundTag data = tag.getCompound("storage");
               MountedFluidStorage.CODEC
                  .decode(registryOps, data)
                  .resultOrPartial(err -> Create.LOGGER.error("Failed to deserialize mounted fluid storage: {}", err))
                  .<MountedFluidStorage>map(Pair::getFirst)
                  .ifPresent(storage -> this.addStorage(storage, pos));
            }
         );
         this.readLegacy(registries, nbt);
         if (nbt.contains("interactable_positions")) {
            this.interactablePositions = new HashSet<>();
            NBTHelper.iterateCompoundList(nbt.getList("interactable_positions", 10), tag -> {
               BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
               this.interactablePositions.add(pos);
            });
         }
      } catch (Throwable var7) {
         Create.LOGGER.error("Error deserializing mounted storage", var7);
      }

      this.initialize();
      if (clientPacket && contraption != null) {
         this.getAllItemStorages().forEach((pos, storage) -> {
            if (storage instanceof SyncedMountedStorage synced) {
               synced.afterSync(contraption, pos);
            }
         });
         this.getFluids().storages.forEach((pos, storage) -> {
            if (storage instanceof SyncedMountedStorage synced) {
               synced.afterSync(contraption, pos);
            }
         });
      }
   }

   public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
      RegistryOps<Tag> registryOps = registries.createSerializationContext(NbtOps.INSTANCE);
      ListTag items = new ListTag();
      this.getAllItemStorages()
         .forEach(
            (posx, storage) -> {
               if (!clientPacket || storage instanceof SyncedMountedStorage) {
                  MountedItemStorage.CODEC
                     .encodeStart(registryOps, storage)
                     .resultOrPartial(err -> Create.LOGGER.error("Failed to serialize mounted item storage: {}", err))
                     .ifPresent(encoded -> {
                        CompoundTag tagx = new CompoundTag();
                        tagx.put("pos", NbtUtils.writeBlockPos(posx));
                        tagx.put("storage", encoded);
                        items.add(tagx);
                     });
               }
            }
         );
      if (!items.isEmpty()) {
         nbt.put("items", items);
      }

      ListTag fluids = new ListTag();
      this.getFluids()
         .storages
         .forEach(
            (posx, storage) -> {
               if (!clientPacket || storage instanceof SyncedMountedStorage) {
                  MountedFluidStorage.CODEC
                     .encodeStart(registryOps, storage)
                     .resultOrPartial(err -> Create.LOGGER.error("Failed to serialize mounted fluid storage: {}", err))
                     .ifPresent(encoded -> {
                        CompoundTag tagx = new CompoundTag();
                        tagx.put("pos", NbtUtils.writeBlockPos(posx));
                        tagx.put("storage", encoded);
                        fluids.add(tagx);
                     });
               }
            }
         );
      if (!fluids.isEmpty()) {
         nbt.put("fluids", fluids);
      }

      if (clientPacket) {
         SetView<BlockPos> positions = Sets.union(this.getAllItemStorages().keySet(), this.getFluids().storages.keySet());
         ListTag list = new ListTag();
         UnmodifiableIterator var9 = positions.iterator();

         while (var9.hasNext()) {
            BlockPos pos = (BlockPos)var9.next();
            CompoundTag tag = new CompoundTag();
            tag.putInt("X", pos.getX());
            tag.putInt("Y", pos.getY());
            tag.putInt("Z", pos.getZ());
            list.add(tag);
         }

         nbt.put("interactable_positions", list);
      }
   }

   public void attachExternal(IItemHandlerModifiable externalStorage) {
      this.externalHandlers.add(externalStorage);
      IItemHandlerModifiable[] all = new IItemHandlerModifiable[this.externalHandlers.size() + 1];
      all[0] = this.items;

      for (int i = 0; i < this.externalHandlers.size(); i++) {
         all[i + 1] = this.externalHandlers.get(i);
      }

      this.allItems = new CombinedInvWrapper(all);
   }

   public CombinedInvWrapper getAllItems() {
      this.assertInitialized();
      return this.allItems;
   }

   public ImmutableMap<BlockPos, MountedItemStorage> getAllItemStorages() {
      this.assertInitialized();
      return this.allItemStorages;
   }

   public MountedItemStorageWrapper getMountedItems() {
      this.assertInitialized();
      return this.items;
   }

   @Nullable
   public MountedItemStorageWrapper getFuelItems() {
      this.assertInitialized();
      return this.fuelItems;
   }

   public MountedFluidStorageWrapper getFluids() {
      this.assertInitialized();
      return this.fluids;
   }

   public boolean handlePlayerStorageInteraction(Contraption contraption, Player player, BlockPos localPos) {
      if (player instanceof ServerPlayer serverPlayer) {
         StructureBlockInfo info = contraption.getBlocks().get(localPos);
         if (info == null) {
            return false;
         } else {
            MountedStorageManager storageManager = contraption.getStorage();
            MountedItemStorage storage = (MountedItemStorage)storageManager.getAllItemStorages().get(localPos);
            return storage != null ? storage.handleInteraction(serverPlayer, contraption, info) : false;
         }
      } else {
         return this.interactablePositions != null && this.interactablePositions.contains(localPos);
      }
   }

   private void readLegacy(Provider registries, CompoundTag nbt) {
      NBTHelper.iterateCompoundList(nbt.getList("Storage", 10), tag -> {
         BlockPos pos = NBTHelper.readBlockPos(tag, "Pos");
         CompoundTag data = tag.getCompound("Data");
         if (data.contains("Toolbox")) {
            this.addStorage(ToolboxMountedStorage.fromLegacy(registries, data), pos);
         } else if (data.contains("NoFuel")) {
            this.addStorage(ItemVaultMountedStorage.fromLegacy(registries, data), pos);
         } else if (data.contains("Bottomless")) {
            ItemStack supplied = ItemStack.parseOptional(registries, data.getCompound("ProvidedStack"));
            this.addStorage(new CreativeCrateMountedStorage(supplied), pos);
         } else if (data.contains("Synced")) {
            this.addStorage(DepotMountedStorage.fromLegacy(registries, data), pos);
         } else {
            ItemStackHandler handler = new ItemStackHandler();
            handler.deserializeNBT(registries, data);
            this.addStorage(new FallbackMountedStorage(handler), pos);
         }
      });
      NBTHelper.iterateCompoundList(nbt.getList("FluidStorage", 10), tag -> {
         BlockPos pos = NBTHelper.readBlockPos(tag, "Pos");
         CompoundTag data = tag.getCompound("Data");
         if (data.contains("Bottomless")) {
            this.addStorage(CreativeFluidTankMountedStorage.fromLegacy(registries, data), pos);
         } else {
            this.addStorage(FluidTankMountedStorage.fromLegacy(registries, data), pos);
         }
      });
   }

   private void addStorage(MountedItemStorage storage, BlockPos pos) {
      this.itemsBuilder.put(pos, storage);
      if (storage instanceof SyncedMountedStorage synced) {
         this.syncedItemsBuilder.put(pos, synced);
      }
   }

   private void addStorage(MountedFluidStorage storage, BlockPos pos) {
      this.fluidsBuilder.put(pos, storage);
      if (storage instanceof SyncedMountedStorage synced) {
         this.syncedFluidsBuilder.put(pos, synced);
      }
   }

   private static <K, V> ImmutableMap<K, V> subMap(Map<K, V> map, Predicate<V> predicate) {
      Builder<K, V> builder = ImmutableMap.builder();
      map.forEach((key, value) -> {
         if (predicate.test((V)value)) {
            builder.put(key, value);
         }
      });
      return builder.build();
   }
}
