package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.particles;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.kinetics.fan.AirFlowParticle;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AirFlowParticle.class})
public abstract class AirFlowParticleMixin extends SimpleAnimatedParticle {
   @Unique
   Vec3 sable$subLevelOrientation;
   @Shadow
   @Final
   private IAirCurrentSource source;

   protected AirFlowParticleMixin(ClientLevel arg, double d, double e, double f, SpriteSet arg2, float g) {
      super(arg, d, e, f, arg2, g);
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void sable$fixAirflowParticle(CallbackInfo ci) {
      if (this.source == null || this.source.getAirCurrent() == null || this.source.getAirCurrent().direction == null) {
         this.remove();
         ci.cancel();
      }
   }

   @Redirect(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/AABB;contains(DDD)Z",
         ordinal = 0
      )
   )
   public boolean sable$reverseProjectPos(AABB instance, double x, double y, double z) {
      SubLevel subLevel = Sable.HELPER.getContainingClient(this.source.getAirCurrentPos());
      return subLevel != null ? true : instance.contains(x, y, z);
   }

   @Redirect(
      method = {"tick"},
      at = @At(
         value = "NEW",
         target = "(DDD)Lnet/minecraft/world/phys/Vec3;",
         ordinal = 0
      )
   )
   public Vec3 sable$reverseProjectPos2(double x, double y, double z) {
      SubLevel subLevel = Sable.HELPER.getContainingClient(this.source.getAirCurrentPos());
      return subLevel != null ? subLevel.logicalPose().transformPositionInverse(new Vec3(x, y, z)) : new Vec3(x, y, z);
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/kinetics/fan/IAirCurrentSource;getAirCurrent()Lcom/simibubi/create/content/kinetics/fan/AirCurrent;",
         ordinal = 1
      )}
   )
   public void sable$transformNormal(CallbackInfo ci, @Local(ordinal = 1) LocalRef<Vec3> motion) {
      SubLevel subLevel = Sable.HELPER.getContainingClient(this.source.getAirCurrentPos());
      if (subLevel != null) {
         if (this.sable$subLevelOrientation == null) {
            this.sable$subLevelOrientation = subLevel.logicalPose().transformNormal((Vec3)motion.get());
         }
      } else {
         this.sable$subLevelOrientation = (Vec3)motion.get();
      }

      motion.set(this.sable$subLevelOrientation);
   }
}
