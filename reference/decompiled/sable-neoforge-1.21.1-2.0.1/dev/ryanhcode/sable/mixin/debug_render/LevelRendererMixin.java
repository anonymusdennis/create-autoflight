package dev.ryanhcode.sable.mixin.debug_render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public class LevelRendererMixin {
   @Shadow
   private ClientLevel level;

   @Inject(
      method = {"renderLevel"},
      at = {@At("TAIL")}
   )
   private void renderLevel(
      DeltaTracker deltaTracker,
      boolean bl,
      Camera camera,
      GameRenderer gameRenderer,
      LightTexture lightTexture,
      Matrix4f matrix4f,
      Matrix4f matrix4f2,
      CallbackInfo ci
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes() && !Minecraft.getInstance().showOnlyReducedInfo()) {
         SubLevelContainer container = SubLevelContainer.getContainer(this.level);
         BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
         VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);
         double cx = camera.getPosition().x;
         double cy = camera.getPosition().y;
         double cz = camera.getPosition().z;
         PoseStack ps = new PoseStack();
         ps.mulPose(matrix4f);

         for (SubLevel subLevel : container.getAllSubLevels()) {
            BoundingBox3dc bounds = subLevel.boundingBox();
            LevelRenderer.renderLineBox(
               ps,
               consumer,
               bounds.minX() - cx,
               bounds.minY() - cy,
               bounds.minZ() - cz,
               bounds.maxX() - cx,
               bounds.maxY() - cy,
               bounds.maxZ() - cz,
               0.5F,
               0.5F,
               0.5F,
               0.7F
            );
            ps.pushPose();
            Pose3dc renderPose = ((ClientSubLevel)subLevel).renderPose();
            BoundingBox3ic plotBounds = subLevel.getPlot().getBoundingBox();
            Vector3dc globalCenter = renderPose.position();
            Vector3dc localCenter = renderPose.rotationPoint();
            ps.translate(globalCenter.x() - cx, globalCenter.y() - cy, globalCenter.z() - cz);
            ps.mulPose(new Quaternionf(renderPose.orientation()));
            LevelRenderer.renderLineBox(ps, consumer, -0.125, -0.125, -0.125, 0.125, 0.125, 0.125, 0.7F, 0.7F, 0.5F, 1.0F);
            LevelRenderer.renderLineBox(
               ps,
               consumer,
               (double)plotBounds.minX() - localCenter.x(),
               (double)plotBounds.minY() - localCenter.y(),
               (double)plotBounds.minZ() - localCenter.z(),
               (double)plotBounds.maxX() + 1.0 - localCenter.x(),
               (double)plotBounds.maxY() + 1.0 - localCenter.y(),
               (double)plotBounds.maxZ() + 1.0 - localCenter.z(),
               0.9F,
               0.5F,
               0.5F,
               1.0F
            );
            ps.popPose();
         }

         bufferSource.endLastBatch();
      }
   }
}
