package dev.ryanhcode.sable.sublevel.storage.holding;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableConfig;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.mixinterface.toast.SableToastableServer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.SubLevelTicketsSavedData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import dev.ryanhcode.sable.sublevel.tracking_points.TrackingPoint;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.Visibility;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector3d;

public class SubLevelHoldingChunkMap implements AutoCloseable {
   private final ServerLevel level;
   private final ServerSubLevelContainer container;
   private final SubLevelStorage storage;
   private final Object2ObjectMap<UUID, HoldingSubLevel> allHoldingSubLevels = new Object2ObjectOpenHashMap();
   private final Long2ObjectMap<SubLevelHoldingChunk> loadedHoldingChunks = new Long2ObjectOpenHashMap();
   private final LongSet dirtyHoldingChunks = new LongOpenHashSet();
   private final ObjectSet<ChunkPos> queuedUnloads = new ObjectOpenHashSet();
   private final ObjectSet<GlobalSavedSubLevelPointer> queuedDeletion = new ObjectOpenHashSet();
   private final LongSet chunksToUnload = new LongOpenHashSet();
   private final LongSet chunksToLoad = new LongOpenHashSet();
   private boolean verboseLogging = false;

   public SubLevelHoldingChunkMap(ServerLevel level, ServerSubLevelContainer container) {
      this.level = level;
      this.container = container;
      File worldFolder = level.getChunkSource().getDataStorage().dataFolder.getParentFile();
      File subLevelsFolder = new File(worldFolder, "sublevels");
      subLevelsFolder.mkdirs();
      this.storage = new SubLevelStorage(subLevelsFolder.toPath());
   }

   public void updateChunkStatus(ChunkPos chunkPos, boolean loaded) {
      long key = chunkPos.toLong();
      if (!loaded) {
         this.chunksToUnload.add(key);
         this.chunksToLoad.remove(key);
      } else {
         this.chunksToLoad.add(key);
         this.chunksToUnload.remove(key);
      }
   }

   private void processLoad(ChunkPos chunkPos) {
      if (this.verboseLogging) {
         Sable.LOGGER.info("Processing load of chunk at {}", chunkPos);
      }

      if (this.queuedUnloads.contains(chunkPos)) {
         if (this.verboseLogging) {
            Sable.LOGGER.info("Removing chunk at {} from queued unloads", chunkPos);
         }

         this.queuedUnloads.remove(chunkPos);
      }

      SubLevelHoldingChunk existingChunk = (SubLevelHoldingChunk)this.loadedHoldingChunks.get(chunkPos.toLong());
      if (existingChunk != null) {
         existingChunk.markKeepLoaded();
      } else {
         SubLevelHoldingChunk holdingChunk = this.getOrLoadHoldingChunk(chunkPos, false);
         if (holdingChunk != null) {
            holdingChunk.markKeepLoaded();
         }
      }
   }

