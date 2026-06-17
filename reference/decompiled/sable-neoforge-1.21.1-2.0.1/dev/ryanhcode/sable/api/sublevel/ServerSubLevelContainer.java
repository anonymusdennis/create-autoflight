package dev.ryanhcode.sable.api.sublevel;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableConfig;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicket;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelTicketInfo;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelOccupancySavedData;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.SubLevelTicketsSavedData;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector3d;

public class ServerSubLevelContainer extends SubLevelContainer {
   @Nullable
   private SubLevelPhysicsSystem physics;
   @Nullable
   private SubLevelTrackingSystem tracking;
   private SubLevelHoldingChunkMap holdingChunkMap;
   protected final Object2ObjectMap<ServerSubLevel, ObjectSet<SubLevelLoadingTicket<?>>> activeTickets = new Object2ObjectOpenHashMap();
   protected final Object2ObjectMap<UUID, SubLevelTicketInfo> allTickets = new Object2ObjectOpenHashMap();

   public ServerSubLevelContainer(Level level, int logSideLength, int logPlotSize, int originX, int originZ) {
      super(level, logSideLength, logPlotSize, originX, originZ);
   }

   public void initialize() {
      this.holdingChunkMap = new SubLevelHoldingChunkMap(this.getLevel(), this);
      this.loadForceLoadedSubLevels();
   }

   @Override
   public void tick() {
      super.tick();
      this.holdingChunkMap.processChanges();
   }

   @Internal
   public void takePhysicsSystem(SubLevelPhysicsSystem physics) {
      this.physics = physics;
   }

   @Internal
   public void takeTrackingSystem(SubLevelTrackingSystem tracking) {
      this.tracking = tracking;
   }

   @NotNull
   public SubLevelPhysicsSystem physicsSystem() {
      assert this.physics != null;

      return this.physics;
   }

   @NotNull
   public SubLevelTrackingSystem trackingSystem() {
      assert this.tracking != null;

      return this.tracking;
   }

   @Override
   public void removeSubLevel(int x, int z, SubLevelRemovalReason reason) {
      ServerSubLevel subLevel = (ServerSubLevel)this.getSubLevel(x, z);
      if (subLevel == null) {
         throw new IllegalStateException("No sub-level at " + x + ", " + z);
      } else {
         if (reason == SubLevelRemovalReason.REMOVED) {
            subLevel.deleteAllEntities();
         }

         super.removeSubLevel(x, z, reason);
         if (reason == SubLevelRemovalReason.REMOVED) {
            ServerLevel level = this.getLevel();
            SubLevelOccupancySavedData.getOrLoad(level).setDirty();
            this.holdingChunkMap.queueDeletion(subLevel);
         }
      }
   }

   @Override
   protected SubLevel createSubLevel(int globalPlotX, int globalPlotZ, Pose3d pose, UUID uuid) {
      ServerLevel level = this.getLevel();
      ServerSubLevel subLevel = new ServerSubLevel(level, globalPlotX, globalPlotZ, pose);
      subLevel.setUniqueId(uuid);
      Vector3d position = pose.position();
      BlockPos blockPos = BlockPos.containing(position.x, position.y, position.z);
      if (level.isLoaded(blockPos)) {
         Holder<Biome> holder = level.getBiome(blockPos);
         Optional<ResourceKey<Biome>> key = holder.unwrapKey();
         if (key.isPresent()) {
            subLevel.getPlot().setBiome(key.get());
         }
      }

      return subLevel;
   }

   public SubLevelHoldingChunkMap getHoldingChunkMap() {
      return this.holdingChunkMap;
   }

   @Override
   public List<ServerSubLevel> getAllSubLevels() {
      return super.getAllSubLevels();
   }

   public ServerLevel getLevel() {
      return (ServerLevel)super.getLevel();
   }

