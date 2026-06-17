package dev.ryanhcode.sable.mixin.particle;

import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ServerLevel.class})
public class ServerLevelMixin {
   @Redirect(
      method = {"sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"
      )
   )
   private boolean sable$sendParticlesCloserToCenterThan(BlockPos blockPos, Position pos, double distance) {
      return Sable.HELPER
            .distanceSquaredWithSubLevels((ServerLevel)this, pos, (double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5)
         < distance * distance;
   }
}
