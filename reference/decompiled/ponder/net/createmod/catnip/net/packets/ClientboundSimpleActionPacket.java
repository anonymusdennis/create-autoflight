package net.createmod.catnip.net.packets;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.SimpleCatnipActions;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.ponder.Ponder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ClientboundSimpleActionPacket(String action, String value) implements ClientboundPacketPayload {
   public static final StreamCodec<ByteBuf, ClientboundSimpleActionPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8,
      ClientboundSimpleActionPacket::action,
      ByteBufCodecs.STRING_UTF8,
      ClientboundSimpleActionPacket::value,
      ClientboundSimpleActionPacket::new
   );
   private static final Map<String, Supplier<Consumer<String>>> actions = new HashMap<>();

   public static void addAction(String name, Supplier<Consumer<String>> action) {
      actions.put(name, action);
   }

   @Override
   public BasePacketPayload.PacketTypeProvider getTypeProvider() {
      return CatnipPackets.CLIENTBOUND_SIMPLE_ACTION;
   }

   @Override
   public void handle(LocalPlayer player) {
      if (!actions.containsKey(this.action)) {
         Ponder.LOGGER.warn("Received ClientboundSimpleActionPacket with invalid Action {}, ignoring the packet", this.action);
      } else {
         Minecraft.getInstance().execute(() -> actions.get(this.action).get().accept(this.value));
      }
   }

   static {
      addAction("test", () -> System.out::println);
      addAction("configScreen", () -> SimpleCatnipActions::configScreen);
   }
}
