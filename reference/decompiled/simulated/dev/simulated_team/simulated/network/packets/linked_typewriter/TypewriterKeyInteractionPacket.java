package dev.simulated_team.simulated.network.packets.linked_typewriter;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.index.SimStats;
import foundry.veil.api.network.handler.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.level.Level;

public record TypewriterKeyInteractionPacket(BlockPos interactionPos, int key, int scanCode, int action) implements CustomPacketPayload {
   public static final Type<TypewriterKeyInteractionPacket> TYPE = new Type(Simulated.path("key_interaction"));
   public static final StreamCodec<ByteBuf, TypewriterKeyInteractionPacket> CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      TypewriterKeyInteractionPacket::interactionPos,
      ByteBufCodecs.INT,
      TypewriterKeyInteractionPacket::key,
      ByteBufCodecs.INT,
      TypewriterKeyInteractionPacket::scanCode,
      ByteBufCodecs.INT,
      TypewriterKeyInteractionPacket::action,
      TypewriterKeyInteractionPacket::new
   );

   public void handle(ServerPacketContext context) {
      Level level = context.level();
      if (level.getBlockEntity(this.interactionPos) instanceof LinkedTypewriterBlockEntity typeWriter) {
         boolean pressed = this.action == 1;
         if (pressed) {
            SimStats.TYPEWRITER_KEY_PRESSES.awardTo(context.player());
         }

         typeWriter.onKeyInteraction(context.player().getUUID(), null, this.key, pressed);
      }
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
