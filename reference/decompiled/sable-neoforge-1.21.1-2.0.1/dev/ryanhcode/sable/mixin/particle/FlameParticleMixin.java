package dev.ryanhcode.sable.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.Particle;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({FlameParticle.class})
public abstract class FlameParticleMixin extends Particle implements ParticleExtension {
   protected FlameParticleMixin(ClientLevel clientLevel, double d, double e, double f) {
      super(clientLevel, d, e, f);
   }

   @WrapMethod(
      method = {"move"}
   )
   public void move(double motionX, double motionY, double motionZ, Operation<Void> original) {
      SubLevel trackingSubLevel = this.sable$getTrackingSubLevel();
      if (trackingSubLevel != null && !trackingSubLevel.isRemoved()) {
         Pose3dc pose = trackingSubLevel.logicalPose();
         Pose3dc last = trackingSubLevel.lastPose();
         Vector3dc globalBoundsCenter = JOMLConversion.getAABBCenter(this.getBoundingBox());
         Vector3d localPosition = last.transformPositionInverse(globalBoundsCenter, new Vector3d());
         Vector3d newGlobalPosition = pose.transformPosition(localPosition);
         original.call(
            new Object[]{
               motionX + newGlobalPosition.x - globalBoundsCenter.x(),
               motionY + newGlobalPosition.y - globalBoundsCenter.y(),
               motionZ + newGlobalPosition.z - globalBoundsCenter.z()
            }
         );
      } else {
         original.call(new Object[]{motionX, motionY, motionZ});
      }
   }
}
