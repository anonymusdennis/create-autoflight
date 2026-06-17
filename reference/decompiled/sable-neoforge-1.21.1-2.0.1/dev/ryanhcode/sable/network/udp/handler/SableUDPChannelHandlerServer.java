package dev.ryanhcode.sable.network.udp.handler;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.udp.ServerConnectionListenerExtension;
import dev.ryanhcode.sable.network.udp.AddressedSableUDPPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConnectionListener;

public class SableUDPChannelHandlerServer extends SimpleChannelInboundHandler<AddressedSableUDPPacket> {
   private final MinecraftServer server;
   private final ServerConnectionListener serverConnectionListener;

   public SableUDPChannelHandlerServer(MinecraftServer server, ServerConnectionListener serverConnectionListener) {
      super(AddressedSableUDPPacket.class);
      this.server = server;
      this.serverConnectionListener = serverConnectionListener;
   }

   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      super.exceptionCaught(ctx, cause);
      Sable.LOGGER.error("Server UDP channel caught exception", cause);
   }

   public void channelActive(ChannelHandlerContext ctx) throws Exception {
      super.channelActive(ctx);
      Sable.LOGGER.info("Server UDP channel active");
      ((ServerConnectionListenerExtension)this.serverConnectionListener).sable$setupUDPServer(ctx.channel());
   }

   protected void channelRead0(ChannelHandlerContext ctx, AddressedSableUDPPacket msg) throws Exception {
      msg.packet().handleServer(this.server, msg.address());
   }
}
