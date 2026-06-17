package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.packet_mixin.PacketActuallyInSubLevelExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerEntity.class})
public class ServerEntityMixin {
   @Shadow
   @Final
   private Entity entity;
   @Shadow
   @Final
   private ServerLevel level;
   @Unique
   private Vec3 sable$oldPos = null;

   @Inject(
      method = {"sendChanges"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;trackingPosition()Lnet/minecraft/world/phys/Vec3;",
         shift = Shift.BEFORE
      )}
   )
   private void sable$pre(CallbackInfo ci, @Share("actuallyInSubLevel") LocalBooleanRef actuallyInSubLevel) {
      this.sable$oldPos = null;
      SubLevel containingSubLevel = Sable.HELPER.getContaining(this.entity);
      if (containingSubLevel != null) {
         actuallyInSubLevel.set(true);
      }

      SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(this.entity);
      Vec3 pos = this.entity.position();
      if (trackingSubLevel != null) {
         this.sable$oldPos = new Vec3(pos.x, pos.y, pos.z);
         pos = trackingSubLevel.lastPose().transformPositionInverse(pos);
         ((EntityMovementExtension)this.entity).sable$setPosField(pos);
      }
   }

   @Inject(
      method = {"sendChanges"},
      at = {@At("RETURN")}
   )
   private void sable$postTransform(CallbackInfo ci) {
      if (this.sable$oldPos != null) {
         ((EntityMovementExtension)this.entity).sable$setPosField(this.sable$oldPos);
         this.sable$oldPos = null;
      }
   }

   @WrapOperation(
      method = {"sendChanges"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"
      )}
   )
   private void sable$sendChanges(Consumer<?> instance, Object t, Operation<Void> original, @Share("actuallyInSubLevel") LocalBooleanRef actuallyInSubLevel) {
      if (actuallyInSubLevel.get() && t instanceof PacketActuallyInSubLevelExtension extension) {
         extension.sable$setActuallyInSubLevel(true);
      }

      original.call(new Object[]{instance, t});
   }
}
