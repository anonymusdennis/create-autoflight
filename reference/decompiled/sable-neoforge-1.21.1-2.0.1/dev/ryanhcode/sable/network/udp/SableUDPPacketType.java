package dev.ryanhcode.sable.network.udp;

import dev.ryanhcode.sable.network.packets.ClientboundSableSnapshotDualPacket;
import dev.ryanhcode.sable.network.packets.ClientboundSableSnapshotInfoDualPacket;
import dev.ryanhcode.sable.network.packets.udp.SableUDPAuthenticationPacket;
import dev.ryanhcode.sable.network.packets.udp.SableUDPClientboundKeepAlivePacket;
import dev.ryanhcode.sable.network.packets.udp.SableUDPEchoPacket;
import dev.ryanhcode.sable.network.packets.udp.SableUDPServerboundAlivePacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public enum SableUDPPacketType {
   PING(SableUDPEchoPacket.CODEC),
   SNAPSHOT(ClientboundSableSnapshotDualPacket.CODEC),
   SNAPSHOT_INFO(ClientboundSableSnapshotInfoDualPacket.CODEC),
   AUTH(SableUDPAuthenticationPacket.CODEC),
   KEEP_ALIVE_CLIENTBOUND(SableUDPClientboundKeepAlivePacket.CODEC),
   ALIVE_SERVERBOUND(SableUDPServerboundAlivePacket.CODEC);

   public static final SableUDPPacketType[] VALUES = values();
   private final StreamCodec<RegistryFriendlyByteBuf, ? extends SableUDPPacket> codec;

   private SableUDPPacketType(final StreamCodec<RegistryFriendlyByteBuf, ? extends SableUDPPacket> codec) {
      this.codec = codec;
   }

   public SableUDPPacket create(RegistryFriendlyByteBuf buf) {
      return (SableUDPPacket)this.codec.decode(buf);
   }

   public void write(RegistryFriendlyByteBuf buf, SableUDPPacket packet) {
      this.codec.encode(buf, packet);
   }
}
