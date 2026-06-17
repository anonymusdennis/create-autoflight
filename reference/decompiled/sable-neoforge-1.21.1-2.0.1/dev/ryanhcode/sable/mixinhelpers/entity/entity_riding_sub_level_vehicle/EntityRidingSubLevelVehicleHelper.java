package dev.ryanhcode.sable.mixinhelpers.entity.entity_riding_sub_level_vehicle;

import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityRidingSubLevelVehicleHelper {
   public static Vec3 kickRidingEntity(Entity entity, SubLevel subLevel) {
      return EntitySubLevelUtil.shouldKick(entity) ? kickRidingEntity(entity, entity.position(), subLevel) : entity.position();
   }

   public static Vec3 kickRidingEntity(Entity entity, Vec3 position, SubLevel subLevel) {
      Vec3 eyePosition = entity.getEyePosition();
      Vec3 feetPosition = entity.position();
      return subLevel.logicalPose().transformPosition(position.add(eyePosition.subtract(feetPosition))).add(feetPosition.subtract(eyePosition));
   }
}
