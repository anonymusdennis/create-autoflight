package dev.simulated_team.simulated.network.packets.rope;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import foundry.veil.api.network.handler.ClientPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.level.Level;

public record ClientboundRopeStoppedPacket(BlockPos ownerPos) implements CustomPacketPayload {
   public static final StreamCodec<ByteBuf, ClientboundRopeStoppedPacket> CODEC = BlockPos.STREAM_CODEC
      .map(ClientboundRopeStoppedPacket::new, ClientboundRopeStoppedPacket::ownerPos);
   public static Type<ClientboundRopeStoppedPacket> TYPE = new Type(Simulated.path("rope_stopped"));

   public void handle(ClientPacketContext context) {
      LocalPlayer player = context.player();
      Level level = player.level();
      if (level.getBlockEntity(this.ownerPos) instanceof SmartBlockEntity smartBlockEntity) {
         RopeStrandHolderBehavior ropeHolder = (RopeStrandHolderBehavior)smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
         if (ropeHolder != null) {
            ropeHolder.receiveClientStrandStopped();
         }
      }
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
