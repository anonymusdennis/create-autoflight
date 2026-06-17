package dev.ryanhcode.sable.network.udp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class SableUDPPacketEncoder extends MessageToMessageEncoder<AddressedSableUDPPacket> {
   protected void encode(ChannelHandlerContext ctx, AddressedSableUDPPacket envelope, List<Object> out) throws Exception {
      SableUDPPacket msg = envelope.packet();
      SableUDPPacketType packetType = msg.getType();

      try {
         ByteBuf buf = ctx.alloc().ioBuffer();
         buf.writeByte(packetType.ordinal());
         packetType.write(new RegistryFriendlyByteBuf(buf, null), msg);
         out.add(new DatagramPacket(buf, envelope.address()));
      } catch (Exception var7) {
         throw new EncoderException("Failed to encode %s packet of type %s".formatted(msg.getClass().getSimpleName(), packetType), var7);
      }
   }
}
