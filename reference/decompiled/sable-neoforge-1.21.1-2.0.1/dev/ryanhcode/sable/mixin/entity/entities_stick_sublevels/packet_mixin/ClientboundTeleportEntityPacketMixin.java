package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels.packet_mixin;

import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.packet_mixin.PacketActuallyInSubLevelExtension;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientboundTeleportEntityPacket.class})
public class ClientboundTeleportEntityPacketMixin implements PacketActuallyInSubLevelExtension {
   @Unique
   private boolean sable$actuallyInSubLevel;

   @Inject(
      method = {"<init>(Lnet/minecraft/network/FriendlyByteBuf;)V"},
      at = {@At("RETURN")}
   )
   private void sable$readActuallyInSubLevel(FriendlyByteBuf friendlyByteBuf, CallbackInfo ci) {
      this.sable$setActuallyInSubLevel(friendlyByteBuf.readBoolean());
   }

   @Inject(
      method = {"write"},
      at = {@At("TAIL")}
   )
   private void sable$writeActuallyInSubLevel(FriendlyByteBuf friendlyByteBuf, CallbackInfo ci) {
      friendlyByteBuf.writeBoolean(this.sable$actuallyInSubLevel);
   }

   @Override
   public void sable$setActuallyInSubLevel(boolean actuallyInSubLevel) {
      this.sable$actuallyInSubLevel = actuallyInSubLevel;
   }

   @Override
   public boolean sable$isActuallyInSubLevel() {
      return this.sable$actuallyInSubLevel;
   }
}
