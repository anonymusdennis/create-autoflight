package dev.simulated_team.simulated.network.packets.honey_glue;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import foundry.veil.api.network.handler.ClientPacketContext;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public record HoneyGlueSyncBoundsPacket(AABB bounds, int honeyGlueId, UUID uuid) implements CustomPacketPayload {
   public static Type<HoneyGlueSyncBoundsPacket> TYPE = new Type(Simulated.path("honey_glue_sync"));
   public static StreamCodec<RegistryFriendlyByteBuf, HoneyGlueSyncBoundsPacket> CODEC = StreamCodec.of(
      HoneyGlueSyncBoundsPacket::writeToBuf, HoneyGlueSyncBoundsPacket::readFromBuf
   );

   public static void writeToBuf(RegistryFriendlyByteBuf buf, HoneyGlueSyncBoundsPacket packet) {
      writeAABB(buf, packet.bounds);
      buf.writeInt(packet.honeyGlueId());
      buf.writeBoolean(packet.uuid != null);
      if (packet.uuid != null) {
         buf.writeUUID(packet.uuid);
      }
   }

   public static HoneyGlueSyncBoundsPacket readFromBuf(RegistryFriendlyByteBuf buf) {
      AABB serializedBounds = new AABB(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
      int honeyGlueId = buf.readInt();
      UUID uuid = null;
      if (buf.readBoolean()) {
         uuid = buf.readUUID();
      }

      return new HoneyGlueSyncBoundsPacket(serializedBounds, honeyGlueId, uuid);
   }

   public static void writeAABB(RegistryFriendlyByteBuf byteBuf, AABB bb) {
      byteBuf.writeDouble(bb.minX);
      byteBuf.writeDouble(bb.minY);
      byteBuf.writeDouble(bb.minZ);
      byteBuf.writeDouble(bb.maxX);
      byteBuf.writeDouble(bb.maxY);
      byteBuf.writeDouble(bb.maxZ);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public void handle(ClientPacketContext context) {
      if (this.uuid == null || !this.uuid.equals(context.player().getUUID())) {
         Level level = context.level();
         if (level.getEntity(this.honeyGlueId) instanceof HoneyGlueEntity honeyGlue) {
            honeyGlue.setBounds(this.bounds);
         }
      }
   }
}
