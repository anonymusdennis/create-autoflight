package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_riding_sub_level_vehicle.EntityRidingSubLevelVehicleHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class EntityMixin {
   @Shadow
   private Level level;
   @Shadow
   @Nullable
   private Entity vehicle;
   @Shadow
   private Vec3 position;

   @Shadow
   public abstract void setPos(Vec3 var1);

   @Shadow
   public abstract boolean hasPassenger(Entity var1);

   @Shadow
   public abstract Vec3 position();

   @Shadow
   protected abstract ListTag newDoubleList(double... var1);

   @Shadow
   public abstract double getX();

   @Shadow
   public abstract double getY();

   @Shadow
   public abstract double getZ();

   @Shadow
   public abstract Level level();

   @Shadow
   @Nullable
   public abstract Entity getVehicle();

   @Shadow
   public abstract Vec3 getLookAngle();

   @Shadow
   public abstract void lookAt(Anchor var1, Vec3 var2);

   @Shadow
   public abstract float getXRot();

   @Shadow
   public abstract float getYRot();

   @Shadow
   protected static Vec3 getInputVector(Vec3 vec3, float f, float g) {
      return null;
   }

   @Shadow
   public abstract void setDeltaMovement(Vec3 var1);

   @Shadow
   public abstract Vec3 getDeltaMovement();

   @WrapOperation(
      method = {"move"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;horizontalDistance()D"
      )}
   )
   private double sable$fixWalkDistance(Vec3 vec, Operation<Double> original) {
      Quaterniondc orientation = EntitySubLevelUtil.getCustomEntityOrientation((Entity)this, 1.0F);
      return orientation == null
         ? (Double)original.call(new Object[]{vec})
         : (Double)original.call(new Object[]{JOMLConversion.toMojang(orientation.transformInverse(JOMLConversion.toJOML(vec)))});
   }

   @Inject(
      method = {"moveRelative"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void moveRelative(float f, Vec3 vec3, CallbackInfo ci) {
      Quaterniondc orientation = EntitySubLevelUtil.getCustomEntityOrientation((Entity)this, 1.0F);
      if (orientation != null) {
         Vec3 inputVector = getInputVector(vec3, f, this.getYRot());
         Vec3 impulse = JOMLConversion.toMojang(orientation.transform(JOMLConversion.toJOML(inputVector)));
         this.setDeltaMovement(this.getDeltaMovement().add(impulse));
         ci.cancel();
      }
   }

   @Inject(
      method = {"rideTick"},
      at = {@At("TAIL")}
   )
   public void sable$onRidingTick(CallbackInfo ci) {
      if (this.vehicle != null) {
         ActiveSableCompanion helper = Sable.HELPER;
         SubLevel vehicleSubLevel = helper.getContaining(this.vehicle);
         if (vehicleSubLevel != null) {
            if (helper.getContaining(this.level, this.position) == vehicleSubLevel) {
               Vec3 pos = EntityRidingSubLevelVehicleHelper.kickRidingEntity((Entity)this, vehicleSubLevel);
               this.setPos(pos);
            }
         }
      }
   }

   @Inject(
      method = {"positionRider(Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At("TAIL")}
   )
   public void sable$onPositionRider(Entity entity, CallbackInfo ci) {
      if (this.hasPassenger(entity)) {
         SubLevel subLevel = Sable.HELPER.getContaining(this.level, entity.position());
         if (subLevel != null) {
            Vec3 pos = EntityRidingSubLevelVehicleHelper.kickRidingEntity(entity, subLevel);
            entity.setPos(pos);
         }
      }
   }

   @Redirect(
      method = {"saveWithoutId"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/nbt/CompoundTag;put(Ljava/lang/String;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;",
         ordinal = 0
      )
   )
   public Tag sable$fixPassengerSaving(CompoundTag instance, String string, Tag tag) {
      if (!EntitySubLevelUtil.shouldKick((Entity)this)) {
         return instance.put(string, tag);
      } else {
         SubLevel subLevel = Sable.HELPER.getContaining(this.vehicle);
         if (subLevel != null) {
            Tag newPositionTag = this.newDoubleList(this.getX(), this.getY(), this.getZ());
            if (this instanceof ServerPlayer serverPlayer) {
               SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad((ServerLevel)this.level());
               UUID loginPointUUID = data.generateTrackingPoint(serverPlayer, (ServerSubLevel)subLevel);
               if (loginPointUUID != null) {
                  instance.putUUID("LoginPoint", loginPointUUID);
               }
            }

            return instance.put(string, newPositionTag);
         } else {
            return instance.put(string, tag);
         }
      }
   }
}