   public <T> boolean addForceLoadTicket(ServerSubLevel subLevel, SubLevelLoadingTicketType<T> ticketType, T key) {
      UUID uuid = subLevel.getUniqueId();
      SubLevelLoadingTicket<T> ticket = new SubLevelLoadingTicket<>(ticketType, uuid, key);
      ObjectSet<SubLevelLoadingTicket<?>> loadedSet = (ObjectSet<SubLevelLoadingTicket<?>>)this.activeTickets
         .computeIfAbsent(subLevel, ignored -> new ObjectArraySet());
      SubLevelTicketInfo allSet = (SubLevelTicketInfo)this.allTickets.computeIfAbsent(uuid, ignored -> new SubLevelTicketInfo());
      loadedSet.add(ticket);
      if (allSet.tickets().add(ticket)) {
         SubLevelTicketsSavedData.getOrLoad(this.getLevel()).setDirty();
         return true;
      } else {
         return false;
      }
   }

   public <T> boolean removeForceLoadTicket(ServerSubLevel subLevel, SubLevelLoadingTicketType<T> ticketType, T key) {
      UUID uuid = subLevel.getUniqueId();
      SubLevelLoadingTicket<T> ticket = new SubLevelLoadingTicket<>(ticketType, uuid, key);
      ObjectSet<SubLevelLoadingTicket<?>> loadedSet = (ObjectSet<SubLevelLoadingTicket<?>>)this.activeTickets.get(subLevel);
      SubLevelTicketInfo allSet = (SubLevelTicketInfo)this.allTickets.get(subLevel.getUniqueId());
      if (loadedSet != null) {
         loadedSet.remove(ticket);
         if (loadedSet.isEmpty()) {
            this.activeTickets.remove(subLevel);
         }
      }

      if (allSet != null) {
         boolean existed = allSet.tickets().remove(ticket);
         if (allSet.tickets().isEmpty()) {
            this.allTickets.remove(subLevel.getUniqueId());
         }

         if (existed) {
            SubLevelTicketsSavedData.getOrLoad(this.getLevel()).setDirty();
            return true;
         }
      }

      return false;
   }

   public Collection<ServerSubLevel> collectForceLoadedSubLevels() {
      if (this.activeTickets.isEmpty()) {
         return List.of();
      } else {
         ObjectOpenHashSet<ServerSubLevel> subLevels = new ObjectOpenHashSet();
         ObjectIterator var2 = this.activeTickets.keySet().iterator();

         while (var2.hasNext()) {
            ServerSubLevel subLevel = (ServerSubLevel)var2.next();
            if (!subLevels.contains(subLevel)) {
               subLevels.addAll(SubLevelHelper.getLoadingDependencyChain(subLevel));
            }
         }

         return subLevels;
      }
   }

   @Internal
   public void loadTickets(Object2ObjectMap<UUID, SubLevelTicketInfo> tickets) {
      this.allTickets.putAll(tickets);
   }

   @Internal
   public Map<UUID, SubLevelTicketInfo> getAllTickets() {
      return Collections.unmodifiableMap(this.allTickets);
   }

   private void loadForceLoadedSubLevels() {
      ObjectIterator var1 = this.allTickets.entrySet().iterator();

      while (var1.hasNext()) {
         Entry<UUID, SubLevelTicketInfo> entry = (Entry<UUID, SubLevelTicketInfo>)var1.next();
         UUID uuid = entry.getKey();
         GlobalSavedSubLevelPointer pointer = entry.getValue().getPointer();
         if (pointer != null) {
            this.holdingChunkMap.snatchAndLoad(pointer, uuid);
         } else {
            Sable.LOGGER.error("Cannot load force-loaded sub-level with ID {} because the ticket info was not saved with a pointer", uuid);
         }
      }
   }

   @Internal
   public void close() {
      List<ServerSubLevel> subLevels = new ObjectArrayList(this.getAllSubLevels());
      if (!subLevels.isEmpty()) {
         Map<UUID, SubLevelTicketInfo> tickets = this.getAllTickets();

         for (ServerSubLevel subLevel : subLevels) {
            if ((Boolean)SableConfig.VERBOSE_SERIALIZATION_LOGGING.get() && !tickets.containsKey(subLevel.getUniqueId())) {
               Sable.LOGGER.error("Sub-level {} was present after world closing, but is not force-loaded.", subLevel);
            }

            this.removeSubLevel(subLevel, SubLevelRemovalReason.UNLOADED);
         }
      }

      if (this.physics != null) {
         this.physics.getPipeline().dispose();
      }

      try {
         this.holdingChunkMap.close();
      } catch (Exception var5) {
         Sable.LOGGER.error("Failed closing sub-level holding chunk map", var5);
      }
   }
}