   private void processUnload(ChunkPos chunkPos, Collection<ServerSubLevel> forceLoaded) {
      if (this.loadedHoldingChunks.containsKey(chunkPos.toLong())) {
         if (this.verboseLogging) {
            Sable.LOGGER.info("Processing unload for chunk {}", chunkPos);
         }

         BoundingBox3d bounds = new BoundingBox3d(
            (double)(chunkPos.x << 4),
            -Double.MAX_VALUE,
            (double)(chunkPos.z << 4),
            (double)((chunkPos.x << 4) + 16),
            Double.MAX_VALUE,
            (double)((chunkPos.z << 4) + 16)
         );
         SubLevelContainer container = SubLevelContainer.getContainer(this.level);

         assert container != null : "Sub-level container is null";

         Iterable<SubLevel> toUnloadIterator = container.queryIntersecting(bounds);
         ObjectOpenHashSet<ServerSubLevel> toUnload = new ObjectOpenHashSet();

         for (SubLevel subLevel : toUnloadIterator) {
            toUnload.add((ServerSubLevel)subLevel);
         }

         if (this.verboseLogging) {
            Sable.LOGGER.info("Adding chunk {} to queued unloads", chunkPos);
         }

         this.queuedUnloads.add(chunkPos);
         if (!toUnload.isEmpty()) {
            SubLevelHoldingChunk holdingChunk = this.getOrLoadHoldingChunk(chunkPos, true);
            ObjectSet<ServerSubLevel> visited = new ObjectOpenHashSet();
            ObjectIterator var9 = toUnload.iterator();

            while (var9.hasNext()) {
               ServerSubLevel subLevel = (ServerSubLevel)var9.next();
               if (forceLoaded.contains(subLevel)) {
                  visited.add(subLevel);
               } else if (!visited.contains(subLevel)) {
                  Collection<ServerSubLevel> chain = SubLevelHelper.getLoadingDependencyChain(subLevel);
                  visited.addAll(chain);
                  List<UUID> uuids = chain.stream().map(SubLevel::getUniqueId).toList();

                  for (ServerSubLevel chainedSubLevel : chain) {
                     GlobalSavedSubLevelPointer pointer = chainedSubLevel.getLastSerializationPointer();
                     if (this.verboseLogging) {
                        Sable.LOGGER
                           .info("Unloading sub-level {} with pointer {} to chunk {} as holding sub-level", new Object[]{chainedSubLevel, pointer, chunkPos});
                     }

                     SubLevelData data = SubLevelSerializer.toData(chainedSubLevel, uuids);
                     HoldingSubLevel holdingSubLevel = new HoldingSubLevel(data, pointer);
                     holdingChunk.acceptHoldingSubLevel(holdingSubLevel);
                     this.allHoldingSubLevels.put(holdingSubLevel.data().uuid(), holdingSubLevel);
                     container.removeSubLevel(chainedSubLevel, SubLevelRemovalReason.UNLOADED);
                  }
               }
            }
         }
      }
   }

