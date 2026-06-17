package dev.ryanhcode.sable.mixin.player_freezing;

import dev.ryanhcode.sable.mixinterface.player_freezing.PlayerFreezeExtension;
import dev.ryanhcode.sable.mixinterface.respawn_point.ServerPlayerRespawnExtension;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFreezePlayerPacket;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity.RemovalReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerList.class})
public class PlayerListMixin {
   @Inject(
      method = {"placeNewPlayer"},
      at = {@At("TAIL")}
   )
   private void sable$player(Connection connection, ServerPlayer serverPlayer, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
      if (serverPlayer instanceof PlayerFreezeExtension extension) {
         UUID uuid = extension.sable$getFrozenToSubLevel();
         if (uuid != null) {
            serverPlayer.connection
               .send(new ClientboundCustomPayloadPacket(new ClientboundFreezePlayerPacket(uuid, extension.sable$getFrozenToSubLevelAnchor())));
         }
      }
   }

   @Inject(
      method = {"respawn"},
      at = {@At("TAIL")}
   )
   private void sable$respawn(ServerPlayer oldPlayer, boolean bl, RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
      ServerPlayer newPlayer = (ServerPlayer)cir.getReturnValue();
      ((ServerPlayerRespawnExtension)newPlayer).sable$takeQueuedFreezeFrom(oldPlayer);
   }
}
