package dev.ryanhcode.sable.sublevel.system;

import dev.ryanhcode.sable.SableConfig;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.api.sublevel.SubLevelTrackingPlugin;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.network.packets.ClientboundSableSnapshotDualPacket;
import dev.ryanhcode.sable.network.packets.ClientboundSableSnapshotInfoDualPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundChangeBoundsSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFinalizeSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundRecentlySplitSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundStartTrackingSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundStopMovingSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundStopTrackingSubLevelPacket;
import dev.ryanhcode.sable.network.udp.SableUDPServer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.plot.SubLevelPlayerChunkSender;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import foundry.veil.api.network.VeilPacketManager;
import foundry.veil.api.network.VeilPacketManager.PacketSink;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

public class SubLevelTrackingSystem implements SubLevelObserver {
   private final ServerLevel level;
   private final List<SubLevel> additionQueue = new ObjectArrayList();
   private final Set<UUID> currentlyUpdatingPlayers = new ObjectOpenHashSet();
   private final Set<UUID> pluginNeededPlayers = new ObjectOpenHashSet();
   private final List<SubLevelTrackingPlugin> plugins = new ObjectArrayList();
   private int interpolationTick;
   private long lastSendMs = -1L;

   public SubLevelTrackingSystem(ServerLevel level) {
      this.level = level;
   }

   private static long getSubLevelLong(ServerSubLevel subLevel, SubLevelContainer subLevels) {
      Vector2i origin = subLevels.getOrigin();
      ChunkPos plotPos = subLevel.getPlot().plotPos;
      return ChunkPos.asLong(plotPos.x - origin.x, plotPos.z - origin.y);
   }

   private boolean shouldLoad(Player player, Vector3dc entityPosition) {
      double trackingRange = SableConfig.SUB_LEVEL_TRACKING_RANGE.getAsDouble();
      return entityPosition.distanceSquared(player.getX(), player.getY(), player.getZ()) < trackingRange * trackingRange;
   }

   @Override
   public void onSubLevelAdded(SubLevel subLevel) {
      this.additionQueue.add(subLevel);
   }

