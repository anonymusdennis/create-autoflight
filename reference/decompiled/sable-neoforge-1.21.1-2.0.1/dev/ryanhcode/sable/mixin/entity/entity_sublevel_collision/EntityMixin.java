package dev.ryanhcode.sable.mixin.entity.entity_sublevel_collision;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.index.SableTags;
import dev.ryanhcode.sable.mixinterface.EntityExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LevelExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import java.util.Iterator;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Entity.class},
   priority = 1100
)
public abstract class EntityMixin implements EntityMovementExtension {
   @Shadow
   public boolean horizontalCollision;
   @Shadow
   public boolean verticalCollision;
   @Shadow
   public boolean verticalCollisionBelow;
   @Shadow
   public boolean minorHorizontalCollision;
   @Unique
   private SubLevel sable$trackingSubLevel = null;
   @Unique
   private UUID sable$lastTrackingSubLevelId = null;
   @Shadow
   private Level level;
   @Shadow
   private Vec3 position;
   @Shadow
   @Nullable
   private BlockState inBlockState;
   @Shadow
   private BlockPos blockPosition;
   @Unique
   private SubLevelEntityCollision.CollisionInfo sable$collisionInfo = null;
   @Unique
   private BlockPos sable$inBlockStatePos = BlockPos.ZERO;

   @Shadow
   protected abstract Vec3 collide(Vec3 var1);

   @Shadow
   protected abstract boolean isHorizontalCollisionMinor(Vec3 var1);

   @Shadow
   public abstract Level level();

   @WrapOperation(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;setOnGroundWithMovement(ZLnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   public void sable$moveInject(Entity instance, boolean bl, Vec3 arg, Operation<Void> original) {
      this.horizontalCollision = this.sable$collisionInfo.horizontalCollision;
      this.verticalCollision = this.sable$collisionInfo.verticalCollision;
      this.verticalCollisionBelow = this.sable$collisionInfo.verticalCollisionBelow;
      this.minorHorizontalCollision = this.sable$collisionInfo.minorHorizontalCollision;
      original.call(new Object[]{instance, this.verticalCollisionBelow, arg});
   }

   @WrapOperation(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/Block;updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V"
      )}
   )
   public void updateEntityAfterFallOn(Block instance, BlockGetter arg, Entity arg2, Operation<Void> original) {
      if (this.verticalCollision) {
         original.call(new Object[]{instance, arg, arg2});
      }
   }

