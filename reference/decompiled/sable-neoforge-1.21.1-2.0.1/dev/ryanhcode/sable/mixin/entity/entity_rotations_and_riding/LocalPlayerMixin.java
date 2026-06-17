package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.mojang.authlib.GameProfile;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LocalPlayer.class})
public abstract class LocalPlayerMixin extends Player {
   public LocalPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
      super(level, blockPos, f, gameProfile);
   }

   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;",
         ordinal = 0
      )
   )
   private Vec3 sable$modifyFlightDir(Vec3 instance, double x, double y, double z) {
      Quaterniondc orientation = EntitySubLevelUtil.getCustomEntityOrientation(this, 1.0F);
      if (orientation == null) {
         return instance.add(x, y, z);
      } else {
         Vector3d dir = orientation.transform(new Vector3d(x, y, z));
         return instance.add(dir.x, dir.y, dir.z);
      }
   }

   @Unique
   public final Vec3 sable$calculateViewVector2(float f, float g) {
      float h = f * (float) (Math.PI / 180.0);
      float i = -g * (float) (Math.PI / 180.0);
      float j = Mth.cos(i);
      float k = Mth.sin(i);
      float l = Mth.cos(h);
      float m = Mth.sin(h);
      return new Vec3((double)(k * l), (double)(-m), (double)(j * l));
   }

   @Inject(
      method = {"startRiding(Lnet/minecraft/world/entity/Entity;Z)Z"},
      at = {@At("RETURN")}
   )
   private void sable$onStartRiding(Entity entity, boolean bl, CallbackInfoReturnable<Boolean> cir) {
      if ((Boolean)cir.getReturnValue() && EntitySubLevelUtil.shouldKick(this)) {
         Entity vehicle = this.getVehicle();
         if (vehicle != null) {
            SubLevel subLevel = Sable.HELPER.getContaining(vehicle);
            if (subLevel != null && EntitySubLevelUtil.shouldKick(this)) {
               Vec3 lookDir = this.sable$calculateViewVector2(this.getXRot(), this.getYRot());
               Vec3 localLookDir = subLevel.logicalPose().transformNormalInverse(lookDir);
               vehicle.positionRider(this);
               EntitySubLevelUtil.setOldPosNoMovement(this);
               this.lookAt(Anchor.FEET, this.position().add(localLookDir));
            }
         }
      }
   }

   @Inject(
      method = {"removeVehicle"},
      at = {@At("HEAD")}
   )
   private void sable$onStopRiding(CallbackInfo ci) {
      if (EntitySubLevelUtil.shouldKick(this)) {
         Entity vehicle = this.getVehicle();
         if (vehicle != null) {
            SubLevel subLevel = Sable.HELPER.getContaining(vehicle);
            if (subLevel != null) {
               Vec3 lookDir = this.sable$calculateViewVector2(this.getXRot(), this.getYRot());
               Vec3 globalLookDir = subLevel.logicalPose().transformNormal(lookDir);
               this.lookAt(Anchor.FEET, this.position().add(globalLookDir));
            }
         }
      }
   }

   @Unique
   private void sable$dismountVehicle(Entity entity) {
      ActiveSableCompanion helper = Sable.HELPER;
      Level level = this.level();
      Vector3d dismountPos;
      if (this.isRemoved()) {
         dismountPos = JOMLConversion.toJOML(this.position());
      } else if (!entity.isRemoved() && !level.getBlockState(entity.blockPosition()).is(BlockTags.PORTALS)) {
         dismountPos = JOMLConversion.toJOML(entity.getDismountLocationForPassenger(this));
      } else {
         double d = Math.max(this.getY(), helper.projectOutOfSubLevel(level, entity.position()).y);
         dismountPos = new Vector3d(this.getX(), d, this.getZ());
      }

      helper.projectOutOfSubLevel(level, dismountPos);
      this.setPos(dismountPos.x, dismountPos.y, dismountPos.z);
   }

   public void stopRiding() {
      Entity vehicle = this.getVehicle();
      super.stopRiding();
      if (this.level().isClientSide && vehicle != null && vehicle != this.getVehicle() && Sable.HELPER.getContaining(vehicle) != null) {
         this.sable$dismountVehicle(vehicle);
      }
   }
}
