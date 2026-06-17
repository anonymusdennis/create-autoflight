package dev.ryanhcode.sable.network.udp.handler;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.mixinterface.udp.ConnectionExtension;
import dev.ryanhcode.sable.network.udp.AddressedSableUDPPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;

public class SableUDPChannelHandlerClient extends SimpleChannelInboundHandler<AddressedSableUDPPacket> {
   private final Connection connection;
   private Channel channel;

   public SableUDPChannelHandlerClient(Connection connection) {
      super(AddressedSableUDPPacket.class);
      this.connection = connection;
   }

   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      super.exceptionCaught(ctx, cause);
      Sable.LOGGER.error("UDP channel exception caught", cause);
   }

   public void channelActive(ChannelHandlerContext ctx) throws Exception {
      super.channelActive(ctx);
      Sable.LOGGER.info("Client UDP channel active");
      this.channel = ctx.channel();
      ((ConnectionExtension)this.connection).sable$setUDPChannel(this.channel);
   }

   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      super.channelInactive(ctx);
      Sable.LOGGER.info("Client UDP channel inactive");
   }

   protected void channelRead0(ChannelHandlerContext ctx, AddressedSableUDPPacket msg) throws Exception {
      Minecraft client = Minecraft.getInstance();
      SableClient.NETWORK_EVENT_LOOP.tell(() -> msg.packet().handleClient(client.level));
   }
}
