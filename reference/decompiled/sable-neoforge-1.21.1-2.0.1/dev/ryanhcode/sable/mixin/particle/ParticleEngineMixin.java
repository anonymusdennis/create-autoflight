package dev.ryanhcode.sable.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ParticleEngine.class})
public abstract class ParticleEngineMixin {
   @Shadow
   protected ClientLevel level;

   @Shadow
   public abstract void add(Particle var1);

   @Inject(
      method = {"add"},
      at = {@At("TAIL")}
   )
   private void sable$onParticleAdd(Particle particle, CallbackInfo ci) {
      ((ParticleExtension)particle).sable$initialKickOut();
   }

   @WrapOperation(
      method = {"*"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/particle/Particle;tick()V"
      )}
   )
   private void sable$onParticleTick(Particle instance, Operation<Void> original) {
      ParticleExtension extension = (ParticleExtension)instance;
      extension.sable$initialKickOut();
      original.call(new Object[]{instance});
      extension.sable$moveWithInheritedVelocity();
   }

   @Redirect(
      method = {"crack"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/particle/TerrainParticle;setPower(F)Lnet/minecraft/client/particle/Particle;"
      )
   )
   private Particle sable$addCrackParticle(TerrainParticle particle, float v, @Local(argsOnly = true) BlockPos pos, @Local BlockState state) {
      Vec3 particlePosition = new Vec3(particle.x, particle.y, particle.z);
      SubLevel subLevel = Sable.HELPER.getContaining(this.level, particlePosition);
      if (subLevel != null) {
         Vec3 velocity = new Vec3(particle.xd, particle.yd, particle.zd);
         Vec3 globalVelocity = subLevel.logicalPose().transformNormal(velocity);
         particle.xd = globalVelocity.x;
         particle.yd = globalVelocity.y;
         particle.zd = globalVelocity.z;
         particle.setPower(v);
         Vec3 localVelocity = subLevel.logicalPose().transformNormalInverse(new Vec3(particle.xd, particle.yd, particle.zd));
         particle.xd = localVelocity.x;
         particle.yd = localVelocity.y;
         particle.zd = localVelocity.z;
         ((ParticleExtension)particle).sable$setTrackingSubLevel((ClientSubLevel)subLevel, particlePosition);
         return particle;
      } else {
         return particle.setPower(v);
      }
   }
}
