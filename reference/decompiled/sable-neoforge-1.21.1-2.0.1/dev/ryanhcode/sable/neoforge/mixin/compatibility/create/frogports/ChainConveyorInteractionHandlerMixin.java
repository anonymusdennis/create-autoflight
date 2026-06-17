package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorInteractionHandler;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorShape;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ChainConveyorInteractionHandler.class})
public class ChainConveyorInteractionHandlerMixin {
   @Shadow
   public static BlockPos selectedLift;
   @Shadow
   public static ChainConveyorShape selectedShape;

   @Redirect(
      method = {"clientTick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
      )
   )
   private static double sable$addParticleInternal(Vec3 instance, Vec3 vec3) {
      return Sable.HELPER.distanceSquaredWithSubLevels(Minecraft.getInstance().level, instance, vec3);
   }

   @Redirect(
      method = {"clientTick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
         ordinal = 0
      )
   )
   private static Vec3 sable$fromSubLiftVec(Vec3 from, Vec3 liftVec, @Local(ordinal = 0) ChainConveyorShape shape) {
      SubLevel subLevel = Sable.HELPER.getContainingClient(liftVec);
      return subLevel != null ? subLevel.logicalPose().transformPositionInverse(from).subtract(liftVec) : from.subtract(liftVec);
   }

   @Redirect(
      method = {"clientTick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
         ordinal = 1
      )
   )
   private static Vec3 sable$toSubLiftVec(Vec3 to, Vec3 liftVec, @Local(ordinal = 0) ChainConveyorShape shape) {
      SubLevel subLevel = Sable.HELPER.getContainingClient(liftVec);
      return subLevel != null ? subLevel.logicalPose().transformPositionInverse(to).subtract(liftVec) : to.subtract(liftVec);
   }

   @Overwrite
   public static void drawCustomBlockSelection(PoseStack ms, MultiBufferSource buffer, Vec3 camera) {
      if (selectedLift != null && selectedShape != null) {
         VertexConsumer vb = buffer.getBuffer(RenderType.lines());
         ms.pushPose();
         Vec3 pos = Vec3.atLowerCornerOf(selectedLift);
         SubLevel subLevel = Sable.HELPER.getContainingClient(pos);
         if (subLevel instanceof ClientSubLevel clientSubLevel) {
            Pose3dc renderPose = clientSubLevel.renderPose();
            pos = renderPose.transformPosition(pos);
            ms.translate(pos.x() - camera.x, pos.y() - camera.y, pos.z() - camera.z);
            ms.mulPose(new Quaternionf(renderPose.orientation()));
         } else {
            ms.translate(pos.x() - camera.x, pos.y() - camera.y, pos.z() - camera.z);
         }

         ((ChainConveyorShapeAccessor)selectedShape).invokeDrawOutline(selectedLift, ms, vb);
         ms.popPose();
      }
   }
}
