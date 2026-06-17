package dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.camera.new_camera_types.SableCameraTypes;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;

public class EntitySubLevelRotationHelper {
   public static boolean shouldCameraRotate() {
      return Minecraft.getInstance().options.getCameraType() != SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED;
   }

   @Nullable
   public static Quaterniond getEntityOrientation(
      Entity cameraEntity, Function<SubLevel, Pose3dc> poseProvider, float partialTicks, EntitySubLevelRotationHelper.Type type
   ) {
      Quaterniond ridingOrientation = getSubLevelInheritedOrientation(cameraEntity, poseProvider, type);
      if (ridingOrientation != null) {
         return ridingOrientation;
      } else {
         Quaterniondc entityOrientation = EntitySubLevelUtil.getCustomEntityOrientation(cameraEntity, partialTicks);
         return entityOrientation != null ? new Quaterniond(entityOrientation) : null;
      }
   }

   public static Quaterniond getSubLevelInheritedOrientation(
      Entity cameraEntity, Function<SubLevel, Pose3dc> poseProvider, EntitySubLevelRotationHelper.Type type
   ) {
      if (type == EntitySubLevelRotationHelper.Type.CAMERA && cameraEntity instanceof Player player && player.isLocalPlayer() && !shouldCameraRotate()) {
         return null;
      }

      ActiveSableCompanion helper = Sable.HELPER;
      if (cameraEntity instanceof LivingEntity livingEntity && livingEntity.isSleeping()) {
         Optional<BlockPos> sleepingPos = livingEntity.getSleepingPos();
         if (sleepingPos.isPresent()) {
            BlockPos pos = sleepingPos.get();
            if (helper.getContaining(livingEntity.level(), pos) instanceof ClientSubLevel clientSubLevel) {
               return new Quaterniond(clientSubLevel.renderPose().orientation());
            }
         }
      }

      if (cameraEntity == null) {
         return null;
      } else {
         Entity entity = cameraEntity.getVehicle();
         if (entity == null) {
            if (cameraEntity instanceof Player) {
               return null;
            }

            if (helper.getContaining(cameraEntity) == null) {
               return null;
            }

            entity = cameraEntity;
         }

         SubLevel subLevel = helper.getContaining(entity);
         return subLevel == null ? null : new Quaterniond(poseProvider.apply(subLevel).orientation());
      }
   }

   public static enum Type {
      CAMERA,
      ENTITY;
   }
}
