package dev.ryanhcode.sable.api.entity;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.index.SableTags;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

public class EntitySubLevelUtil {
   public static void setOldPosNoMovement(Entity entity) {
      SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(entity);
      if (trackingSubLevel != null) {
         Vec3 entityPos = entity.position();
         Vec3 oldPos = trackingSubLevel.lastPose().transformPosition(trackingSubLevel.logicalPose().transformPositionInverse(entityPos));
         entity.xOld = oldPos.x;
         entity.xo = oldPos.x;
         entity.yOld = oldPos.y;
         entity.yo = oldPos.y;
         entity.zOld = oldPos.z;
         entity.zo = oldPos.z;
      } else {
         entity.xOld = entity.getX();
         entity.xo = entity.getX();
         entity.yOld = entity.getY();
         entity.yo = entity.getY();
         entity.zOld = entity.getZ();
         entity.zo = entity.getZ();
      }
   }

   public static void kickEntity(SubLevel subLevel, Entity entity) {
      Vector3d subLevelGainedVelo = new Vector3d();
      if (entity instanceof AbstractHurtingProjectile ahp && ahp.accelerationPower == 0.0) {
         Sable.HELPER.getVelocity(entity.level(), JOMLConversion.toJOML(entity.position()), subLevelGainedVelo);
      }

      subLevelGainedVelo.mul(0.05);
      Vec3 pos = entity.position();
      Vec3 anchor = Vec3.ZERO;
      if (entity instanceof FallingBlockEntity) {
         anchor = new Vec3(0.0, (double)entity.getBbHeight() / 2.0, 0.0);
      }

      entity.moveTo(subLevel.logicalPose().transformPosition(pos.add(anchor)).subtract(anchor));
      entity.setDeltaMovement(
         subLevel.logicalPose().transformNormal(entity.getDeltaMovement()).add(subLevelGainedVelo.x, subLevelGainedVelo.y, subLevelGainedVelo.z)
      );
      entity.lookAt(Anchor.FEET, subLevel.logicalPose().transformNormal(entity.getLookAngle()).add(entity.position()));
      if (entity instanceof AbstractArrow) {
         Vec3 deltaMovement = entity.getDeltaMovement();
         double horizontal = deltaMovement.horizontalDistance();
         entity.setYRot((float)(Mth.atan2(deltaMovement.x, deltaMovement.z) * 180.0 / (float) Math.PI));
         entity.setXRot((float)(Mth.atan2(deltaMovement.y, horizontal) * 180.0 / (float) Math.PI));
      }
   }

   public static boolean shouldKick(Entity entity) {
      return !entity.getType().is(SableTags.RETAIN_IN_SUB_LEVEL);
   }

   @Nullable
   public static Quaterniondc getCustomEntityOrientation(Entity entity, float partialTicks) {
      return null;
   }

   public static boolean hasCustomEntityOrientation(Entity entity) {
      return false;
   }
}
