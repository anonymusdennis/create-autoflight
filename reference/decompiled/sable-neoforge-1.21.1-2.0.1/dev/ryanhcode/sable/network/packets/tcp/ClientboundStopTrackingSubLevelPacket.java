package dev.ryanhcode.sable.network.packets.tcp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import foundry.veil.api.network.handler.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public record ClientboundStopTrackingSubLevelPacket(long plotCoordinate) implements SableTCPPacket {
   public static final Type<ClientboundStopTrackingSubLevelPacket> TYPE = new Type(Sable.sablePath("stop_tracking_sub_level"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundStopTrackingSubLevelPacket> CODEC = StreamCodec.of(
      (buf, value) -> value.write(buf), ClientboundStopTrackingSubLevelPacket::read
   );

   private static ClientboundStopTrackingSubLevelPacket read(FriendlyByteBuf buf) {
      return new ClientboundStopTrackingSubLevelPacket(buf.readLong());
   }

   private void write(FriendlyByteBuf buf) {
      buf.writeLong(this.plotCoordinate);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   @Override
   public void handle(PacketContext context) {
      Level level = context.level();
      SubLevelContainer container = SubLevelContainer.getContainer(level);
      if (container == null) {
         Sable.LOGGER.error("Received a sub-level tracking packet for a level without a sub-level container");
      } else {
         int chunkX = ChunkPos.getX(this.plotCoordinate);
         int chunkZ = ChunkPos.getZ(this.plotCoordinate);
         if (container.getSubLevel(chunkX, chunkZ) == null) {
            Sable.LOGGER.error("Received a sub-level tracking removal packet for unknown sub-level: {}, {}", chunkX, chunkZ);
         } else {
            container.removeSubLevel(chunkX, chunkZ, SubLevelRemovalReason.REMOVED);
         }
      }
   }
}
