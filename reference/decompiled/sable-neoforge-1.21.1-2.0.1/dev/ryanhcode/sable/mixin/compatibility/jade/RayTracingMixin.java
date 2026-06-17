package dev.ryanhcode.sable.mixin.compatibility.jade;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.Optional;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import snownee.jade.overlay.RayTracing;

@Mixin({RayTracing.class})
public class RayTracingMixin {
   @WrapOperation(
      method = {"getEntityHitResult"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"
      )}
   )
   private static Optional<Vec3> sable$clip(AABB aabb, Vec3 start, Vec3 end, Operation<Optional<Vec3>> original, @Local(argsOnly = true) Level worldIn) {
      ClientSubLevel subLevel = (ClientSubLevel)Sable.HELPER.getContaining(worldIn, aabb.getCenter());
      if (subLevel != null) {
         Pose3dc renderPose = subLevel.renderPose();
         start = renderPose.transformPositionInverse(start);
         end = renderPose.transformPositionInverse(end);
         return aabb.clip(start, end);
      } else {
         return (Optional<Vec3>)original.call(new Object[]{aabb, start, end});
      }
   }

   @WrapOperation(
      method = {"getEntityHitResult"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
      )}
   )
   private static double sable$distanceToSqr(Vec3 instance, Vec3 vec3, Operation<Double> original, @Local(argsOnly = true) Level worldIn) {
      return (Double)original.call(new Object[]{instance, Sable.HELPER.projectOutOfSubLevel(worldIn, vec3)});
   }
}
