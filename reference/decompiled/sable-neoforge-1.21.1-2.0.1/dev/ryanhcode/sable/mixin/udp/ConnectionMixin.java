package dev.ryanhcode.sable.mixin.udp;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.mixinterface.udp.ConnectionExtension;
import dev.ryanhcode.sable.network.udp.SableUDPPacket;
import dev.ryanhcode.sable.network.udp.handler.SableUDPChannelHandlerClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Connection.class})
public abstract class ConnectionMixin implements ConnectionExtension {
   @Unique
   private Channel sable$udpChannel = null;

   @Override
   public void sable$setUDPChannel(Channel channel) {
      this.sable$udpChannel = channel;
   }

   @Inject(
      method = {"disconnect(Lnet/minecraft/network/DisconnectionDetails;)V"},
      at = {@At("TAIL")}
   )
   private void sable$onDisconnect(DisconnectionDetails disconnectionDetails, CallbackInfo ci) {
      Channel channel = this.sable$udpChannel;
      if (this.sable$udpChannel != null && this.sable$udpChannel.isOpen()) {
         this.sable$udpChannel = null;
         channel.close().awaitUninterruptibly().addListener(x -> {
            if (x.isSuccess()) {
               Sable.LOGGER.info("Closed UDP channel!");
            } else {
               Sable.LOGGER.info("Failed to close UDP channel", x.cause());
            }
         });
      }
   }

   @Override
   public Channel sable$getUDPChannel() {
      return this.sable$udpChannel;
   }

   @Inject(
      method = {"connect"},
      at = {@At("TAIL")}
   )
   private static void sable$connect(InetSocketAddress inetSocketAddress, boolean bl, final Connection connection, CallbackInfoReturnable<ChannelFuture> cir) {
      boolean useNativeTransport = SableClient.useNativeTransport();
      Class<? extends Channel> channelClass;
      EventLoopGroup eventLoopGroup;
      if (Epoll.isAvailable() && useNativeTransport) {
         channelClass = EpollDatagramChannel.class;
         eventLoopGroup = (EventLoopGroup)Connection.NETWORK_EPOLL_WORKER_GROUP.get();
      } else {
         channelClass = NioDatagramChannel.class;
         eventLoopGroup = (EventLoopGroup)Connection.NETWORK_WORKER_GROUP.get();
      }

      Sable.LOGGER.info("Starting remote client UDP channel future");
      ChannelFuture channelFuture = ((Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group(eventLoopGroup)).handler(new ChannelInitializer<Channel>() {
         protected void initChannel(Channel channel) {
            channel.config().setOption(ChannelOption.SO_KEEPALIVE, true);
            SableUDPPacket.configureSerialization(channel.pipeline(), PacketFlow.CLIENTBOUND, false, null);
            ConnectionMixin.sable$setupChannel(channel, connection);
         }
      })).channel(channelClass)).connect(inetSocketAddress.getAddress(), inetSocketAddress.getPort());
      channelFuture.syncUninterruptibly();
   }

   @Inject(
      method = {"connectToLocalServer"},
      at = {@At("TAIL")}
   )
   private static void sable$connectToLocalServer(SocketAddress socketAddress, CallbackInfoReturnable<Connection> cir, @Local final Connection connection) {
      Sable.LOGGER.info("Starting local client UDP channel future");
      ChannelFuture channelFuture = ((Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group((EventLoopGroup)Connection.LOCAL_WORKER_GROUP.get()))
               .handler(new ChannelInitializer<Channel>() {
                  protected void initChannel(Channel channel) {
                     SableUDPPacket.configureInMemoryPipeline(channel.pipeline(), PacketFlow.CLIENTBOUND);
                     ConnectionMixin.sable$setupChannel(channel, connection);
                  }
               }))
            .channel(LocalChannel.class))
         .connect(socketAddress)
         .syncUninterruptibly();
      channelFuture.syncUninterruptibly();
   }

   @Unique
   private static void sable$setupChannel(Channel channel, Connection connection) {
      ChannelPipeline pipeline = channel.pipeline();
      pipeline.addLast(new ChannelHandler[]{new SableUDPChannelHandlerClient(connection)});
   }
}
