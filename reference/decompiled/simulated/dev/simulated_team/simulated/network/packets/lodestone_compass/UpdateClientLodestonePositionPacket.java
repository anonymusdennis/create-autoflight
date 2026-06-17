package dev.simulated_team.simulated.network.packets.lodestone_compass;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability.ClientLodestonePositions;
import foundry.veil.api.network.handler.PacketContext;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import org.joml.Vector3d;

public record UpdateClientLodestonePositionPacket(UUID id, Vector3d sentPosition) implements CustomPacketPayload {
   public static final Type<UpdateClientLodestonePositionPacket> TYPE = new Type(Simulated.path("update_client_lodestone"));
   public static final StreamCodec<ByteBuf, UpdateClientLodestonePositionPacket> STREAM_CODEC = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC,
      UpdateClientLodestonePositionPacket::id,
      StreamCodec.of((byteBuf, p) -> {
         byteBuf.writeDouble(p.x);
         byteBuf.writeDouble(p.y);
         byteBuf.writeDouble(p.z);
      }, byteBuf -> new Vector3d(byteBuf.readDouble(), byteBuf.readDouble(), byteBuf.readDouble())),
      UpdateClientLodestonePositionPacket::sentPosition,
      UpdateClientLodestonePositionPacket::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public void handle(PacketContext context) {
      if (context.level() instanceof ClientLevel cl) {
         ClientLodestonePositions positions = (ClientLodestonePositions)ClientLodestonePositions.clientPositions.get(cl);
         positions.CLIENT_LODESTONE_MAP.put(this.id, this.sentPosition);
      }
   }
}
