package dev.ryanhcode.sable.mixin.camera.camera_rotation;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin({CompassItemPropertyFunction.class})
public abstract class CompassItemPropertyFunctionMixin {
   @Overwrite
   private double getAngleFromEntityToPos(Entity entity, BlockPos pos) {
      Vec3 localPos = Vec3.atCenterOf(pos);
      double entityX = entity.getX();
      double entityZ = entity.getZ();
      ActiveSableCompanion helper = Sable.HELPER;
      SubLevel subLevel = helper.getContaining(entity);
      if (subLevel == null) {
         Entity vehicle = entity.getVehicle();
         if (vehicle != null) {
            subLevel = helper.getContaining(vehicle);
            if (subLevel != null) {
               Vec3 localEntityPos = subLevel.lastPose().transformPositionInverse(entity.position());
               entityX = localEntityPos.x;
               entityZ = localEntityPos.z;
            }
         }
      }

      if (subLevel != null) {
         localPos = subLevel.lastPose().transformPositionInverse(localPos);
      }

      return Math.atan2(localPos.z() - entityZ, localPos.x() - entityX) / (float) (Math.PI * 2);
   }
}