   public void saveAll() {
      if ((Boolean)SableConfig.SUB_LEVEL_SAVING_LOG_MESSAGE.get()) {
         Sable.LOGGER.info("Saving sub-levels for level '{}'/{}", this.level, this.level.dimension().location());
      }

      if (this.verboseLogging) {
         Sable.LOGGER.info("Saving holding chunk-map");
      }

      this.processChanges();
      ObjectIterator subLevels = this.queuedDeletion.iterator();

      while (subLevels.hasNext()) {
         GlobalSavedSubLevelPointer deletion = (GlobalSavedSubLevelPointer)subLevels.next();
         if (this.verboseLogging) {
            Sable.LOGGER.info("Processing queued deletion & clearing data for {}", deletion);
         }

         this.storage.attemptSaveSubLevel(deletion, null);
      }

      this.queuedDeletion.clear();
      List<ServerSubLevel> subLevelsx = this.container.getAllSubLevels();
      Collection<ServerSubLevel> toMove = new ObjectArrayList(subLevelsx);
      Collection<ServerSubLevel> moved = new ObjectArraySet(toMove.size());

      for (ServerSubLevel subLevel : toMove) {
         if (!moved.contains(subLevel)) {
            Vector3d currentPosition = subLevel.logicalPose().position();
            ChunkPos moveToChunk = new ChunkPos(BlockPos.containing(currentPosition.x, currentPosition.y, currentPosition.z));
            Collection<ServerSubLevel> chain = SubLevelHelper.getLoadingDependencyChain(subLevel);
            moved.addAll(chain);
            List<UUID> uuids = chain.stream().map(SubLevel::getUniqueId).toList();

            for (ServerSubLevel chainedSubLevel : chain) {
               if (this.verboseLogging) {
                  Sable.LOGGER.info("Moving sub-level {} with last pointer {}", chainedSubLevel, chainedSubLevel.getLastSerializationPointer());
               }

               this.moveAndSaveSubLevel(chainedSubLevel, moveToChunk, uuids);
               SubLevelHoldingChunk holdingChunk = (SubLevelHoldingChunk)this.loadedHoldingChunks.get(moveToChunk.toLong());
               holdingChunk.markKeepLoaded();
            }
         }
      }

      ObjectIterator var16 = this.loadedHoldingChunks.values().iterator();

      while (var16.hasNext()) {
         SubLevelHoldingChunk holdingChunk = (SubLevelHoldingChunk)var16.next();
         ChunkPos holdingChunkPos = holdingChunk.getChunkPos();

         for (HoldingSubLevel holdingSubLevel : holdingChunk.getLoadedHoldingSubLevels()) {
            if (this.verboseLogging) {
               Sable.LOGGER
                  .info(
                     "Processing holding sub-level {} stored in chunk {} with pointer {}",
                     new Object[]{holdingSubLevel, holdingChunkPos, holdingSubLevel.pointer()}
                  );
            }

            if (holdingSubLevel.pointer() != null && Objects.equals(holdingSubLevel.pointer().chunkPos(), holdingChunkPos)) {
               this.storage.attemptSaveSubLevel(holdingSubLevel.pointer(), holdingSubLevel.data());
            } else {
               if (this.verboseLogging) {
                  Sable.LOGGER.info("Chunk position of holding chunk and pointer mis-match. Moving");
               }

               GlobalSavedSubLevelPointer newPointer = this.moveAndSaveSubLevel(null, holdingSubLevel.data(), holdingSubLevel.pointer(), holdingChunkPos);
               holdingSubLevel.setPointer(newPointer);
            }
         }

         if (!holdingChunk.shouldKeepLoaded()) {
            ChunkHolder chunkHolder = (ChunkHolder)this.level.getChunkSource().chunkMap.visibleChunkMap.get(holdingChunkPos.toLong());
            if (chunkHolder == null || Visibility.fromFullChunkStatus(chunkHolder.getFullStatus()) == Visibility.HIDDEN) {
               this.queuedUnloads.add(holdingChunkPos);
            }
         }
      }

      var16 = this.queuedUnloads.iterator();

      while (var16.hasNext()) {
         ChunkPos unload = (ChunkPos)var16.next();
         SubLevelHoldingChunk holdingChunk = (SubLevelHoldingChunk)this.loadedHoldingChunks.get(unload.toLong());
         if (this.verboseLogging) {
            Sable.LOGGER.info("Processing queued unload for chunk {} at position {}", holdingChunk, holdingChunk != null ? holdingChunk.getChunkPos() : null);
         }

         if (holdingChunk != null) {
            for (HoldingSubLevel holdingSubLevel : holdingChunk.getLoadedHoldingSubLevels()) {
               this.allHoldingSubLevels.remove(holdingSubLevel.data().uuid());
            }

            this.setDirty(unload);
         }
      }

      LongIterator var18 = this.dirtyHoldingChunks.iterator();

      while (var18.hasNext()) {
         long longKey = (Long)var18.next();
         ChunkPos chunkPos = new ChunkPos(longKey);
         SubLevelHoldingChunk holdingChunkx = (SubLevelHoldingChunk)this.loadedHoldingChunks.get(longKey);
         if (this.verboseLogging) {
            Sable.LOGGER.info("Saving holding chunk {} at {}", holdingChunkx, chunkPos);
         }

         if (holdingChunkx != null) {
            this.storage.attemptSaveHoldingChunk(chunkPos, holdingChunkx);
         }
      }

      var16 = this.queuedUnloads.iterator();

      while (var16.hasNext()) {
         ChunkPos unloadx = (ChunkPos)var16.next();
         this.loadedHoldingChunks.remove(unloadx.toLong());
      }

      this.queuedUnloads.clear();

      try {
         if (this.verboseLogging) {
            Sable.LOGGER.info("Flushing storage");
         }

         this.storage.flush();
      } catch (IOException var13) {
         Sable.LOGGER.error("Failed to flush sub-level storage to disk", var13);
      }

      SubLevelTicketsSavedData.getOrLoad(this.level).setDirty();
   }

   private void moveAndSaveSubLevel(ServerSubLevel subLevel, ChunkPos moveToChunk, List<UUID> uuids) {
      GlobalSavedSubLevelPointer lastPointer = subLevel.getLastSerializationPointer();
      SubLevelData data = SubLevelSerializer.toData(subLevel, uuids);
      subLevel.setLastSerializationPointer(this.moveAndSaveSubLevel(subLevel, data, lastPointer, moveToChunk));
      if (this.verboseLogging) {
         Sable.LOGGER.info("Moved sub-level {}. {} -> {}", new Object[]{subLevel, lastPointer, subLevel.getLastSerializationPointer()});
      }
   }

