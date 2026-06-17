package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.tracks;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.Translate;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({TrackTargetingClient.class})
public class TrackTargetingClientMixin {
   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Ldev/engine_room/flywheel/lib/transform/PoseTransformStack;translate(Lnet/minecraft/world/phys/Vec3;)Ldev/engine_room/flywheel/lib/transform/Translate;"
      )
   )
   private static Translate sable$manipulateMatrixStack(
      PoseTransformStack instance, Vec3 vec3, @Local(ordinal = 0) Minecraft minecraft, @Local(ordinal = 0) BlockPos pos, @Local(argsOnly = true) Vec3 camera
   ) {
      ClientLevel level = minecraft.level;
      if (Sable.HELPER.getContaining(level, pos) instanceof ClientSubLevel clientSubLevel) {
         Pose3dc renderPose = clientSubLevel.renderPose();
         Vec3 renderPos = renderPose.transformPosition(Vec3.atLowerCornerOf(pos));
         Quaternionf renderOrientation = new Quaternionf(renderPose.orientation());
         return ((PoseTransformStack)instance.translate(renderPos.x() - camera.x(), renderPos.y() - camera.y(), renderPos.z() - camera.z()))
            .rotate(renderOrientation);
      } else {
         return instance.translate(vec3);
      }
   }
}
