package dev.ryanhcode.sable.network.udp;

import dev.ryanhcode.sable.Sable;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import net.minecraft.network.ProtocolSwapHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class SableUDPPacketDecoder extends MessageToMessageDecoder<DatagramPacket> implements ProtocolSwapHandler {
   public SableUDPPacketDecoder() {
      super(DatagramPacket.class);
   }

   protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
      ByteBuf byteBuf = (ByteBuf)msg.content();
      int i = byteBuf.readableBytes();
      if (i != 0) {
         short packetID = byteBuf.readUnsignedByte();
         if (packetID >= SableUDPPacketType.VALUES.length) {
            throw new IOException("Received an invalid packet ID: " + packetID);
         }

         SableUDPPacketType packetType = SableUDPPacketType.VALUES[packetID];

         SableUDPPacket packet;
         try {
            packet = packetType.create(new RegistryFriendlyByteBuf(byteBuf, null));
         } catch (Exception var10) {
            Sable.LOGGER.error("Failed to decode UDP packet of type {} from {}", new Object[]{packetType, msg.sender(), var10});
            return;
         }

         if (byteBuf.readableBytes() > 0) {
            Sable.LOGGER
               .error(
                  "SableUDPPacket {} ({}) was larger than expected, found {} bytes extra",
                  new Object[]{packetType, packet.getClass().getSimpleName(), byteBuf.readableBytes()}
               );
            return;
         }

         out.add(new AddressedSableUDPPacket(packet, (InetSocketAddress)msg.sender()));
      }
   }
}
