package dev.ryanhcode.sable.sublevel.storage.holding;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class SubLevelHoldingChunk {
   private final ObjectList<HoldingSubLevel> alsoLoad = new ObjectArrayList();
   private final ObjectList<SavedSubLevelPointer> pointers = new ObjectArrayList();
   private final Object2ObjectMap<UUID, HoldingSubLevel> loadedHoldingSubLevels = new Object2ObjectOpenHashMap();
   private final ChunkPos pos;
   final ObjectOpenHashSet<UUID> visitedSet = new ObjectOpenHashSet();
   private boolean keepLoaded = false;

   public SubLevelHoldingChunk(ChunkPos pos) {
      this.pos = pos;
   }

   public void acceptHoldingSubLevel(HoldingSubLevel subLevelData) {
      this.loadedHoldingSubLevels.put(subLevelData.data().uuid(), subLevelData);
   }

   public Iterable<HoldingSubLevel> getLoadedHoldingSubLevels() {
      return this.loadedHoldingSubLevels.values();
   }

   @Nullable
   protected Collection<HoldingSubLevel> snatch(UUID subLevelId) {
      HoldingSubLevel holdingSubLevel = (HoldingSubLevel)this.loadedHoldingSubLevels.remove(subLevelId);
      if (holdingSubLevel == null) {
         return null;
      } else {
         SubLevelData data = holdingSubLevel.data();
         List<UUID> relations = data.dependencies();
         ObjectList<HoldingSubLevel> snatchedSubLevels = new ObjectArrayList();
         snatchedSubLevels.add(holdingSubLevel);

         for (UUID uuid : relations) {
            HoldingSubLevel dependencySubLevel = (HoldingSubLevel)this.loadedHoldingSubLevels.remove(uuid);
            if (dependencySubLevel == null) {
               Sable.LOGGER
                  .error("Sub-level dependency does not exist in chunk when loading force-loaded holding sub-level. Something has gone terribly wrong.");
            } else {
               snatchedSubLevels.add(dependencySubLevel);
            }
         }

         return snatchedSubLevels;
      }
   }

   public void collectReadySubLevels(ServerLevel level, Object2ObjectMap<UUID, HoldingSubLevel> readySubLevels) {
      if (!this.loadedHoldingSubLevels.isEmpty()) {
         this.visitedSet.clear();
         Iterator<Entry<UUID, HoldingSubLevel>> iter = this.loadedHoldingSubLevels.entrySet().iterator();

         label59:
         while (iter.hasNext()) {
            Entry<UUID, HoldingSubLevel> entry = iter.next();
            if (!this.visitedSet.contains(entry.getKey())) {
               HoldingSubLevel holdingSubLevel = entry.getValue();
               SubLevelData data = holdingSubLevel.data();
               List<UUID> relations = data.dependencies();
               this.visitedSet.add(entry.getKey());
               this.visitedSet.addAll(relations);

               for (UUID uuid : relations) {
                  HoldingSubLevel dependencySubLevel = (HoldingSubLevel)this.loadedHoldingSubLevels.get(uuid);
                  if (dependencySubLevel == null) {
                     Sable.LOGGER.error("Sub-level dependency does not exist in chunk. Something has gone terribly wrong.");
                     iter.remove();
                     continue label59;
                  }

                  if (!canLoadSubLevel(level, dependencySubLevel.data())) {
                     continue label59;
                  }
               }

               boolean allChunksLoaded = canLoadSubLevel(level, data);
               if (allChunksLoaded) {
                  readySubLevels.put(data.uuid(), holdingSubLevel);
                  iter.remove();

                  for (UUID uuid : relations) {
                     HoldingSubLevel dependencySubLevelx = (HoldingSubLevel)this.loadedHoldingSubLevels.get(uuid);
                     if (dependencySubLevelx != null) {
                        this.alsoLoad.add(dependencySubLevelx);
                     }
                  }
               }
            }
         }

         ObjectListIterator var12 = this.alsoLoad.iterator();

         while (var12.hasNext()) {
            HoldingSubLevel holdingSubLevel = (HoldingSubLevel)var12.next();
            UUID uuidx = holdingSubLevel.data().uuid();
            this.loadedHoldingSubLevels.remove(uuidx);
            readySubLevels.put(uuidx, holdingSubLevel);
         }

         this.alsoLoad.clear();
      }
   }

   private static boolean canLoadSubLevel(ServerLevel level, SubLevelData data) {
      BoundingBox3dc bounds = data.bounds();
      BoundingBox3i chunkBounds = new BoundingBox3i(
         Mth.floor(bounds.minX() - 1.0) >> 4,
         Mth.floor(bounds.minY() - 1.0) >> 4,
         Mth.floor(bounds.minZ() - 1.0) >> 4,
         Mth.floor(bounds.maxX() + 1.0) >> 4,
         Mth.floor(bounds.maxY() + 1.0) >> 4,
         Mth.floor(bounds.maxZ() + 1.0) >> 4
      );
      boolean allChunksLoaded = true;

      for (int x = chunkBounds.minX(); x <= chunkBounds.maxX(); x++) {
         for (int z = chunkBounds.minZ(); z <= chunkBounds.maxZ(); z++) {
            if (!PhysicsChunkTicketManager.isChunkLoadedEnough(level, x, z)) {
               allChunksLoaded = false;
               return allChunksLoaded;
            }
         }
      }

      return allChunksLoaded;
   }

   public static SubLevelHoldingChunk from(ChunkPos pos, CompoundTag tag) {
      SubLevelHoldingChunk chunk = new SubLevelHoldingChunk(pos);
      int[] pointer = tag.getIntArray("pointers");
      chunk.pointers.addAll(Arrays.stream(pointer).mapToObj(SavedSubLevelPointer::unpack).toList());
      return chunk;
   }

   public void writeTo(CompoundTag tag) {
      int[] pointers = this.pointers.stream().mapToInt(SavedSubLevelPointer::packed).toArray();
      tag.putIntArray("pointers", pointers);
   }

   public ChunkPos getChunkPos() {
      return this.pos;
   }

   public List<SavedSubLevelPointer> getSubLevelPointers() {
      return this.pointers;
   }

   public void markKeepLoaded() {
      this.keepLoaded = true;
   }

   public boolean shouldKeepLoaded() {
      return this.keepLoaded;
   }

   @Override
   public String toString() {
      return "SubLevelHoldingChunk{pos=" + this.pos + ", keepLoaded=" + this.keepLoaded + ", pointers=" + this.pointers + "}";
   }
}
