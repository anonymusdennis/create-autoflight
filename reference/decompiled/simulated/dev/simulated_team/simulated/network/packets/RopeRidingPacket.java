package dev.simulated_team.simulated.network.packets;

import com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler;
import dev.simulated_team.simulated.Simulated;
import foundry.veil.api.network.handler.ServerPacketContext;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;

public record RopeRidingPacket(UUID uuid, boolean stop) implements CustomPacketPayload {
   public static Type<RopeRidingPacket> TYPE = new Type(Simulated.path("ride_rope"));
   public static StreamCodec<RegistryFriendlyByteBuf, RopeRidingPacket> CODEC = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC, RopeRidingPacket::uuid, ByteBufCodecs.BOOL, RopeRidingPacket::stop, RopeRidingPacket::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public void handle(ServerPacketContext ctx) {
      ServerPlayer player = ctx.player();
      player.connection.aboveGroundTickCount = 0;
      player.connection.aboveGroundVehicleTickCount = 0;
      player.fallDistance = 0.0F;
      if (this.stop) {
         ServerChainConveyorHandler.handleStopRidingPacket(player);
      } else {
         ServerChainConveyorHandler.handleTTLPacket(player);
      }
   }
}
