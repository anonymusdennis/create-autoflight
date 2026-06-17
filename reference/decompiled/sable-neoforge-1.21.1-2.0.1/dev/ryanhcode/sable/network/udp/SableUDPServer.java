package dev.ryanhcode.sable.network.udp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.SableConfig;
import dev.ryanhcode.sable.mixinterface.udp.ServerConnectionListenerExtension;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundSableUDPActivationPacket;
import dev.ryanhcode.sable.network.packets.udp.SableUDPClientboundKeepAlivePacket;
import dev.ryanhcode.sable.util.SableDistUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.local.LocalAddress;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class SableUDPServer {
   public static final long PING_INTERVAL = 2500L;
   private static final int MISSED_PINGS_ALLOWED = 10;
   private final Channel channel;
   private final Map<Connection, SableUDPAuthenticationState> udpAuthStates;
   private final MinecraftServer server;
   private int pingIndex = 0;

   public SableUDPServer(MinecraftServer server, Channel channel) {
      this.server = server;
      this.channel = channel;
      this.udpAuthStates = new WeakHashMap<>();
   }

   @Internal
   @Nullable
   public static SableUDPServer getServer(MinecraftServer server) {
      return ((ServerConnectionListenerExtension)server.getConnection()).sable$getServer();
   }

   public boolean isConnectedTo(ServerPlayer player) {
      if (!(Boolean)SableConfig.ATTEMPT_UDP_NETWORKING.get()) {
         return false;
      } else if (player.connection.getRemoteAddress() instanceof LocalAddress
         && player.server.isSingleplayer()
         && player.server.isSingleplayerOwner(player.getGameProfile())) {
         return true;
      } else {
         Connection connection = player.connection.connection;
         SableUDPAuthenticationState authState = this.udpAuthStates.get(connection);
         return authState != null && authState.getState() == SableUDPAuthenticationState.State.AUTHENTICATED;
      }
   }

   public boolean sendUDPPacket(ServerPlayer player, SableUDPPacket packet, boolean flush) {
      if (this.channel.eventLoop().inEventLoop()) {
         throw new IllegalStateException("Cannot send packet from event loop");
      } else {
         Connection connection = player.connection.connection;
         if (connection.getRemoteAddress() instanceof LocalAddress) {
            this.sendUDPPacketLocal(packet);
            return true;
         } else {
            SableUDPAuthenticationState authenticationState = this.udpAuthStates.get(connection);
            if (authenticationState == null) {
               Sable.LOGGER.error("Attempted to send packet to player \"{}\" without authentication state", player.getName().getString());
               return false;
            } else {
               InetSocketAddress inetSocketAddress = authenticationState.getActiveAddress();
               if (inetSocketAddress == null) {
                  Sable.LOGGER.error("No UDP address in authentication state for player \"{}\"", player.getName().getString());
                  return false;
               } else {
                  this.channel.eventLoop().execute(() -> {
                     AddressedSableUDPPacket envelope = new AddressedSableUDPPacket(packet, inetSocketAddress);
                     ChannelFuture writeFuture = flush ? this.channel.writeAndFlush(envelope) : this.channel.write(envelope);
                     writeFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                  });
                  return true;
               }
            }
         }
      }
   }

   private void sendUDPPacketLocal(SableUDPPacket packet) {
      RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), this.server.registryAccess());
      packet.getType().write(buffer, packet);
      SableUDPPacket decodedPacket = packet.getType().create(buffer);
      SableClient.NETWORK_EVENT_LOOP.tell(() -> decodedPacket.handleClient(SableDistUtil.getClientLevel()));
   }

   @Internal
   public void beginAuthentication(ServerPlayer player) {
      if (!(player.connection.getRemoteAddress() instanceof LocalAddress)) {
         UUID token = UUID.randomUUID();
         SableUDPAuthenticationState authState = new SableUDPAuthenticationState(token);
         this.udpAuthStates.put(player.connection.connection, authState);
         if ((Boolean)SableConfig.ATTEMPT_UDP_NETWORKING.get()) {
            player.connection.send(new ClientboundCustomPayloadPacket(new ClientboundSableUDPActivationPacket(token)));
         }
      }
   }

   @Internal
   public void receiveAuthenticationPacket(UUID uuid, InetSocketAddress inetSocketAddress) {
      for (Entry<Connection, SableUDPAuthenticationState> entry : this.udpAuthStates.entrySet()) {
         SableUDPAuthenticationState state = entry.getValue();
         if (state.isExpectedToken(uuid)) {
            state.assignAddress(inetSocketAddress);
            state.setLastAlivePingIndex(this.pingIndex);
            Sable.LOGGER.info("UDP authentication complete with {}, UDP routing to {}", entry.getKey().getRemoteAddress(), inetSocketAddress);
            return;
         }
      }
   }

   public void sendPings() {
      Iterator<Entry<Connection, SableUDPAuthenticationState>> iter = this.udpAuthStates.entrySet().iterator();

      while (iter.hasNext()) {
         Entry<Connection, SableUDPAuthenticationState> entry = iter.next();
         Connection connection = entry.getKey();
         SableUDPAuthenticationState state = entry.getValue();
         if (!connection.isConnected()) {
            iter.remove();
         } else if (state.getState() == SableUDPAuthenticationState.State.AUTHENTICATED) {
            if (this.pingIndex - state.getLastAlivePingIndex() > 10) {
               Sable.LOGGER
                  .warn(
                     "UDP connection with {} failed to respond to any keep-alive packets after ~{}ms, kicking them to TCP",
                     connection.getRemoteAddress(),
                     25000L
                  );
               iter.remove();
            } else {
               InetSocketAddress inetSocketAddress = state.getActiveAddress();
               if (inetSocketAddress != null) {
                  SableUDPClientboundKeepAlivePacket packet = new SableUDPClientboundKeepAlivePacket();
                  this.channel.eventLoop().execute(() -> {
                     AddressedSableUDPPacket envelope = new AddressedSableUDPPacket(packet, inetSocketAddress);
                     ChannelFuture writeFuture = this.channel.writeAndFlush(envelope);
                     writeFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                  });
               }
            }
         }
      }

      this.pingIndex++;
   }

   public void receiveAlivePacket(InetSocketAddress sender) {
      for (SableUDPAuthenticationState state : this.udpAuthStates.values()) {
         if (state.getState() == SableUDPAuthenticationState.State.AUTHENTICATED && Objects.equals(state.getActiveAddress(), sender)) {
            state.setLastAlivePingIndex(this.pingIndex);
            return;
         }
      }
   }
}
