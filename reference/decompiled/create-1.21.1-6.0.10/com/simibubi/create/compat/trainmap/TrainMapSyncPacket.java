package com.simibubi.create.compat.trainmap;

import com.simibubi.create.AllPackets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TrainMapSyncPacket implements ClientboundPacketPayload {
   public static final StreamCodec<FriendlyByteBuf, TrainMapSyncPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      packet -> packet.light,
      CatnipStreamCodecBuilders.list(Pair.streamCodec(UUIDUtil.STREAM_CODEC, TrainMapSync.TrainMapSyncEntry.STREAM_CODEC)),
      packet -> packet.entries,
      TrainMapSyncPacket::new
   );
   public boolean light;
   public List<Pair<UUID, TrainMapSync.TrainMapSyncEntry>> entries = new ArrayList<>();

   public TrainMapSyncPacket(boolean light) {
      this.light = light;
   }

   public TrainMapSyncPacket(boolean light, List<Pair<UUID, TrainMapSync.TrainMapSyncEntry>> entries) {
      this.light = light;
      this.entries = entries;
   }

   public void add(UUID trainId, TrainMapSync.TrainMapSyncEntry data) {
      this.entries.add(Pair.of(trainId, data));
   }

   @OnlyIn(Dist.CLIENT)
   public void handle(LocalPlayer player) {
      TrainMapSyncClient.receive(this);
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.TRAIN_MAP_SYNC;
   }
}
