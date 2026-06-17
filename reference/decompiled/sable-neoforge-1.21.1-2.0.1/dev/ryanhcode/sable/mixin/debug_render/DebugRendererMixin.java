package dev.ryanhcode.sable.mixin.debug_render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin({DebugRenderer.class})
public class DebugRendererMixin {
   @Overwrite
   public static void renderFilledBox(PoseStack poseStack, MultiBufferSource bufferSource, BlockPos blockPos, float f, float g, float h, float i, float j) {
      Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
      if (camera.isInitialized()) {
         ClientSubLevel subLevel = Sable.HELPER.getContainingClient(blockPos);
         if (subLevel != null) {
            poseStack.pushPose();
            Pose3dc renderPose = subLevel.renderPose();
            Vec3 pos = renderPose.transformPosition(blockPos.getCenter()).subtract(camera.getPosition());
            poseStack.translate(pos.x, pos.y, pos.z);
            poseStack.mulPose(new Quaternionf(renderPose.orientation()));
            DebugRenderer.renderFilledBox(poseStack, bufferSource, new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0).inflate(0.5).inflate((double)f), g, h, i, j);
            poseStack.popPose();
            return;
         }

         Vec3 relativePos = camera.getPosition().reverse();
         AABB box = new AABB(blockPos).move(relativePos).inflate((double)f);
         DebugRenderer.renderFilledBox(poseStack, bufferSource, box, g, h, i, j);
      }
   }
}
