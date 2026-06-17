package dev.simulated_team.simulated.network.packets.honey_glue;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueMaxSizing;
import dev.simulated_team.simulated.index.SimSoundEvents;
import foundry.veil.api.network.handler.PacketContext;
import java.util.UUID;
import net.createmod.catnip.data.Pair;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public record HoneyGlueChangeBoundsPacket(AABB bounds, UUID honeyGlue) implements CustomPacketPayload {
   public static Type<HoneyGlueChangeBoundsPacket> TYPE = new Type(Simulated.path("honey_glue_change_bounds"));
   public static StreamCodec<RegistryFriendlyByteBuf, HoneyGlueChangeBoundsPacket> CODEC = StreamCodec.of(
      HoneyGlueChangeBoundsPacket::writeToBuf, HoneyGlueChangeBoundsPacket::readFromBuf
   );

   public static void writeToBuf(RegistryFriendlyByteBuf buf, HoneyGlueChangeBoundsPacket packet) {
      writeAABB(buf, packet.bounds);
      buf.writeUUID(packet.honeyGlue());
   }

   public static HoneyGlueChangeBoundsPacket readFromBuf(RegistryFriendlyByteBuf buf) {
      AABB serializedBounds = new AABB(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
      return new HoneyGlueChangeBoundsPacket(serializedBounds, buf.readUUID());
   }

   public static void writeAABB(RegistryFriendlyByteBuf bytebuf, AABB bb) {
      HoneyGlueSyncBoundsPacket.writeAABB(bytebuf, bb);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public void handle(PacketContext context) {
      Level level = context.level();
      Entity entity = ((ServerLevel)level).getEntity(this.honeyGlue);
      if (entity instanceof HoneyGlueEntity honeyGlue) {
         Pair<Boolean, String> pair = HoneyGlueMaxSizing.checkBounds(this.bounds);
         if (!(Boolean)pair.getFirst()) {
            SimSoundEvents.HONEY_ADDED.play(entity.level(), null, honeyGlue.getBoundingBox().getCenter(), 0.5F, 0.5F);
            honeyGlue.spawnParticles();
            entity.remove(RemovalReason.KILLED);
         } else {
            honeyGlue.setBoundsAndSync(this.bounds, context.player());
         }
      }
   }
}