   private GlobalSavedSubLevelPointer moveAndSaveSubLevel(
      @Nullable ServerSubLevel subLevel, SubLevelData data, GlobalSavedSubLevelPointer lastPointer, ChunkPos moveToChunk
   ) {
      ChunkPos oldChunkPos = lastPointer != null ? lastPointer.chunkPos() : null;
      if (Objects.equals(oldChunkPos, moveToChunk)) {
         if (this.getOrLoadHoldingChunk(moveToChunk, false) == null) {
            throw new IllegalStateException("this shouldn't be possible");
         } else {
            if (this.verboseLogging) {
               Sable.LOGGER.info("Old chunk is the same as the new chunk position ({}, {})", oldChunkPos, moveToChunk);
               Sable.LOGGER.info("Saving sub-level data to {}", lastPointer);
            }

            this.storage.attemptSaveSubLevel(lastPointer, data);
            this.setDirty(moveToChunk);
            return lastPointer;
         }
      } else {
         if (this.verboseLogging) {
            Sable.LOGGER.info("Saving sub-level data to storage in new chunk, {}", moveToChunk);
         }

         GlobalSavedSubLevelPointer newPointer = this.storage.attemptSaveSubLevel(moveToChunk, data);
         if (newPointer == null) {
            if (this.level.getServer() instanceof SableToastableServer toastable) {
               toastable.sable$reportSubLevelSaveFailure(data);
            }

            return null;
         } else {
            if (this.verboseLogging) {
               Sable.LOGGER.info("New pointer {}", newPointer);
            }

            SubLevelTrackingPointSavedData trackingPoints = SubLevelTrackingPointSavedData.getOrLoad(this.level);

            for (Entry<UUID, TrackingPoint> entry : trackingPoints.getAllTrackingPoints()) {
               TrackingPoint point = entry.getValue();
               if (point.inSubLevel()) {
                  boolean movingPointers = point.lastSavedSubLevelPointer() != null && point.lastSavedSubLevelPointer().equals(lastPointer);
                  boolean pointerInSubLevel = subLevel != null && Sable.HELPER.getContaining(this.level, point.point()) == subLevel;
                  if (movingPointers || pointerInSubLevel) {
                     trackingPoints.setTrackingPoint(entry.getKey(), new TrackingPoint(true, point.subLevelID(), newPointer, point.point(), null));
                  }
               }
            }

            if (this.verboseLogging) {
               Sable.LOGGER.info("Clearing last pointer (if exists) {}", lastPointer);
            }

            if (oldChunkPos != null) {
               SavedSubLevelPointer localPointer = lastPointer.local();
               SubLevelHoldingChunk oldHoldingChunk = this.getOrLoadHoldingChunk(oldChunkPos, false);
               if (this.verboseLogging) {
                  Sable.LOGGER.info("Removing pointer from last holding chunk {}", oldHoldingChunk);
               }

               if (oldHoldingChunk != null) {
                  oldHoldingChunk.getSubLevelPointers().remove(localPointer);
                  this.setDirty(oldChunkPos);
               } else if (this.verboseLogging) {
                  Sable.LOGGER.info("Old holding chunk doesn't exist at {}! This may be a problem", oldChunkPos);
               }
            }

            if (lastPointer != null) {
               this.storage.attemptSaveSubLevel(lastPointer, null);
            }

            SubLevelHoldingChunk newHoldingChunk = this.getOrLoadHoldingChunk(moveToChunk, true);
            if (this.verboseLogging) {
               Sable.LOGGER.info("Adding pointer to new holding chunk.");
            }

            SavedSubLevelPointer newLocalPointer = newPointer.local();
            newHoldingChunk.getSubLevelPointers().add(newLocalPointer);
            this.setDirty(moveToChunk);
            return newPointer;
         }
      }
   }

