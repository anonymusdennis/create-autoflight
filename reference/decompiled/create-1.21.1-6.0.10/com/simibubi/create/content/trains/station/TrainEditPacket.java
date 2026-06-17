package com.simibubi.create.content.trains.station;

import com.mojang.datafixers.util.Function4;
import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainIconType;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class TrainEditPacket implements CustomPacketPayload {
   protected final UUID id;
   protected final String name;
   protected final ResourceLocation iconType;
   protected final int mapColor;

   protected TrainEditPacket(UUID id, String name, ResourceLocation iconType, int mapColor) {
      this.id = id;
      this.name = name;
      this.iconType = iconType;
      this.mapColor = mapColor;
   }

   private static <T extends TrainEditPacket> StreamCodec<ByteBuf, T> codec(Function4<UUID, String, ResourceLocation, Integer, T> factory) {
      return StreamCodec.composite(
         UUIDUtil.STREAM_CODEC,
         packet -> packet.id,
         ByteBufCodecs.stringUtf8(256),
         packet -> packet.name,
         ResourceLocation.STREAM_CODEC,
         packet -> packet.iconType,
         ByteBufCodecs.INT,
         packet -> packet.mapColor,
         factory
      );
   }

   public void handleSided(Player sender) {
      Level level = sender == null ? null : sender.level();
      Train train = Create.RAILWAYS.sided(level).trains.get(this.id);
      if (train != null) {
         if (!this.name.isBlank()) {
            train.name = Component.literal(this.name);
         }

         train.icon = TrainIconType.byId(this.iconType);
         train.mapColorIndex = this.mapColor;
         if (sender != null) {
            CatnipServices.NETWORK.sendToAllClients(new TrainEditPacket.TrainEditReturnPacket(this.id, this.name, this.iconType, this.mapColor));
         }
      }
   }

   public static class Serverbound extends TrainEditPacket implements ServerboundPacketPayload {
      public static final StreamCodec<ByteBuf, TrainEditPacket.Serverbound> STREAM_CODEC = TrainEditPacket.codec(TrainEditPacket.Serverbound::new);

      public Serverbound(UUID id, String name, ResourceLocation iconType, int mapColor) {
         super(id, name, iconType, mapColor);
      }

      public void handle(ServerPlayer sender) {
         this.handleSided(sender);
      }

      public PacketTypeProvider getTypeProvider() {
         return AllPackets.C_CONFIGURE_TRAIN;
      }
   }

   public static class TrainEditReturnPacket extends TrainEditPacket implements ClientboundPacketPayload {
      public static final StreamCodec<ByteBuf, TrainEditPacket.TrainEditReturnPacket> STREAM_CODEC = TrainEditPacket.codec(
         TrainEditPacket.TrainEditReturnPacket::new
      );

      public TrainEditReturnPacket(UUID id, String name, ResourceLocation iconType, int mapColor) {
         super(id, name, iconType, mapColor);
      }

      @OnlyIn(Dist.CLIENT)
      public void handle(LocalPlayer player) {
         this.handleSided(null);
      }

      public PacketTypeProvider getTypeProvider() {
         return AllPackets.S_CONFIGURE_TRAIN;
      }
   }
}
