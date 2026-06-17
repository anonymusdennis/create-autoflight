package dev.ryanhcode.sable.mixin.entity.entity_sublevel_collision;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinterface.EntityExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import java.util.Map.Entry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LivingEntity.Fallsounds;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public abstract class LivingEntityMixin extends Entity implements LivingEntityMovementExtension {
   @Unique
   private final Vector3d sable$inheritedVelocity = new Vector3d();
   @Unique
   private final Vector3d sable$tempPlayerVelocity = new Vector3d();
   @Unique
   private final Vector3d sable$tempSubLevelVelocity = new Vector3d();

   @Shadow
   public abstract Fallsounds getFallSounds();

   public LivingEntityMixin(EntityType<?> entityType, Level level) {
      super(entityType, level);
   }

   @Inject(
      method = {"travel"},
      at = {@At("RETURN")}
   )
   public void sable$beforeAnimation(Vec3 vec3, CallbackInfo ci) {
      SubLevelEntityCollision.CollisionInfo info = ((EntityMovementExtension)this).sable$getCollisionInfo();
      if (info != null && info.inheritedMotion != null && !this.isSpectator()) {
         this.setPos(this.position().add(((EntityExtension)this).sable$vanillaCollide(info.inheritedMotion)));
         this.sable$inheritedVelocity.set(info.inheritedMotion.x, info.inheritedMotion.y, info.inheritedMotion.z);
      }

      if (info != null && info.firstCollisions != null && !info.firstCollisions.isEmpty()) {
         for (Entry<SubLevel, SubLevelEntityCollision.FirstCollisionInfo> firstCollision : info.firstCollisions.entrySet()) {
            SubLevelEntityCollision.FirstCollisionInfo collisionInfo = firstCollision.getValue();
            SubLevel subLevel = firstCollision.getKey();
            if (collisionInfo.horizontal() && subLevel != info.preTrackingSubLevel) {
               this.sable$computeCollisionEffects(info, subLevel, collisionInfo);
            }
         }
      }

      double threshold = 1.0E-7;
      if (this.sable$inheritedVelocity.lengthSquared() <= 1.0E-7) {
         this.sable$inheritedVelocity.zero();
      }

      if ((info == null || info.inheritedMotion == null) && this.sable$inheritedVelocity.lengthSquared() > 1.0E-7) {
         this.sable$applyDrag();
      }
   }

   @Unique
   private void sable$applyDrag() {
      if (this.verticalCollision || this.onGround()) {
         double drag = 0.7;
         this.sable$inheritedVelocity.mul(0.7, 0.0, 0.7);
      }

      if (this.horizontalCollision) {
         double drag = 0.8;
         this.sable$inheritedVelocity.mul(0.8, 0.6, 0.8);
      }

      if (this instanceof Player player && player.getAbilities().flying) {
         this.sable$inheritedVelocity.mul(0.9);
      }

      if (this.wasTouchingWater) {
         this.sable$inheritedVelocity.mul(0.9);
      }

      this.sable$inheritedVelocity.mul(0.99);
      if (Math.abs(this.sable$inheritedVelocity.y) < 0.01) {
         this.sable$inheritedVelocity.y = 0.0;
      }
   }

   @Unique
   private void sable$computeCollisionEffects(
      SubLevelEntityCollision.CollisionInfo info, SubLevel collidedSubLevel, SubLevelEntityCollision.FirstCollisionInfo collisionInfo
   ) {
      Vector3d playerVelocity = JOMLConversion.toJOML(info.preDeltaMovement, this.sable$tempPlayerVelocity);
      playerVelocity.add(this.sable$inheritedVelocity);
      Level level = this.level();
      Vector3d pointVelocity = Sable.HELPER.getVelocity(level, collidedSubLevel, collisionInfo.localLocation(), this.sable$tempSubLevelVelocity).mul(0.05);
      Vector3d relativeVelocity = playerVelocity.sub(pointVelocity).negate();
      double magnitude = collisionInfo.globalDirection().dot(relativeVelocity);
      if (magnitude > 0.15) {
         relativeVelocity.set(collisionInfo.globalDirection()).mul(-magnitude);
         if (collisionInfo.bouncy()) {
            SoundEvent sound = collisionInfo.block().getSoundType().getFallSound();
            level.playSound(this instanceof Player player ? player : null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.BLOCKS, 0.75F, 1.0F);
            this.addDeltaMovement(JOMLConversion.toMojang(collisionInfo.globalDirection()).scale(relativeVelocity.length() * 0.65));
            if (Sable.HELPER.getTrackingSubLevel(this) == null) {
               this.addDeltaMovement(JOMLConversion.toMojang(pointVelocity));
            }
         } else {
            float damageAmount = (float)(magnitude * 12.0 - 8.0);
            if ((double)damageAmount > 0.0) {
               this.playSound(damageAmount > 4.0F ? this.getFallSounds().big() : this.getFallSounds().small(), 1.0F, 1.0F);
               this.hurt(this.damageSources().flyIntoWall(), damageAmount);
            }
         }
      }
   }

   @Override
   public Vector3d sable$getInheritedVelocity() {
      return this.sable$inheritedVelocity;
   }
}