   @Contract("_, true -> !null")
   @Nullable
   private SubLevelHoldingChunk getOrLoadHoldingChunk(ChunkPos chunkPos, boolean create) {
      long longKey = chunkPos.toLong();
      SubLevelHoldingChunk holdingChunk = (SubLevelHoldingChunk)this.loadedHoldingChunks.get(longKey);
      if (holdingChunk != null) {
         return holdingChunk;
      } else {
         SubLevelHoldingChunk loadedChunk = this.storage.attemptLoadHoldingChunk(chunkPos);
         if (loadedChunk != null) {
            if (this.verboseLogging) {
               Sable.LOGGER.info("Loaded chunk at {} from disk", chunkPos);
            }

            for (SavedSubLevelPointer pointer : loadedChunk.getSubLevelPointers()) {
               if (this.verboseLogging) {
                  Sable.LOGGER.info("Attempting to read pointer at {} into sub-level data", pointer);
               }

               SubLevelData subLevelData = this.storage.attemptLoadSubLevel(chunkPos, pointer);
               if (subLevelData == null) {
                  Sable.LOGGER
                     .error(
                        "Due to a failed storage sub-level data load, we can't add a holding sub-level for pointer {}. This will cause issues later down the line.",
                        pointer
                     );
               } else {
                  GlobalSavedSubLevelPointer globalPointer = new GlobalSavedSubLevelPointer(chunkPos, pointer.storageIndex(), pointer.subLevelIndex());
                  HoldingSubLevel holdingSubLevel = new HoldingSubLevel(subLevelData, globalPointer);
                  loadedChunk.acceptHoldingSubLevel(holdingSubLevel);
                  this.allHoldingSubLevels.put(holdingSubLevel.data().uuid(), holdingSubLevel);
               }
            }

            this.loadedHoldingChunks.put(longKey, loadedChunk);
            return loadedChunk;
         } else if (create) {
            SubLevelHoldingChunk newHoldingChunk = new SubLevelHoldingChunk(chunkPos);
            this.loadedHoldingChunks.put(longKey, newHoldingChunk);
            return newHoldingChunk;
         } else {
            return null;
         }
      }
   }

   public void snatchAndLoad(GlobalSavedSubLevelPointer pointer, UUID subLevelId) {
      ChunkPos chunkPos = pointer.chunkPos();
      SubLevelHoldingChunk holdingChunk = this.getOrLoadHoldingChunk(chunkPos, false);
      if (holdingChunk == null) {
         Sable.LOGGER
            .error("Attempted to snatch sub-level with ID {} stored at {}, but no holding chunk was present at the pointer chunk position", pointer, subLevelId);
      } else {
         Collection<HoldingSubLevel> holdingSubLevels = holdingChunk.snatch(subLevelId);
         if (holdingSubLevels == null) {
            Sable.LOGGER
               .error(
                  "Attempted to snatch sub-level with ID {} stored at {}, but the requested sub-level wasn't present in the holding chunk", pointer, subLevelId
               );
         } else {
            this.setDirty(chunkPos);

            for (HoldingSubLevel holdingSubLevel : holdingSubLevels) {
               this.loadHoldingSubLevel(holdingSubLevel);
            }
         }
      }
   }

   private void setDirty(ChunkPos chunkPos) {
      if (this.verboseLogging) {
         Sable.LOGGER.info("Setting chunk at {} as dirty", chunkPos);
      }

      this.dirtyHoldingChunks.add(chunkPos.toLong());
   }

   public void processChanges() {
      this.verboseLogging = (Boolean)SableConfig.VERBOSE_SERIALIZATION_LOGGING.get();
      this.processUnloads();
      Object2ObjectMap<UUID, HoldingSubLevel> readySubLevels = new Object2ObjectOpenHashMap();
      ObjectIterator var2 = this.loadedHoldingChunks.values().iterator();

      while (var2.hasNext()) {
         SubLevelHoldingChunk chunk = (SubLevelHoldingChunk)var2.next();
         if (!this.queuedUnloads.contains(chunk.getChunkPos())) {
            chunk.collectReadySubLevels(this.level, readySubLevels);
         }
      }

      var2 = readySubLevels.values().iterator();

      while (var2.hasNext()) {
         HoldingSubLevel holdingSubLevel = (HoldingSubLevel)var2.next();
         if (this.verboseLogging) {
            Sable.LOGGER.info("Holding sub-level {} with pointer {} reportedly ready to load", holdingSubLevel, holdingSubLevel.pointer());
         }

         this.loadHoldingSubLevel(holdingSubLevel);
      }
   }

