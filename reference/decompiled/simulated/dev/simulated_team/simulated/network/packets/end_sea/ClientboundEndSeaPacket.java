package dev.simulated_team.simulated.network.packets.end_sea;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysics;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysicsData;
import foundry.veil.api.network.handler.ClientPacketContext;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ClientboundEndSeaPacket(List<EndSeaPhysics> physics) implements CustomPacketPayload {
   public static final Type<ClientboundEndSeaPacket> TYPE = new Type(Simulated.path("end_sea"));
   public static final StreamCodec<ByteBuf, ClientboundEndSeaPacket> CODEC = StreamCodec.of((buf, value) -> value.write(buf), ClientboundEndSeaPacket::read);

   private static ClientboundEndSeaPacket read(ByteBuf buf) {
      int entries = buf.readInt();
      ArrayList<EndSeaPhysics> physics = new ArrayList<>();

      for (int i = 0; i < entries; i++) {
         physics.add((EndSeaPhysics)EndSeaPhysics.STREAM_CODEC.decode(buf));
      }

      return new ClientboundEndSeaPacket(physics);
   }

   private void write(ByteBuf buf) {
      buf.writeInt(this.physics.size());

      for (EndSeaPhysics physics : this.physics) {
         EndSeaPhysics.STREAM_CODEC.encode(buf, physics);
      }
   }

   public void handle(ClientPacketContext context) {
      Minecraft.getInstance().execute(() -> EndSeaPhysicsData.handleDataPacket(this));
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
