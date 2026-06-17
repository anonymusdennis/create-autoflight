package dev.ryanhcode.sable.network.packets;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.network.udp.SableUDPPacket;
import dev.ryanhcode.sable.network.udp.SableUDPPacketType;
import foundry.veil.api.network.handler.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.level.Level;

public final class ClientboundSableSnapshotInfoDualPacket implements SableUDPPacket, SableTCPPacket {
   public static final Type<ClientboundSableSnapshotInfoDualPacket> TYPE = new Type(Sable.sablePath("snapshot_info_packet"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSableSnapshotInfoDualPacket> CODEC = StreamCodec.of(
      (buf, value) -> value.encode(buf), ClientboundSableSnapshotInfoDualPacket::new
   );
   private final int msSinceLast;
   private final int gameTick;
   private final boolean stopped;

   public ClientboundSableSnapshotInfoDualPacket(int msSinceLast, int gameTick, boolean stopped) {
      this.msSinceLast = msSinceLast;
      this.gameTick = gameTick;
      this.stopped = stopped;
   }

   public ClientboundSableSnapshotInfoDualPacket(ByteBuf buf) {
      this(buf.readInt(), buf.readInt(), buf.readBoolean());
   }

   @Override
   public void handle(PacketContext context) {
      this.handleClient(context.level());
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public void encode(ByteBuf byteBuf) {
      FriendlyByteBuf buf = (FriendlyByteBuf)byteBuf;
      buf.writeInt(this.msSinceLast);
      buf.writeInt(this.gameTick);
      buf.writeBoolean(this.stopped);
   }

   @Override
   public SableUDPPacketType getType() {
      return SableUDPPacketType.SNAPSHOT_INFO;
   }

   @Override
   public void handleClient(Level level) {
      SubLevelContainer container = SubLevelContainer.getContainer(level);
      if (container == null) {
         Sable.LOGGER.error("Received a sub-level movement packet for a level without a sub-level container");
      } else {
         ((ClientSubLevelContainer)container).getInterpolation().receiveInfo(this.msSinceLast, this.gameTick, this.stopped);
      }
   }
}
