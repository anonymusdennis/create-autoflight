package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.packagePort.PackagePortPlacementPacket;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({PackagePortPlacementPacket.class})
public class PackagePortPlacementPacketMixin {
   @Redirect(
      method = {"handle"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"
      )
   )
   private boolean sable$handle(Vec3 instance, Position position, double d, @Local(argsOnly = true) ServerPlayer player) {
      return Sable.HELPER.distanceSquaredWithSubLevels(player.level(), instance, position.x(), position.y(), position.z()) < d * d;
   }
}
