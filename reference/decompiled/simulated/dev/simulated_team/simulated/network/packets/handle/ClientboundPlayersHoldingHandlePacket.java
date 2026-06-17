package dev.simulated_team.simulated.network.packets.handle;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.handle.PlayerHoldingHandleRenderer;
import foundry.veil.api.network.handler.ClientPacketContext;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ClientboundPlayersHoldingHandlePacket(Collection<UUID> uuids) implements CustomPacketPayload {
   public static Type<ClientboundPlayersHoldingHandlePacket> TYPE = new Type(Simulated.path("players_holding_handles"));
   public static final StreamCodec<ByteBuf, ClientboundPlayersHoldingHandlePacket> CODEC = StreamCodec.composite(
      ByteBufCodecs.collection(HashSet::new, UUIDUtil.STREAM_CODEC), ClientboundPlayersHoldingHandlePacket::uuids, ClientboundPlayersHoldingHandlePacket::new
   );

   public void handle(ClientPacketContext context) {
      PlayerHoldingHandleRenderer.updatePlayerList(this.uuids);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
