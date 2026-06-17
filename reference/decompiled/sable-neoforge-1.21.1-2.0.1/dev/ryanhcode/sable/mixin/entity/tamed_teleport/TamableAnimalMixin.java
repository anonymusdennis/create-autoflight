package dev.ryanhcode.sable.mixin.entity.tamed_teleport;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({TamableAnimal.class})
public class TamableAnimalMixin {
   @Unique
   private static final BoundingBox3d sable$BOX = new BoundingBox3d();

   @WrapOperation(
      method = {"maybeTeleportTo"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/TamableAnimal;canTeleportTo(Lnet/minecraft/core/BlockPos;)Z"
      )}
   )
   private static boolean sable$blockPosition(TamableAnimal instance, BlockPos blockPos, Operation<Boolean> original) {
      SubLevel subLevel = Sable.HELPER.getTrackingSubLevel(instance.getOwner());
      if (subLevel != null) {
         BlockPos pos = BlockPos.containing(subLevel.logicalPose().transformPositionInverse(blockPos.getCenter()));
         if ((Boolean)original.call(new Object[]{instance, pos})) {
            double dot = subLevel.logicalPose().transformNormal(new Vector3d(0.0, 1.0, 0.0)).dot(OrientedBoundingBox3d.UP);
            if (dot > 0.85) {
               return true;
            }
         }
      }

      sable$BOX.set(instance.getBoundingBox().move(blockPos.subtract(instance.blockPosition())));

      for (SubLevel subLevel1 : Sable.HELPER.getAllIntersecting(instance.level(), sable$BOX)) {
         Vector3d center = sable$BOX.center();
         BlockPos pos = BlockPos.containing(subLevel1.logicalPose().transformPositionInverse(new Vec3(center.x(), center.y(), center.z())));
         if (!instance.level().getBlockState(pos).isAir()) {
            return false;
         }
      }

      return (Boolean)original.call(new Object[]{instance, blockPos});
   }
}
