package net.createmod.catnip.net.base;

import net.minecraft.server.level.ServerPlayer;

public non-sealed interface ServerboundPacketPayload extends BasePacketPayload {
   void handle(ServerPlayer var1);
}
