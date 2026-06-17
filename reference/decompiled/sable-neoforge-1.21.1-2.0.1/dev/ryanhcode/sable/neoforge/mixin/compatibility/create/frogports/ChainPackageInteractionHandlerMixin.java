package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.chainConveyor.ChainPackageInteractionHandler;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage.ChainConveyorPackagePhysicsData;
import com.simibubi.create.foundation.utility.RaycastHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ChainPackageInteractionHandler.class})
public class ChainPackageInteractionHandlerMixin {
   @Redirect(
      method = {"lambda$onUse$0"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getEyePosition()Lnet/minecraft/world/phys/Vec3;"
      )
   )
   private static Vec3 sable$getTraceOrigin(LocalPlayer instance, @Local(argsOnly = true) ChainConveyorPackagePhysicsData data) {
      Vec3 origin = instance.getEyePosition();
      SubLevel subLevel = Sable.HELPER.getContainingClient(data.targetPos);
      if (subLevel != null) {
         origin = subLevel.logicalPose().transformPositionInverse(origin);
      }

      return origin;
   }

   @Redirect(
      method = {"lambda$onUse$0"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/foundation/utility/RaycastHelper;getTraceTarget(Lnet/minecraft/world/entity/player/Player;DLnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"
      )
   )
   private static Vec3 sable$getTraceTarget(Player playerIn, double range, Vec3 from, @Local(argsOnly = true) ChainConveyorPackagePhysicsData data) {
      Vec3 target = RaycastHelper.getTraceTarget(playerIn, range, playerIn.getEyePosition());
      SubLevel subLevel = Sable.HELPER.getContainingClient(data.targetPos);
      if (subLevel != null) {
         target = subLevel.logicalPose().transformPositionInverse(target);
      }

      return target;
   }
}
