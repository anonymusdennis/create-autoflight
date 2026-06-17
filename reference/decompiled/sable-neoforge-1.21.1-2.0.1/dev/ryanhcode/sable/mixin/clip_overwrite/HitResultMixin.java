package dev.ryanhcode.sable.mixin.clip_overwrite;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({HitResult.class})
public class HitResultMixin {
   @Shadow
   @Final
   protected Vec3 location;

   @Overwrite
   public double distanceTo(Entity entity) {
      return entity.distanceToSqr(this.location);
   }
}
