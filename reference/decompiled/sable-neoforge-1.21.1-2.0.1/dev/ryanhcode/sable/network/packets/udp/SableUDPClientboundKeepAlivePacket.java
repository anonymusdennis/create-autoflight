package dev.ryanhcode.sable.network.packets.udp;

import dev.ryanhcode.sable.mixinterface.udp.ConnectionExtension;
import dev.ryanhcode.sable.network.udp.AddressedSableUDPPacket;
import dev.ryanhcode.sable.network.udp.SableUDPPacket;
import dev.ryanhcode.sable.network.udp.SableUDPPacketType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.InetSocketAddress;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

public record SableUDPClientboundKeepAlivePacket() implements SableUDPPacket {
   public static final StreamCodec<RegistryFriendlyByteBuf, SableUDPClientboundKeepAlivePacket> CODEC = StreamCodec.of((buf, value) -> {
   }, buf -> new SableUDPClientboundKeepAlivePacket());

   @Override
   public SableUDPPacketType getType() {
      return SableUDPPacketType.KEEP_ALIVE_CLIENTBOUND;
   }

   @Override
   public void handleClient(Level level) {
      Connection connection = Minecraft.getInstance().getConnection().getConnection();
      ConnectionExtension connectionExtension = (ConnectionExtension)connection;
      Channel channel = connectionExtension.sable$getUDPChannel();
      InetSocketAddress baseAddress = (InetSocketAddress)connection.getRemoteAddress();
      InetSocketAddress remoteAddress = new InetSocketAddress(baseAddress.getAddress(), baseAddress.getPort());
      channel.eventLoop().execute(() -> {
         SableUDPServerboundAlivePacket packet = new SableUDPServerboundAlivePacket();
         AddressedSableUDPPacket envelope = new AddressedSableUDPPacket(packet, remoteAddress);
         ChannelFuture writeFuture = channel.writeAndFlush(envelope);
         writeFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
      });
   }
}
