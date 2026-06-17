package dev.simulated_team.simulated.network.packets;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;
import foundry.veil.api.network.handler.ServerPacketContext;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public record RopeBreakPacket(UUID uuid) implements CustomPacketPayload {
   public static Type<RopeBreakPacket> TYPE = new Type(Simulated.path("break_rope"));
   public static StreamCodec<RegistryFriendlyByteBuf, RopeBreakPacket> CODEC = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC, RopeBreakPacket::uuid, RopeBreakPacket::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public void handle(ServerPacketContext ctx) {
      ServerPlayer player = ctx.player();
      Level level = player.level();
      ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(level);
      ServerRopeStrand strand = manager.getStrand(this.uuid);
      if (strand != null) {
         RopeAttachment startAttachment = strand.getAttachment(RopeAttachmentPoint.START);
         if (startAttachment == null) {
            return;
         }

         BlockPos blockAttachment = startAttachment.blockAttachment();
         if (!(level.getBlockEntity(blockAttachment) instanceof SmartBlockEntity smartBlockEntity)) {
            return;
         }

         RopeStrandHolderBehavior holder = (RopeStrandHolderBehavior)smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
         if (holder == null) {
            return;
         }

         holder.destroyRope(player, null, !player.hasInfiniteMaterials());
      }
   }
}
