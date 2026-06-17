package net.createmod.catnip.net.base;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

public sealed interface BasePacketPayload extends CustomPacketPayload permits ClientboundPacketPayload, ServerboundPacketPayload {
   BasePacketPayload.PacketTypeProvider getTypeProvider();

   @NonExtendable
   @NotNull
   default Type<? extends CustomPacketPayload> type() {
      return this.getTypeProvider().getType();
   }

   public interface PacketTypeProvider {
      <T extends CustomPacketPayload> Type<T> getType();
   }
}
