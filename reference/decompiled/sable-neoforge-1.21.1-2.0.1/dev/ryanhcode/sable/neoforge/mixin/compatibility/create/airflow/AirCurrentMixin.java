package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.airflow;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.lang.ref.WeakReference;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AirCurrent.class})
public abstract class AirCurrentMixin {
   @Shadow
   @Final
   public IAirCurrentSource source;
   @Unique
   private WeakReference<SubLevel> sable$subLevelReference;

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   public void sable$updateSubLevel(CallbackInfo ci) {
      if (this.sable$subLevelReference == null) {
         this.sable$subLevelReference = new WeakReference<>(Sable.HELPER.getContaining(this.source.getAirCurrentWorld(), this.source.getAirCurrentPos()));
      }
   }

   @Redirect(
      method = {"tickAffectedEntities"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"
      )
   )
   public AABB sable$reverseProjectEntityBB(Entity instance) {
      SubLevel subLevel = this.sable$subLevelReference.get();
      return subLevel != null
         ? new BoundingBox3d(instance.getBoundingBox()).transformInverse(subLevel.logicalPose(), new BoundingBox3d()).toMojang()
         : instance.getBoundingBox();
   }

   @WrapOperation(
      method = {"tickAffectedEntities"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   public void sable$transformFlowVector(
      Entity instance,
      Vec3 vec3,
      Operation<Void> original,
      @Local Vec3i flow,
      @Local(ordinal = 2) float acceleration,
      @Local Vec3 previousMotion,
      @Local(ordinal = 3) float maxAcceleration
   ) {
      SubLevel subLevel = this.sable$subLevelReference.get();
      if (subLevel != null) {
         Vector3d nonIntFlow = JOMLConversion.atLowerCornerOf(flow);
         subLevel.logicalPose().transformNormal(nonIntFlow);
         double xIn = Mth.clamp((double)((float)nonIntFlow.get(0) * acceleration) - previousMotion.x, (double)(-maxAcceleration), (double)maxAcceleration);
         double yIn = Mth.clamp((double)((float)nonIntFlow.get(1) * acceleration) - previousMotion.y, (double)(-maxAcceleration), (double)maxAcceleration);
         double zIn = Mth.clamp((double)((float)nonIntFlow.get(2) * acceleration) - previousMotion.z, (double)(-maxAcceleration), (double)maxAcceleration);
         original.call(new Object[]{instance, previousMotion.add(new Vec3(xIn, yIn, zIn).scale(0.125))});
      } else {
         original.call(new Object[]{instance, vec3});
      }
   }

   @Redirect(
      method = {"tickAffectedEntities"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;"
      )
   )
   public Vec3 sable$reverseProjectAllPositions(Entity instance) {
      SubLevel subLevel = this.sable$subLevelReference.get();
      return subLevel != null ? subLevel.logicalPose().transformPositionInverse(instance.position()) : instance.position();
   }
}
