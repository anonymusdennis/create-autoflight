package dev.ryanhcode.sable.network.packets.tcp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import foundry.veil.api.network.handler.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public record ClientboundStopMovingSubLevelPacket(long plotCoordinate) implements SableTCPPacket {
   public static final Type<ClientboundStopMovingSubLevelPacket> TYPE = new Type(Sable.sablePath("stop_moving_sub_level"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundStopMovingSubLevelPacket> CODEC = StreamCodec.of(
      (buf, value) -> value.write(buf), ClientboundStopMovingSubLevelPacket::read
   );

   private void write(FriendlyByteBuf buf) {
      buf.writeLong(this.plotCoordinate);
   }

   private static ClientboundStopMovingSubLevelPacket read(FriendlyByteBuf buf) {
      return new ClientboundStopMovingSubLevelPacket(buf.readLong());
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   @Override
   public void handle(PacketContext context) {
      Level level = context.level();
      SubLevelContainer container = SubLevelContainer.getContainer(level);
      if (container == null) {
         Sable.LOGGER.error("Received a sub-level movement packet for a level without a sub-level container");
      } else {
         SubLevel subLevel = container.getSubLevel(ChunkPos.getX(this.plotCoordinate), ChunkPos.getZ(this.plotCoordinate));

         assert subLevel != null : "Received a sub-level movement packet for a non-existent sub-level";

         if (subLevel instanceof ClientSubLevel clientSubLevel) {
            clientSubLevel.receiveServerMovementStop();
         } else {
            Sable.LOGGER.error("Client sub-level is not a client sub-level. How?");
         }
      }
   }
}
