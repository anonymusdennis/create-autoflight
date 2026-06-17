package dev.ryanhcode.sable.mixin.interaction_distance;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public abstract class PlayerMixin extends LivingEntity {
   protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
      super(entityType, level);
   }

   @Shadow
   public abstract double blockInteractionRange();

   @Inject(
      method = {"canInteractWithBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$canInteractWithBlock(BlockPos pos, double slop, CallbackInfoReturnable<Boolean> cir) {
      SubLevel subLevel = Sable.HELPER.getContaining(this.level(), pos);
      if (subLevel != null) {
         double rangeWithSlop = this.blockInteractionRange() + slop;
         Vec3 eyePos = subLevel.logicalPose().transformPositionInverse(this.getEyePosition());
         boolean closeEnough = new AABB(pos).distanceToSqr(eyePos) < rangeWithSlop * rangeWithSlop;
         if (closeEnough) {
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(
      method = {"canInteractWithEntity(Lnet/minecraft/world/phys/AABB;D)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$canInteractWithEntity(AABB aabb, double slop, CallbackInfoReturnable<Boolean> cir) {
      SubLevel subLevel = Sable.HELPER.getContaining(this.level(), aabb.getBottomCenter());
      if (subLevel != null) {
         double rangeWithSlop = this.blockInteractionRange() + slop;
         Vec3 eyePos = subLevel.logicalPose().transformPositionInverse(this.getEyePosition());
         boolean closeEnough = aabb.distanceToSqr(eyePos) < rangeWithSlop * rangeWithSlop;
         if (closeEnough) {
            cir.setReturnValue(true);
         }
      }
   }
}
