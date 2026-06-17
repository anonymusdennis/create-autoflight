package dev.ryanhcode.sable.network.udp;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.flow.FlowControlHandler;
import java.net.InetSocketAddress;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.MonitorFrameDecoder;
import net.minecraft.network.NoOpFrameDecoder;
import net.minecraft.network.NoOpFrameEncoder;
import net.minecraft.network.Varint21FrameDecoder;
import net.minecraft.network.Varint21LengthFieldPrepender;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public interface SableUDPPacket {
   static void configureSerialization(ChannelPipeline pipeline, PacketFlow flow, boolean memoryOnly, @Nullable BandwidthDebugMonitor debugMonitor) {
      pipeline.addLast("splitter", createFrameDecoder(debugMonitor, memoryOnly))
         .addLast(new ChannelHandler[]{new FlowControlHandler()})
         .addLast("decoder", new SableUDPPacketDecoder())
         .addLast("prepender", createFrameEncoder(memoryOnly))
         .addLast("encoder", new SableUDPPacketEncoder());
   }

   private static ChannelOutboundHandler createFrameEncoder(boolean memoryOnly) {
      return (ChannelOutboundHandler)(memoryOnly ? new NoOpFrameEncoder() : new Varint21LengthFieldPrepender());
   }

   private static ChannelInboundHandler createFrameDecoder(@Nullable BandwidthDebugMonitor debugMonitor, boolean memoryOnly) {
      if (!memoryOnly) {
         return new Varint21FrameDecoder(debugMonitor);
      } else {
         return (ChannelInboundHandler)(debugMonitor != null ? new MonitorFrameDecoder(debugMonitor) : new NoOpFrameDecoder());
      }
   }

   static void configureInMemoryPipeline(ChannelPipeline channelPipeline, PacketFlow arg) {
      configureSerialization(channelPipeline, arg, true, null);
   }

   SableUDPPacketType getType();

   default void handleClient(Level level) {
   }

   default void handleServer(MinecraftServer server, InetSocketAddress sender) {
   }
}
