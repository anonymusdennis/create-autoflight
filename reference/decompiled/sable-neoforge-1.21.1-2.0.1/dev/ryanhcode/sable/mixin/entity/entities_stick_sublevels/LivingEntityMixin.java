package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.LivingEntityStickExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public abstract class LivingEntityMixin extends Entity implements LivingEntityStickExtension {
   @Shadow
   protected int lerpSteps;
   @Shadow
   protected double lerpYRot;
   @Shadow
   protected double lerpXRot;
   @Unique
   private Vec3 sable$lerpTarget = Vec3.ZERO;
   @Unique
   private int sable$sableLerpSteps;
   @Unique
   private int sable$sableRotLerpSteps;

   @Shadow
   protected abstract void updateWalkAnimation(float var1);

   public LivingEntityMixin(EntityType<?> entityType, Level level) {
      super(entityType, level);
   }

   @Override
   public void sable$setupLerp() {
      if (this.sable$getPlotPosition() != null && this.lerpSteps > 0) {
         this.sable$sableRotLerpSteps = this.lerpSteps;
         this.lerpSteps = 0;
      }
   }

   @Override
   public void sable$applyLerp() {
      Vec3 plotPos = this.sable$getPlotPosition();
      if (plotPos == null) {
         this.sable$sableLerpSteps = 0;
         this.sable$sableRotLerpSteps = 0;
      } else {
         if (this.sable$sableLerpSteps > 0) {
            this.sable$setPlotPosition(plotPos.lerp(this.sable$lerpTarget, 1.0 / (double)this.sable$sableLerpSteps));
            this.sable$sableLerpSteps--;
         }

         if (this.sable$sableRotLerpSteps > 0) {
            double difference = Mth.wrapDegrees(this.lerpYRot - (double)this.getYRot());
            this.setYRot(this.getYRot() + (float)difference / (float)this.sable$sableRotLerpSteps);
            this.setXRot(this.getXRot() + (float)(this.lerpXRot - (double)this.getXRot()) / (float)this.sable$sableRotLerpSteps);
            this.sable$sableRotLerpSteps--;
            this.setRot(this.getYRot(), this.getXRot());
         }
      }
   }

   @Override
   public Vec3 sable$getLerpTarget() {
      return this.sable$lerpTarget;
   }

   @Inject(
      method = {"aiStep"},
      at = {@At("HEAD")}
   )
   private void sable$updateRotLerp(CallbackInfo ci) {
      this.sable$setupLerp();
   }

   @Inject(
      method = {"aiStep"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V",
         shift = Shift.BEFORE
      )}
   )
   private void sable$updatePlotPosition(CallbackInfo ci) {
      this.sable$applyLerp();
   }

   @Override
   public void sable$plotLerpTo(Vec3 pos, int lerpSteps) {
      this.sable$lerpTarget = pos;
      this.sable$sableLerpSteps = lerpSteps;
   }

   @ModifyVariable(
      method = {"tick"},
      at = @At("STORE"),
      ordinal = 0
   )
   private double sable$modifyXDifference(double x) {
      return this.sable$getDifference(true).x;
   }

   @ModifyVariable(
      method = {"tick"},
      at = @At("STORE"),
      ordinal = 1
   )
   private double sable$modifyZDifference(double x) {
      return this.sable$getDifference(true).z;
   }

   @Redirect(
      method = {"calculateEntityAnimation"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;updateWalkAnimation(F)V"
      )
   )
   private void sable$walkAnimation(LivingEntity instance, float g, boolean pIncludeHeight) {
      Vec3 delta = this.sable$getDifference(false);
      float f = (float)Mth.length(delta.x, pIncludeHeight ? delta.y : 0.0, delta.z);
      this.updateWalkAnimation(f);
   }

   @Unique
   private Vec3 sable$getDifference(boolean countLocalPlayer) {
      Vec3 currentPos = this.position();
      Vec3 oldPos = new Vec3(this.xo, this.yo, this.zo);
      Vec3 delta = currentPos.subtract(oldPos);
      SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(this);
      if (trackingSubLevel != null && (countLocalPlayer || !(this instanceof Player player) || !player.isLocalPlayer())) {
         Pose3d pose = trackingSubLevel.logicalPose();
         Pose3dc lastPose = trackingSubLevel.lastPose();
         currentPos = pose.transformPositionInverse(currentPos);
         oldPos = lastPose.transformPositionInverse(oldPos);
         delta = currentPos.subtract(oldPos);
         delta = pose.transformNormal(delta);
      }

      Quaterniondc orientation = EntitySubLevelUtil.getCustomEntityOrientation(this, 1.0F);
      if (orientation != null) {
         delta = JOMLConversion.toMojang(orientation.transformInverse(JOMLConversion.toJOML(delta)));
      }

      return delta;
   }

   @Inject(
      method = {"recreateFromPacket"},
      at = {@At("TAIL")}
   )
   public void sable$recreateFromPacket(ClientboundAddEntityPacket packet, CallbackInfo ci) {
      if (EntitySubLevelUtil.shouldKick(this)) {
         double packetX = packet.getX();
         double packetY = packet.getY();
         double packetZ = packet.getZ();
         SubLevel packetSubLevel = Sable.HELPER.getContaining(this.level(), packetX, packetZ);
         if (packetSubLevel != null) {
            Vector3d globalPacketPos = packetSubLevel.logicalPose().transformPosition(new Vector3d(packetX, packetY, packetZ));
            this.setPos(globalPacketPos.x, globalPacketPos.y, globalPacketPos.z);
         }
      }
   }
}
