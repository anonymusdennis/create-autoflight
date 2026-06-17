package dev.ryanhcode.sable.network.packets.tcp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import foundry.veil.api.network.handler.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ClientboundEnterGizmoPacket() implements SableTCPPacket {
   public static final Type<ClientboundEnterGizmoPacket> TYPE = new Type(Sable.sablePath("enter_gizmo_mode"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundEnterGizmoPacket> CODEC = StreamCodec.of(
      (buf, value) -> value.write(buf), ClientboundEnterGizmoPacket::read
   );

   private static ClientboundEnterGizmoPacket read(FriendlyByteBuf buf) {
      return new ClientboundEnterGizmoPacket();
   }

   private void write(FriendlyByteBuf buf) {
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   @Override
   public void handle(PacketContext context) {
      SableClient.GIZMO_HANDLER.start();
   }
}
