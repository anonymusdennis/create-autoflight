package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.util.Observable;
import foundry.veil.api.network.handler.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record BlockEntityObservedPacket(BlockPos pos) implements CustomPacketPayload {
   public static Type<BlockEntityObservedPacket> TYPE = new Type(Simulated.path("be_observed"));
   public static StreamCodec<ByteBuf, BlockEntityObservedPacket> CODEC = BlockPos.STREAM_CODEC
      .map(BlockEntityObservedPacket::new, BlockEntityObservedPacket::pos);

   @NotNull
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public void handle(ServerPacketContext context) {
      Level level = context.level();
      ServerPlayer player = context.player();
      if (player.canInteractWithBlock(this.pos, 4.0)) {
         if (level.getBlockEntity(this.pos) instanceof Observable observable) {
            observable.onObserved(player);
         }
      }
   }
}
