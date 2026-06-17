package dev.ryanhcode.sable.mixin.particle;

import dev.ryanhcode.sable.Sable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({LevelRenderer.class})
public class LevelRendererMixin {
   @Shadow
   @Nullable
   private ClientLevel level;

   @Redirect(
      method = {"addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(DDD)D"
      )
   )
   private double sable$addParticleInternal(Vec3 vec, double x, double y, double z) {
      return Sable.HELPER.distanceSquaredWithSubLevels(this.level, vec, x, y, z);
   }
}
