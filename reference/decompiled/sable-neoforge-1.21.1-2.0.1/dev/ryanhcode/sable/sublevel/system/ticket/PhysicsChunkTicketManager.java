package dev.ryanhcode.sable.sublevel.system.ticket;

import dev.ryanhcode.sable.SableConfig;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.object.ArbitraryPhysicsObject;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

public class PhysicsChunkTicketManager {
   public static final double MAX_PREDICTION_DISTANCE = 20.0;
   public static final TicketType<UUID> SUB_LEVEL_LOADED_TICKET_TYPE = TicketType.create("sable_sub_level_loaded", UUID::compareTo);
   private final Map<SectionPos, PhysicsChunkTicket> physicsChunks = new Object2ObjectOpenHashMap();
   private final Long2ObjectMap<ObjectArraySet<InhabitedChunkTicket>> forcedInhabitedChunks = new Long2ObjectOpenHashMap();

   public void update(ServerLevel level, ServerSubLevelContainer container, SubLevelPhysicsSystem system, PhysicsPipeline pipeline, double timeStep) {
      SubLevelHoldingChunkMap holdingChunkMap = container.getHoldingChunkMap();
      long gameTime = level.getGameTime();
      Collection<ServerSubLevel> forceLoaded = container.collectForceLoadedSubLevels();
      this.expirePhysicsChunkTickets(level, pipeline, gameTime);
      if (!DimensionPhysicsData.of(level).ignoreChunks()) {
         DistanceManager distanceManager = level.getChunkSource().chunkMap.getDistanceManager();
         this.expireForcedInhabitedChunks(gameTime, distanceManager);
         LongOpenHashSet unloadedChunks = new LongOpenHashSet();
         boolean cannotUnloadPlayerInhabited = SableConfig.SUB_LEVELS_WITH_PLAYERS_CANNOT_UNLOAD.getAsBoolean();
         BoundingBox3d b = new BoundingBox3d();
         BoundingBox3d b2 = new BoundingBox3d();
         Vector3d velocity = new Vector3d();
         Iterator<ArbitraryPhysicsObject> objectIter = system.getArbitraryObjects().iterator();

         label165:
         while (objectIter.hasNext()) {
            ArbitraryPhysicsObject arbitraryObject = objectIter.next();
            arbitraryObject.getBoundingBox(b);
            b.expand(1.0, b);
            BoundingBox3i chunkBounds = new BoundingBox3i(
               Mth.floor(b.minX()) >> 4,
               Mth.floor(b.minY()) >> 4,
               Mth.floor(b.minZ()) >> 4,
               Mth.floor(b.maxX()) >> 4,
               Mth.floor(b.maxY()) >> 4,
               Mth.floor(b.maxZ()) >> 4
            );

            for (int x = chunkBounds.minX(); x <= chunkBounds.maxX(); x++) {
               for (int z = chunkBounds.minZ(); z <= chunkBounds.maxZ(); z++) {
                  long l = ChunkPos.asLong(x, z);
                  if (!isChunkLoadedEnough(level, x, z) || unloadedChunks.contains(l)) {
                     arbitraryObject.onUnloaded(holdingChunkMap, new ChunkPos(x, z));
                     unloadedChunks.add(l);
                     objectIter.remove();
                     continue label165;
                  }
               }
            }

            for (int x = chunkBounds.minX(); x <= chunkBounds.maxX(); x++) {
               for (int zx = chunkBounds.minZ(); zx <= chunkBounds.maxZ(); zx++) {
                  for (int y = chunkBounds.minY(); y <= chunkBounds.maxY(); y++) {
                     SectionPos sectionPos = SectionPos.of(x, y, zx);
                     int index = level.getSectionIndexFromSectionY(y);
                     if (index >= 0 && index < level.getSectionsCount()) {
                        this.addTicket(level, pipeline, sectionPos, x, y, zx, index, gameTime);
                     }
                  }
               }
            }
         }

         label128:
         for (int i = 0; i < container.getAllSubLevels().size(); i++) {
            ServerSubLevel subLevel = container.getAllSubLevels().get(i);
            if (!subLevel.isRemoved()) {
               UUID uuid = subLevel.getUniqueId();
               b.set(subLevel.boundingBox());
               b2.set(b);
               if (subLevel.lastPose().position().distanceSquared(subLevel.logicalPose().position()) > 0.0025000000000000005) {
                  system.getPipeline().getLinearVelocity(subLevel, velocity.zero()).mul(timeStep);
                  b2.move(0.0, Mth.clamp(velocity.y, -20.0, 20.0), 0.0);
                  b.expandTo(b2);
               }

               b.expand(1.0, b);
               BoundingBox3i chunkBounds = b.chunkBoundsFrom();
               boolean checkedPlayerInhabited = false;
               boolean playerInhabited = false;

               for (int x = chunkBounds.minX(); x <= chunkBounds.maxX(); x++) {
                  for (int zx = chunkBounds.minZ(); zx <= chunkBounds.maxZ(); zx++) {
                     long chunkLong = ChunkPos.asLong(x, zx);
                     boolean chunkLoadedEnough = isChunkLoadedEnough(level, x, zx);
                     if (forceLoaded.contains(subLevel)) {
                        this.inhabitChunk(level, distanceManager, uuid, gameTime, chunkLong, x, zx);
                     } else if (!chunkLoadedEnough || unloadedChunks.contains(chunkLong)) {
                        unloadedChunks.add(chunkLong);
                        if (!checkedPlayerInhabited && cannotUnloadPlayerInhabited) {
                           playerInhabited = !level.getPlayers(player -> {
                              Vec3 position = player.getBoundingBox().getCenter();
                              return b.contains(position.x, position.y, position.z);
                           }).isEmpty();
                           checkedPlayerInhabited = true;
                        }

                        if (!cannotUnloadPlayerInhabited || !playerInhabited) {
                           holdingChunkMap.moveToUnloaded(subLevel, new ChunkPos(x, zx));
                           i--;
                           continue label128;
                        }

                        this.inhabitChunk(level, distanceManager, uuid, gameTime, chunkLong, x, zx);
                     }
                  }
               }

               for (int x = chunkBounds.minX(); x <= chunkBounds.maxX(); x++) {
                  for (int zxx = chunkBounds.minZ(); zxx <= chunkBounds.maxZ(); zxx++) {
                     for (int yx = chunkBounds.minY(); yx <= chunkBounds.maxY(); yx++) {
                        SectionPos sectionPos = SectionPos.of(x, yx, zxx);
                        int index = level.getSectionIndexFromSectionY(yx);
                        if (index >= 0 && index < level.getSectionsCount()) {
                           PhysicsChunkTicket var29 = this.addTicket(level, pipeline, sectionPos, x, yx, zxx, index, gameTime);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void inhabitChunk(ServerLevel level, DistanceManager distanceManager, UUID subLevelId, long gameTime, long chunkLong, int x, int z) {
      ObjectArraySet<InhabitedChunkTicket> set = (ObjectArraySet<InhabitedChunkTicket>)this.forcedInhabitedChunks.get(chunkLong);
      if (set == null) {
         this.forcedInhabitedChunks.put(chunkLong, set = new ObjectArraySet(1));
      }

      Ticket<UUID> newChunkTicket = new Ticket(SUB_LEVEL_LOADED_TICKET_TYPE, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING), subLevelId);
      InhabitedChunkTicket newSableTicket = new InhabitedChunkTicket(subLevelId, gameTime, newChunkTicket);
      if (set.add(newSableTicket)) {
         distanceManager.addTicket(chunkLong, newChunkTicket);
         distanceManager.tickingTicketsTracker.addTicket(chunkLong, newChunkTicket);
         level.getChunk(x, z, ChunkStatus.FULL, true);
      } else {
         boolean any = false;
         ObjectIterator var14 = set.iterator();

         while (var14.hasNext()) {
            InhabitedChunkTicket ticket = (InhabitedChunkTicket)var14.next();
            if (ticket.equals(newSableTicket)) {
               ticket.setLastInhabitedTick(gameTime);
               any = true;
               break;
            }
         }

         if (!any) {
            throw new RuntimeException("Chunk ticket state management has gone horribly wrong.");
         }
      }
   }

   private void expirePhysicsChunkTickets(ServerLevel level, PhysicsPipeline pipeline, long gameTime) {
      Iterator<Entry<SectionPos, PhysicsChunkTicket>> chunkTicketIter = this.physicsChunks.entrySet().iterator();

      while (chunkTicketIter.hasNext()) {
         Entry<SectionPos, PhysicsChunkTicket> entry = chunkTicketIter.next();
         SectionPos sectionPos = entry.getKey();
         PhysicsChunkTicket ticket = entry.getValue();
         LevelPlot plot = SubLevelContainer.getContainer(level).getPlot(sectionPos.chunk());
         boolean outdated = ticket.lastInhabitedTick() < gameTime - 20L && plot == null;
         boolean noLongerExistent = !isChunkLoadedEnough(level, sectionPos.x(), sectionPos.z());
         if (outdated || noLongerExistent) {
            pipeline.handleChunkSectionRemoval(sectionPos.x(), sectionPos.y(), sectionPos.z());
            chunkTicketIter.remove();
         }
      }
   }

   private void expireForcedInhabitedChunks(long gameTime, DistanceManager distanceManager) {
      ObjectIterator<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<ObjectArraySet<InhabitedChunkTicket>>> forcedChunkIter = this.forcedInhabitedChunks
         .long2ObjectEntrySet()
         .iterator();

      while (forcedChunkIter.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<ObjectArraySet<InhabitedChunkTicket>> entry = (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<ObjectArraySet<InhabitedChunkTicket>>)forcedChunkIter.next();
         long chunkLong = entry.getLongKey();
         ObjectArraySet<InhabitedChunkTicket> set = (ObjectArraySet<InhabitedChunkTicket>)entry.getValue();
         Iterator<InhabitedChunkTicket> setIter = set.iterator();

         while (setIter.hasNext()) {
            InhabitedChunkTicket ticket = setIter.next();
            boolean outdated = ticket.lastInhabitedTick() < gameTime - 20L;
            if (outdated) {
               Ticket<UUID> chunkTicket = ticket.getTicket();
               distanceManager.removeTicket(chunkLong, chunkTicket);
               distanceManager.tickingTicketsTracker.removeTicket(chunkLong, chunkTicket);
               setIter.remove();
            }
         }

         if (set.isEmpty()) {
            forcedChunkIter.remove();
         }
      }
   }

   @NotNull
   private PhysicsChunkTicket addTicket(Level level, PhysicsPipeline pipeline, SectionPos sectionPos, int x, int y, int z, int index, long gameTime) {
      PhysicsChunkTicket existingTicket = this.physicsChunks.get(sectionPos);
      if (existingTicket == null) {
         LevelChunk chunk = level.getChunk(x, z);
         pipeline.handleChunkSectionAddition(chunk.getSection(index), x, y, z, false);
         Collection<SubLevel> residents = null;
         PhysicsChunkTicket newTicket = new PhysicsChunkTicket(sectionPos, gameTime, residents);
         this.physicsChunks.put(sectionPos, newTicket);
         existingTicket = newTicket;
      }

      existingTicket.setLastInhabitedTick(gameTime);
      return existingTicket;
   }

   public void addSectionIfNotTracked(ServerLevel level, LevelChunkSection section, SectionPos sectionPos, PhysicsPipeline pipeline) {
      if (!this.physicsChunks.containsKey(sectionPos)) {
         pipeline.handleChunkSectionAddition(section, sectionPos.x(), sectionPos.y(), sectionPos.z(), false);
         PhysicsChunkTicket ticket = new PhysicsChunkTicket(sectionPos, level.getGameTime(), null);
         this.physicsChunks.put(sectionPos, ticket);
      }
   }

   public void addTicketForSection(ServerLevel level, SectionPos sectionPos) {
      PhysicsChunkTicket ticket = new PhysicsChunkTicket(sectionPos, level.getGameTime(), null);
      this.physicsChunks.put(sectionPos, ticket);
   }

   public Iterable<SubLevel> queryIntersecting(BoundingBox3dc bounds) {
      throw new IllegalStateException("Cannot query intersecting sub-levels when tickets are not used for queries.");
   }

   public boolean wouldBeLoaded(Level level, ArbitraryPhysicsObject object) {
      BoundingBox3d b = new BoundingBox3d();
      object.getBoundingBox(b);
      b.expand(1.0, b);
      BoundingBox3i chunkBounds = b.chunkBoundsFrom();

      for (int x = chunkBounds.minX(); x <= chunkBounds.maxX(); x++) {
         for (int z = chunkBounds.minZ(); z <= chunkBounds.maxZ(); z++) {
            if (!isChunkLoadedEnough((ServerLevel)level, x, z)) {
               return false;
            }
         }
      }

      return true;
   }

   public static boolean isChunkLoadedEnough(ServerLevel level, int x, int z) {
      ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
      if (container != null && container.inBounds(x, z)) {
         return true;
      } else {
         DistanceManager distanceManager = level.getChunkSource().chunkMap.getDistanceManager();
         return distanceManager.inBlockTickingRange(ChunkPos.asLong(x, z));
      }
   }
}
