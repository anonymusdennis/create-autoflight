package dev.ryanhcode.sable.mixin.sublevel_sounds;

import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ClientLevel.class})
public class ClientLevelMixin {
   @Redirect(
      method = {"playSound"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(DDD)D"
      )
   )
   private double sable$playSound(Vec3 instance, double x, double y, double z) {
      return Sable.HELPER.distanceSquaredWithSubLevels(Minecraft.getInstance().level, instance, x, y, z);
   }
}
