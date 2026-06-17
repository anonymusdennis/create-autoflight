package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ChainConveyorRidingHandler.class})
public class ChainConveyorRidingHandlerMixin {
   @Redirect(
      method = {"clientTick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
         ordinal = 1
      )
   )
   private static Vec3 sable$fixDiff(Vec3 targetPosition, Vec3 playerPosition) {
      return JOMLConversion.toMojang(
         Sable.HELPER
            .projectOutOfSubLevel(Minecraft.getInstance().level, JOMLConversion.toJOML(targetPosition))
            .sub(playerPosition.x, playerPosition.y, playerPosition.z)
      );
   }

   @Redirect(
      method = {"updateTargetPosition"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getLookAngle()Lnet/minecraft/world/phys/Vec3;"
      )
   )
   private static Vec3 sable$fixLookAngle(LocalPlayer instance, @Local(ordinal = 1) BlockPos connection, @Local ChainConveyorBlockEntity clbe) {
      SubLevel subLevel = Sable.HELPER.getContaining(clbe);
      if (subLevel != null) {
         Pose3dc pose = subLevel.logicalPose();
         Vec3 lookAngle = instance.getLookAngle();
         return pose.transformNormalInverse(lookAngle);
      } else {
         return instance.getLookAngle();
      }
   }
}
