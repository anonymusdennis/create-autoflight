package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.raycast;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.simibubi.create.foundation.utility.RaycastHelper.PredicateTraceResult;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.raycasts.SableRaycastHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({RaycastHelper.class})
public class RaycastHelperMixin {
   @Redirect(
      method = {"getTraceTarget"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"
      )
   )
   private static Vec3 sable$rotateWithSublevels(Vec3 instance, double pX, double pY, double pZ, @Local(argsOnly = true) Player player) {
      Vec3 resultTarget = new Vec3(pX, pY, pZ);
      Entity vehicle = player.getVehicle();
      if (vehicle != null) {
         SubLevel vehicleSubLevel = Sable.HELPER.getContaining(player.level(), vehicle.position());
         if (vehicleSubLevel != null) {
            Vector3d vec = JOMLConversion.toJOML(resultTarget);
            vehicleSubLevel.logicalPose().orientation().transform(vec);
            resultTarget = JOMLConversion.toMojang(vec);
         }
      }

      return instance.add(resultTarget);
   }

   @Inject(
      method = {"rayTraceUntil(Lnet/minecraft/world/entity/player/Player;DLjava/util/function/Predicate;)Lcom/simibubi/create/foundation/utility/RaycastHelper$PredicateTraceResult;"},
      at = {@At("HEAD")},
      remap = false,
      cancellable = true
   )
   private static void sable$rayTraceSublevels(Player playerIn, double range, Predicate<BlockPos> predicate, CallbackInfoReturnable<PredicateTraceResult> cir) {
      Vec3 start = playerIn.getEyePosition();
      Vec3 end = RaycastHelper.getTraceTarget(playerIn, range, start);
      cir.setReturnValue(SableRaycastHelper.rayCastUntilWithSublevels(playerIn.level(), start, end, predicate));
   }
}
