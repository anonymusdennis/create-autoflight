package dev.ryanhcode.sable.mixin.udp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.network.udp.SableUDPServer;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerList.class})
public class PlayerListMixin {
   @Inject(
      method = {"placeNewPlayer"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V",
         ordinal = 0,
         shift = Shift.AFTER
      )}
   )
   private void onPlayerJoin(Connection connection, ServerPlayer serverPlayer, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
      SableUDPServer server = SableUDPServer.getServer(serverPlayer.server);
      if (server != null) {
         Sable.LOGGER.info("Beginning attempted authentication with player {}", serverPlayer.getName().getString());
         server.beginAuthentication(serverPlayer);
      }
   }
}
