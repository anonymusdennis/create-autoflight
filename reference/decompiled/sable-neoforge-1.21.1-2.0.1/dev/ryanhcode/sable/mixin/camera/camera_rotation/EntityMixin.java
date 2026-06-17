package dev.ryanhcode.sable.mixin.camera.camera_rotation;

import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityMixin {
   @Shadow
   private Level level;

   @Inject(
      method = {"calculateViewVector"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void sable$calculateViewVector(float f, float g, CallbackInfoReturnable<Vec3> cir) {
      Function<SubLevel, Pose3dc> provider;
      if (this.level instanceof LevelPoseProviderExtension levelPoseProvider) {
         provider = levelPoseProvider::sable$getPose;
      } else {
         provider = SubLevel::logicalPose;
      }

      Quaterniond orientation = EntitySubLevelRotationHelper.getEntityOrientation((Entity)this, provider, 0.0F, EntitySubLevelRotationHelper.Type.CAMERA);
      if (orientation != null) {
         Vec3 viewVector = (Vec3)cir.getReturnValue();
         cir.setReturnValue(JOMLConversion.toMojang(orientation.transform(JOMLConversion.toJOML(viewVector))));
      }
   }
}