   @Redirect(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"
      )
   )
   public Vec3 sable$collideRedirect(Entity entity, Vec3 collisionMotion) {
      Entity self = (Entity)this;
      Vec3 velocity = Vec3.ZERO;
      if (self instanceof LivingEntity livingEntity) {
         velocity = JOMLConversion.toMojang(((LivingEntityMovementExtension)livingEntity).sable$getInheritedVelocity());
      }

      SubLevel preTrackingSubLevel = this.sable$trackingSubLevel;
      Vec3 preDeltaMovement = this.getDeltaMovement();
      this.sable$collisionInfo = SubLevelEntityCollision.collide(entity, collisionMotion, velocity, ((LevelExtension)this.level).sable$getJOMLSink());
      this.sable$collisionInfo.preTrackingSubLevel = preTrackingSubLevel;
      this.sable$collisionInfo.preDeltaMovement = preDeltaMovement;
      if (this.sable$collisionInfo.trackingSubLevel != null) {
         if (this.sable$collisionInfo.verticalCollisionBelow) {
            this.sable$trackingSubLevel = this.sable$collisionInfo.trackingSubLevel;
         }
      } else if (!(entity instanceof ServerPlayer)) {
         this.sable$trackingSubLevel = null;
      }

      Vec3 beforeVanillaCollision = this.sable$collisionInfo.motion;
      Vec3 afterVanillaCollision = this.collide(beforeVanillaCollision);
      boolean xCollision = !Mth.equal(beforeVanillaCollision.x, afterVanillaCollision.x);
      boolean zCollision = !Mth.equal(beforeVanillaCollision.z, afterVanillaCollision.z);
      this.sable$collisionInfo.horizontalCollision |= xCollision || zCollision;
      if (beforeVanillaCollision.y != afterVanillaCollision.y) {
         this.sable$trackingSubLevel = null;
      }

      this.sable$collisionInfo.verticalCollision = this.sable$collisionInfo.verticalCollision | beforeVanillaCollision.y != afterVanillaCollision.y;
      this.sable$collisionInfo.verticalCollisionBelow = this.sable$collisionInfo.verticalCollisionBelow
         | (this.sable$collisionInfo.verticalCollision && collisionMotion.y < 0.0);
      if (this.horizontalCollision) {
         this.sable$collisionInfo.minorHorizontalCollision = this.isHorizontalCollisionMinor(afterVanillaCollision);
      }

      if (this.sable$trackingSubLevel != null && this.sable$trackingSubLevel.isRemoved()) {
         this.sable$trackingSubLevel = null;
      }

      return afterVanillaCollision;
   }

   @Inject(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("TAIL")}
   )
   public void sable$moveInject(MoverType moverType, Vec3 vec3, CallbackInfo ci) {
      if (this.sable$collisionInfo != null) {
         this.horizontalCollision = this.horizontalCollision | this.sable$collisionInfo.subLevelHorizontalCollision;
      }

      if (!(this instanceof LivingEntity)
         && this.sable$collisionInfo != null
         && this.sable$collisionInfo.inheritedMotion != null
         && this.sable$collisionInfo.inheritedMotion.lengthSqr() > Math.pow(1.0E-6, 2.0)) {
         this.setPos(this.position.add(((EntityExtension)this).sable$vanillaCollide(this.sable$collisionInfo.inheritedMotion)));
      }
   }

   @Shadow
   public abstract void setPos(Vec3 var1);

   @Shadow
   @Nullable
   public abstract Entity getVehicle();

   @Shadow
   public abstract Vec3 getDeltaMovement();

   @Shadow
   public abstract AABB getBoundingBox();

   @Shadow
   public abstract void remove(RemovalReason var1);

   @Shadow
   public abstract EntityType<?> getType();

   @Shadow
   public abstract void kill();

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   public void sable$tickInject(CallbackInfo ci) {
      ActiveSableCompanion helper = Sable.HELPER;
      Entity vehicle = this.getVehicle();
      SubLevel containingSubLevel = helper.getContaining((Entity)this);
      if (containingSubLevel != null) {
         this.sable$trackingSubLevel = null;
      } else if (vehicle != null) {
         SubLevel vehicleSubLevel = helper.getContaining(vehicle);
         if (vehicleSubLevel != null) {
            this.sable$trackingSubLevel = vehicleSubLevel;
         } else {
            this.sable$trackingSubLevel = Sable.HELPER.getTrackingSubLevel(vehicle);
         }
      }

      if (this.sable$trackingSubLevel != null && this.sable$trackingSubLevel.isRemoved()) {
         this.sable$trackingSubLevel = null;
      }

      if (containingSubLevel != null
         && !this.getBoundingBox().intersects(containingSubLevel.getPlot().getBoundingBox().toAABB().inflate(1.0))
         && this.getType().is(SableTags.DESTROY_WHEN_LEAVING_PLOT)) {
         this.kill();
      }
   }

   @Override
   public BlockPos sable$getInBlockStatePos() {
      return this.sable$inBlockStatePos;
   }

   @Overwrite
   public BlockState getInBlockState() {
      Level level = this.level();
      if (this.inBlockState == null || this.sable$trackingSubLevel != null) {
         this.inBlockState = level.getBlockState(this.blockPosition);
         this.sable$inBlockStatePos = this.blockPosition;
         Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(this.level, new BoundingBox3d(this.blockPosition));
         Iterator<SubLevel> iter = intersecting.iterator();

         while (this.inBlockState.isAir() && iter.hasNext()) {
            SubLevel subLevel = iter.next();
            BlockPos localBlockPos = BlockPos.containing(subLevel.logicalPose().transformPositionInverse(this.position.add(0.0, 0.001, 0.0)));
            this.inBlockState = level.getBlockState(localBlockPos);
            this.sable$inBlockStatePos = localBlockPos;
         }
      }

      return this.inBlockState;
   }

   @Override
   public SubLevel sable$getTrackingSubLevel() {
      return this.sable$trackingSubLevel;
   }

   @Override
   public UUID sable$getLastTrackingSubLevelID() {
      return this.sable$lastTrackingSubLevelId;
   }

   @Override
   public void sable$setTrackingSubLevel(SubLevel subLevel) {
      this.sable$trackingSubLevel = subLevel;
      if (subLevel != null) {
         this.sable$setLastTrackingSubLevelID(subLevel.getUniqueId());
      }
   }

   @Override
   public void sable$setLastTrackingSubLevelID(UUID uuid) {
      this.sable$lastTrackingSubLevelId = uuid;
   }

   @Override
   public SubLevelEntityCollision.CollisionInfo sable$getCollisionInfo() {
      return this.sable$collisionInfo;
   }

   @Override
   public void sable$setPosField(Vec3 newPosition) {
      this.position = newPosition;
   }
}
