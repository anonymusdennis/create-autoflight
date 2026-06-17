package com.simibubi.create.content.trains.graph;

import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record TrackGraphRollCallPacket(List<TrackGraphRollCallPacket.Entry> entries) implements ClientboundPacketPayload {
   public static final StreamCodec<ByteBuf, TrackGraphRollCallPacket> STREAM_CODEC = CatnipStreamCodecBuilders.list(TrackGraphRollCallPacket.Entry.STREAM_CODEC)
      .map(TrackGraphRollCallPacket::new, TrackGraphRollCallPacket::entries);

   public static TrackGraphRollCallPacket ofServer() {
      List<TrackGraphRollCallPacket.Entry> entries = new ArrayList<>();

      for (TrackGraph graph : Create.RAILWAYS.trackNetworks.values()) {
         entries.add(new TrackGraphRollCallPacket.Entry(graph.netId, graph.getChecksum()));
      }

      return new TrackGraphRollCallPacket(entries);
   }

   @OnlyIn(Dist.CLIENT)
   public void handle(LocalPlayer player) {
      GlobalRailwayManager manager = Create.RAILWAYS.sided(null);
      Set<UUID> unusedIds = new HashSet<>(manager.trackNetworks.keySet());
      List<Integer> failedIds = new ArrayList<>();
      Map<Integer, UUID> idByNetId = new HashMap<>();
      manager.trackNetworks.forEach((uuidx, g) -> idByNetId.put(g.netId, uuidx));

      for (TrackGraphRollCallPacket.Entry entry : this.entries) {
         UUID uuid = idByNetId.get(entry.netId);
         if (uuid == null) {
            failedIds.add(entry.netId);
         } else {
            unusedIds.remove(uuid);
            TrackGraph trackGraph = manager.trackNetworks.get(uuid);
            if (trackGraph.getChecksum() != entry.checksum) {
               Create.LOGGER.warn("Track network: {} failed its checksum; Requesting refresh", uuid.toString().substring(0, 6));
               failedIds.add(entry.netId);
            }
         }
      }

      for (Integer failed : failedIds) {
         CatnipServices.NETWORK.sendToServer(new TrackGraphRequestPacket(failed));
      }

      for (UUID unused : unusedIds) {
         manager.trackNetworks.remove(unused);
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.TRACK_GRAPH_ROLL_CALL;
   }

   public static record Entry(int netId, int checksum) {
      public static final StreamCodec<ByteBuf, TrackGraphRollCallPacket.Entry> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         TrackGraphRollCallPacket.Entry::netId,
         ByteBufCodecs.INT,
         TrackGraphRollCallPacket.Entry::checksum,
         TrackGraphRollCallPacket.Entry::new
      );
   }
}
