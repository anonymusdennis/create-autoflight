package dev.simulated_team.simulated.network.packets.contraption_diagram;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import foundry.veil.api.network.handler.ServerPacketContext;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public record RequestDiagramDataPacket(UUID subLevel) implements CustomPacketPayload {
   public static Type<RequestDiagramDataPacket> TYPE = new Type(Simulated.path("request_diagram_data"));
   public static StreamCodec<RegistryFriendlyByteBuf, RequestDiagramDataPacket> CODEC = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC, RequestDiagramDataPacket::subLevel, RequestDiagramDataPacket::new
   );

   public void handle(ServerPacketContext context) {
      ServerPlayer player = context.player();
      Level level = player.level();
      SubLevelContainer container = SubLevelContainer.getContainer(level);

      assert container != null;

      if (container.getSubLevel(this.subLevel) instanceof ServerSubLevel serverSubLevel) {
         DiagramEntity.queueDiagramDataFor(serverSubLevel, player);
      }
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