   @Internal
   public void loadHoldingSubLevel(HoldingSubLevel holdingSubLevel) {
      ServerSubLevel subLevel = SubLevelSerializer.fullyLoad(this.level, holdingSubLevel.data());
      if (subLevel != null) {
         subLevel.setLastSerializationPointer(holdingSubLevel.pointer());
      } else {
         if (this.level.getServer() instanceof SableToastableServer toastable) {
            toastable.sable$reportSubLevelLoadFailure(holdingSubLevel.pointer());
         }

         Sable.LOGGER.info("Failed to load holding sub-level {} with pointer {}. This is a problem.", holdingSubLevel, holdingSubLevel.pointer());
      }

      this.allHoldingSubLevels.remove(holdingSubLevel.data().uuid());
   }

   @Nullable
   public HoldingSubLevel getHoldingSubLevel(UUID uuid) {
      return (HoldingSubLevel)this.allHoldingSubLevels.get(uuid);
   }

   private void processUnloads() {
      Collection<ServerSubLevel> forceLoaded = this.container.collectForceLoadedSubLevels();
      LongIterator var2 = this.chunksToUnload.iterator();

      while (var2.hasNext()) {
         long l = (Long)var2.next();
         this.processUnload(new ChunkPos(l), forceLoaded);
      }

      var2 = this.chunksToLoad.iterator();

      while (var2.hasNext()) {
         long l = (Long)var2.next();
         this.processLoad(new ChunkPos(l));
      }

      this.chunksToUnload.clear();
      this.chunksToLoad.clear();
   }

   public void moveToUnloaded(ServerSubLevel subLevel, ChunkPos pos) {
      if (this.verboseLogging) {
         Sable.LOGGER
            .info("Sub-level {} with pointer {} detected unloaded chunk, moving to {}", new Object[]{subLevel, subLevel.getLastSerializationPointer(), pos});
      }

      Collection<ServerSubLevel> chain = SubLevelHelper.getLoadingDependencyChain(subLevel);
      List<UUID> uuids = chain.stream().map(SubLevel::getUniqueId).toList();
      SubLevelHoldingChunk holdingChunk = this.getOrLoadHoldingChunk(pos, true);

      for (ServerSubLevel chainSubLevel : chain) {
         SubLevelData data = SubLevelSerializer.toData(chainSubLevel, uuids);
         HoldingSubLevel holdingSubLevel = new HoldingSubLevel(data, chainSubLevel.getLastSerializationPointer());
         holdingChunk.acceptHoldingSubLevel(holdingSubLevel);
         this.allHoldingSubLevels.put(holdingSubLevel.data().uuid(), holdingSubLevel);
         if (this.verboseLogging) {
            Sable.LOGGER.info("Added {} to holding chunk {}", chainSubLevel, holdingChunk);
         }

         this.container.removeSubLevel(chainSubLevel, SubLevelRemovalReason.UNLOADED);
      }

      this.setDirty(pos);
   }

   public void queueDeletion(ServerSubLevel subLevel) {
      GlobalSavedSubLevelPointer pointer = subLevel.getLastSerializationPointer();
      if (this.verboseLogging) {
         Sable.LOGGER.info("Queuing sub-level {} with pointer {} for deletion", subLevel, pointer);
      }

      if (pointer != null) {
         ChunkPos chunkPos = pointer.chunkPos();
         SubLevelHoldingChunk holdingChunk = this.getOrLoadHoldingChunk(chunkPos, false);
         if (holdingChunk != null) {
            holdingChunk.getSubLevelPointers().remove(pointer.local());
            this.setDirty(chunkPos);
         }

         this.queuedDeletion.add(pointer);
      }
   }

   public SubLevelStorage getStorage() {
      return this.storage;
   }

   @Override
   public void close() throws Exception {
      this.storage.close();
   }
}
