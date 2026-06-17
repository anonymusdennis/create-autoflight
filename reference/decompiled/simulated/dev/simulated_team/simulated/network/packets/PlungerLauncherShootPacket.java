package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer;
import foundry.veil.api.network.handler.PacketContext;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

public class PlungerLauncherShootPacket implements CustomPacketPayload {
   public static Type<PlungerLauncherShootPacket> TYPE = new Type(Simulated.path("plunger_launcher_shoot"));
   public static final StreamCodec<ByteBuf, PlungerLauncherShootPacket> CODEC = StreamCodec.composite(
      CatnipStreamCodecs.HAND, packet -> packet.hand, PlungerLauncherShootPacket::new
   );
   protected final InteractionHand hand;

   public PlungerLauncherShootPacket(InteractionHand hand) {
      this.hand = hand;
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public void handle(PacketContext context) {
      Entity renderViewEntity = Minecraft.getInstance().getCameraEntity();
      if (renderViewEntity != null) {
         PlungerLauncherItemRenderer.RenderHandler handler = SimulatedClient.PLUNGER_LAUNCHER_RENDER_HANDLER;
         handler.basicShoot(this.hand);
         handler.playSound(this.hand, context.player().position());
      }
   }
}
