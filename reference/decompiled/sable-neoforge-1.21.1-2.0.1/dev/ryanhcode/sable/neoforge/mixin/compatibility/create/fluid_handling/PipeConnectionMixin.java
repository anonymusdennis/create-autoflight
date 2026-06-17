package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.fluid_handling;

import com.simibubi.create.content.fluids.PipeConnection;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({PipeConnection.class})
public class PipeConnectionMixin {
   @Redirect(
      method = {"isRenderEntityWithinDistance"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"
      )
   )
   private static double sable$distanceIncludingSubLevels(Vec3 instance, Vec3 vec3) {
      return Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(Minecraft.getInstance().level, instance, vec3));
   }
}
