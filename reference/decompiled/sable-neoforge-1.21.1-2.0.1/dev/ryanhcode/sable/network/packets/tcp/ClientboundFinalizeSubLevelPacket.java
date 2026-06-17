package dev.ryanhcode.sable.network.packets.tcp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import foundry.veil.api.network.handler.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public record ClientboundFinalizeSubLevelPacket(long plotCoordinate) implements SableTCPPacket {
   public static final Type<ClientboundFinalizeSubLevelPacket> TYPE = new Type(Sable.sablePath("finalize_sub_level"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundFinalizeSubLevelPacket> CODEC = StreamCodec.of(
      (buf, value) -> value.write(buf), ClientboundFinalizeSubLevelPacket::read
   );

   private void write(FriendlyByteBuf buf) {
      buf.writeLong(this.plotCoordinate);
   }

   private static ClientboundFinalizeSubLevelPacket read(FriendlyByteBuf buf) {
      return new ClientboundFinalizeSubLevelPacket(buf.readLong());
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   @Override
   public void handle(PacketContext context) {
      Level level = context.level();
      SubLevelContainer container = SubLevelContainer.getContainer(level);
      if (container == null) {
         Sable.LOGGER.error("Received a sub-level finalize packet for a level without a sub-level container");
      } else if (container.getSubLevel(ChunkPos.getX(this.plotCoordinate), ChunkPos.getZ(this.plotCoordinate)) instanceof ClientSubLevel clientSubLevel) {
         clientSubLevel.setFinalized();
         clientSubLevel.updateRenderData();
      } else {
         Sable.LOGGER.error("Received a sub-level finalize packet for an unknown sub-level plot");
      }
   }
}