   @Override
   public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
      this.additionQueue.remove(subLevel);
      ServerSubLevel serverSubLevel = (ServerSubLevel)subLevel;
      this.sendRemoval(this.serverWidePlayerSink(serverSubLevel), serverSubLevel);
   }

   public PacketSink serverWidePlayerSink(ServerSubLevel serverSubLevel) {
      return packet -> {
         for (UUID uuid : serverSubLevel.getTrackingPlayers()) {
            ServerPlayer player = this.level.getServer().getPlayerList().getPlayer(uuid);
            if (player instanceof ServerPlayer) {
               player.connection.send(packet);
            }
         }
      };
   }

   private void collectPlayers(Vector3d position, Collection<UUID> tracking) {
      for (ServerPlayer player : this.level.players()) {
         if (this.shouldLoad(player, position)) {
            tracking.add(player.getGameProfile().getId());
         }
      }
   }

   private void sendFullSync(ServerPlayer player, ServerSubLevel subLevel, @Nullable CustomPacketPayload extraPacket) {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);

      assert container != null;

      long l = getSubLevelLong(subLevel, container);
      LevelPlot plot = subLevel.getPlot();
      Collection<PlotChunkHolder> chunks = plot.getLoadedChunks();
      ObjectList<Packet<? super ClientGamePacketListener>> packets = new ObjectArrayList(3 + chunks.size());
      packets.add(
         new ClientboundCustomPayloadPacket(
            new ClientboundStartTrackingSubLevelPacket(
               l, subLevel.getUniqueId(), subLevel.lastPose(), subLevel.logicalPose(), plot.getBoundingBox(), subLevel.getName(), this.interpolationTick
            )
         )
      );
      if (extraPacket != null) {
         packets.add(new ClientboundCustomPayloadPacket(extraPacket));
      }

      for (PlotChunkHolder chunk : chunks) {
         SubLevelPlayerChunkSender.sendChunk(packets::add, plot.getLightEngine(), chunk.getChunk());
      }

      packets.add(new ClientboundCustomPayloadPacket(new ClientboundFinalizeSubLevelPacket(l)));
      player.connection.send(new ClientboundBundlePacket(packets));

      for (PlotChunkHolder chunk : chunks) {
         SubLevelPlayerChunkSender.sendChunkPoiData(this.level, chunk.getChunk());
      }
   }

   private void sendRemoval(PacketSink sink, ServerSubLevel subLevel) {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);

      assert container != null;

      long l = getSubLevelLong(subLevel, container);
      sink.sendPacket(new CustomPacketPayload[]{new ClientboundStopTrackingSubLevelPacket(l)});
   }

   @Override
   public void tick(SubLevelContainer container) {
      for (SubLevel subLevel : this.additionQueue) {
         if (!subLevel.isRemoved()) {
            ServerSubLevel serverSubLevel = (ServerSubLevel)subLevel;
            Collection<UUID> tracking = serverSubLevel.getTrackingPlayers();
            Vector3d position = subLevel.logicalPose().position();
            this.collectPlayers(position, tracking);
            UUID splitFromSubLevelID = serverSubLevel.getSplitFromSubLevel();
            SubLevel splitFromSubLevel = splitFromSubLevelID != null ? container.getSubLevel(splitFromSubLevelID) : null;

            for (UUID uuid : tracking) {
               ServerPlayer player = (ServerPlayer)this.level.getPlayerByUUID(uuid);
               if (player == null) {
                  throw new IllegalStateException("Player not found immediately after tracking initializes");
               }

               CustomPacketPayload extraPacket = null;
               if (splitFromSubLevelID != null && splitFromSubLevel != null) {
                  extraPacket = new ClientboundRecentlySplitSubLevelPacket(
                     serverSubLevel.getUniqueId(), splitFromSubLevel.getUniqueId(), serverSubLevel.getSplitFromPose()
                  );
               }

               this.sendFullSync(player, serverSubLevel, extraPacket);
            }

            serverSubLevel.clearSplitFrom();
         }
      }

      this.additionQueue.clear();

      for (SubLevel subLevelx : container.getAllSubLevels()) {
         if (!subLevelx.isRemoved()) {
            ServerSubLevel serverSubLevel = (ServerSubLevel)subLevelx;
            Collection<UUID> tracking = serverSubLevel.getTrackingPlayers();
            Vector3dc entityPos = subLevelx.logicalPose().position();
            Iterator<UUID> iter = tracking.iterator();

            while (iter.hasNext()) {
               UUID uuid = iter.next();
               ServerPlayer playerx = (ServerPlayer)this.level.getPlayerByUUID(uuid);
               if (playerx == null) {
                  ServerPlayer serverWidePlayer = this.level.getServer().getPlayerList().getPlayer(uuid);
                  if (serverWidePlayer != null) {
                     this.sendRemoval(VeilPacketManager.player(serverWidePlayer), serverSubLevel);
                  }

                  iter.remove();
               } else if (!this.shouldLoad(playerx, entityPos)) {
                  this.sendRemoval(VeilPacketManager.player(playerx), serverSubLevel);
                  iter.remove();
               }
            }

            for (ServerPlayer playerx : this.level.players()) {
               UUID uuid = playerx.getGameProfile().getId();
               if (this.shouldLoad(playerx, entityPos) && !tracking.contains(uuid)) {
                  tracking.add(uuid);
                  this.sendFullSync(playerx, serverSubLevel, null);
               }
            }
         }
      }

      this.sendBoundsUpdates(container);
      this.sendMovementUpdates(container);
   }

   private void sendBoundsUpdates(SubLevelContainer container) {
      for (SubLevel subLevel : container.getAllSubLevels()) {
         if (!subLevel.isRemoved()) {
            ServerSubLevel serverSubLevel = (ServerSubLevel)subLevel;
            BoundingBox3ic plotBounds = serverSubLevel.getPlot().getBoundingBox();
            BoundingBox3i lastNetworkedBounds = serverSubLevel.lastNetworkedBoundingBox();
            if (!plotBounds.equals(lastNetworkedBounds)) {
               lastNetworkedBounds.set(plotBounds);
               long l = getSubLevelLong(serverSubLevel, container);
               serverSubLevel.playerSink().sendPacket(new CustomPacketPayload[]{new ClientboundChangeBoundsSubLevelPacket(l, plotBounds)});
            }
         }
      }
   }

   public int getInterpolationTick() {
      return this.interpolationTick;
   }

   private void sendMovementUpdates(SubLevelContainer container) {
      Map<UUID, List<SubLevelTrackingSystem.SubLevelUpdateTicket>> movementUpdates = new Object2ObjectOpenHashMap();
      Iterator ms = container.getAllSubLevels().iterator();

      while (true) {
         ServerSubLevel serverSubLevel;
         Collection<UUID> tracking;
         SubLevelTrackingSystem.SubLevelUpdateTicket.UpdateTicketType type;
         while (true) {
            if (!ms.hasNext()) {
               long msx = System.currentTimeMillis();
               int msSinceLastSend;
               if (this.lastSendMs == -1L) {
                  msSinceLastSend = (int)(1000.0 / (double)this.level.getServer().tickRateManager().tickrate());
               } else {
                  msSinceLastSend = (int)(msx - this.lastSendMs);
               }

               this.lastSendMs = msx;
               this.pluginNeededPlayers.clear();

               for (SubLevelTrackingPlugin plugin : this.plugins) {
                  for (UUID neededPlayer : plugin.neededPlayers()) {
                     this.pluginNeededPlayers.add(neededPlayer);
                  }
               }

               this.currentlyUpdatingPlayers.addAll(movementUpdates.keySet());
               this.currentlyUpdatingPlayers.addAll(this.pluginNeededPlayers);
               Iterator<UUID> currentlyUpdatingIter = this.currentlyUpdatingPlayers.iterator();

               while (currentlyUpdatingIter.hasNext()) {
                  UUID uuid = currentlyUpdatingIter.next();
                  ServerPlayer player = (ServerPlayer)this.level.getPlayerByUUID(uuid);
                  if (player == null) {
                     currentlyUpdatingIter.remove();
                  } else if (!movementUpdates.containsKey(uuid)) {
                     if (this.pluginNeededPlayers.contains(uuid)) {
                        player.connection
                           .send(new ClientboundCustomPayloadPacket(new ClientboundSableSnapshotInfoDualPacket(msSinceLastSend, this.interpolationTick, false)));
                     } else {
                        player.connection
                           .send(new ClientboundCustomPayloadPacket(new ClientboundSableSnapshotInfoDualPacket(msSinceLastSend, this.interpolationTick, true)));
                        currentlyUpdatingIter.remove();
                     }
                  }
               }

               for (SubLevelTrackingPlugin plugin : this.plugins) {
                  plugin.sendTrackingData(this.interpolationTick);
               }

               for (Entry<UUID, List<SubLevelTrackingSystem.SubLevelUpdateTicket>> entry : movementUpdates.entrySet()) {
                  UUID uuid = entry.getKey();
                  ServerPlayer player = (ServerPlayer)this.level.getPlayerByUUID(uuid);
                  List<SubLevelTrackingSystem.SubLevelUpdateTicket> toUpdate = entry.getValue();
                  List<ClientboundSableSnapshotDualPacket.Entry> entries = new ObjectArrayList();

                  for (SubLevelTrackingSystem.SubLevelUpdateTicket ticket : toUpdate) {
                     ServerSubLevel serverSubLevelx = (ServerSubLevel)ticket.subLevels;
                     long l = getSubLevelLong(serverSubLevelx, container);
                     switch (ticket.type) {
                        case STOP:
                           player.connection.send(new ClientboundCustomPayloadPacket(new ClientboundStopMovingSubLevelPacket(l)));
                           break;
                        case MOVE:
                           Vector3f linearVelocity = new Vector3f(
                              (float)serverSubLevelx.latestLinearVelocity.x,
                              (float)serverSubLevelx.latestLinearVelocity.y,
                              (float)serverSubLevelx.latestLinearVelocity.z
                           );
                           Vector3f angularVelocity = new Vector3f(
                              (float)serverSubLevelx.latestAngularVelocity.x,
                              (float)serverSubLevelx.latestAngularVelocity.y,
                              (float)serverSubLevelx.latestAngularVelocity.z
                           );
                           entries.add(new ClientboundSableSnapshotDualPacket.Entry(l, serverSubLevelx.logicalPose(), linearVelocity, angularVelocity));
                     }
                  }

                  int maxBatchSize = 16;
                  SableUDPServer udpServer = SableUDPServer.getServer(this.level.getServer());
                  if (udpServer != null && udpServer.isConnectedTo(player)) {
                     Iterator<ClientboundSableSnapshotDualPacket.Entry> iter = entries.iterator();
                     udpServer.sendUDPPacket(player, new ClientboundSableSnapshotInfoDualPacket(msSinceLastSend, this.interpolationTick, false), true);

                     while (iter.hasNext()) {
                        List<ClientboundSableSnapshotDualPacket.Entry> batch = new ObjectArrayList();

                        for (int i = 0; i < 16 && iter.hasNext(); i++) {
                           batch.add(iter.next());
                        }

                        udpServer.sendUDPPacket(player, new ClientboundSableSnapshotDualPacket(this.interpolationTick, batch), true);
                     }
                  } else {
                     Iterator<ClientboundSableSnapshotDualPacket.Entry> iter = entries.iterator();

                     while (iter.hasNext()) {
                        List<ClientboundSableSnapshotDualPacket.Entry> batch = new ObjectArrayList();

                        for (int i = 0; i < 16 && iter.hasNext(); i++) {
                           batch.add(iter.next());
                        }

                        player.connection
                           .send(
                              new ClientboundBundlePacket(
                                 List.of(
                                    new ClientboundCustomPayloadPacket(
                                       new ClientboundSableSnapshotInfoDualPacket(msSinceLastSend, this.interpolationTick, false)
                                    ),
                                    new ClientboundCustomPayloadPacket(new ClientboundSableSnapshotDualPacket(this.interpolationTick, batch))
                                 )
                              )
                           );
                     }
                  }
               }

               this.interpolationTick++;
               return;
            }

            SubLevel subLevel = (SubLevel)ms.next();
            if (!subLevel.isRemoved()) {
               serverSubLevel = (ServerSubLevel)subLevel;
               tracking = serverSubLevel.getTrackingPlayers();
               type = SubLevelTrackingSystem.SubLevelUpdateTicket.UpdateTicketType.MOVE;
               if (!serverSubLevel.logicalPose().withinTolerance(serverSubLevel.lastNetworkedPose(), 9.375E-4, Math.toRadians(0.015))) {
                  serverSubLevel.lastNetworkedPose().set(serverSubLevel.logicalPose());
                  serverSubLevel.setLastNetworkedStopped(false);
                  break;
               }

               if (!serverSubLevel.getLastNetworkedStopped()) {
                  type = SubLevelTrackingSystem.SubLevelUpdateTicket.UpdateTicketType.STOP;
                  serverSubLevel.setLastNetworkedStopped(true);
                  break;
               }
            }
         }

         for (UUID uuid : tracking) {
            ServerPlayer player = (ServerPlayer)this.level.getPlayerByUUID(uuid);
            if (player != null) {
               List<SubLevelTrackingSystem.SubLevelUpdateTicket> playerUpdates = movementUpdates.computeIfAbsent(uuid, p -> new ArrayList<>());
               playerUpdates.add(new SubLevelTrackingSystem.SubLevelUpdateTicket(serverSubLevel, type));
            }
         }
      }
   }

   public void addTrackingPlugin(SubLevelTrackingPlugin plugin) {
      if (!this.plugins.contains(plugin)) {
         this.plugins.add(plugin);
      }
   }

   private static record SubLevelUpdateTicket(SubLevel subLevels, SubLevelTrackingSystem.SubLevelUpdateTicket.UpdateTicketType type) {
      private static enum UpdateTicketType {
         STOP,
         MOVE;
      }
   }
}
