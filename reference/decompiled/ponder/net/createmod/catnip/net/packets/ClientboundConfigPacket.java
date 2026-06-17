package net.createmod.catnip.net.packets;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.config.ui.ConfigHelper;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.ponder.Ponder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.fml.config.ModConfig.Type;

public record ClientboundConfigPacket(String path, String value) implements ClientboundPacketPayload {
   public static final StreamCodec<ByteBuf, ClientboundConfigPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, ClientboundConfigPacket::path, ByteBufCodecs.STRING_UTF8, ClientboundConfigPacket::value, ClientboundConfigPacket::new
   );

   @Override
   public BasePacketPayload.PacketTypeProvider getTypeProvider() {
      return CatnipPackets.CLIENTBOUND_CONFIG;
   }

   @Override
   public void handle(LocalPlayer player) {
      if (Minecraft.getInstance().player != null) {
         ConfigHelper.ConfigPath path;
         try {
            path = ConfigHelper.ConfigPath.parse(this.path);
         } catch (IllegalArgumentException var6) {
            player.displayClientMessage(Ponder.lang().text(var6.getMessage()).component(), false);
            return;
         }

         if (path.getType() != Type.CLIENT) {
            Ponder.LOGGER.warn("Received type-mismatched config packet on client");
         } else {
            try {
               ConfigHelper.setConfigValue(path, this.value);
               player.displayClientMessage(Component.literal("Great Success!"), false);
            } catch (ConfigHelper.InvalidValueException var4) {
               player.displayClientMessage(Component.literal("Config could not be set the the specified value!"), false);
            } catch (Exception var5) {
               player.displayClientMessage(
                  Component.literal("Something went wrong while trying to set config value. Check the client logs for more information"), false
               );
               Ponder.LOGGER.warn("Exception during client-side config value set:", var5);
            }
         }
      }
   }
}
