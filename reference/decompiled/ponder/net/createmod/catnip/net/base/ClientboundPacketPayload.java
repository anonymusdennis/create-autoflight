package net.createmod.catnip.net.base;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public non-sealed interface ClientboundPacketPayload extends BasePacketPayload {
   void handle(LocalPlayer var1);

   default void handleInternal(Player player) {
      if (player instanceof LocalPlayer localPlayer) {
         this.handle(localPlayer);
      }
   }
}
