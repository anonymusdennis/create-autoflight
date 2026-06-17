package com.simibubi.create.content.trains.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public class CarriageContraptionEntityRenderer extends ContraptionEntityRenderer<CarriageContraptionEntity> {
   public CarriageContraptionEntityRenderer(Context context) {
      super(context);
   }

   public boolean shouldRender(CarriageContraptionEntity entity, Frustum clippingHelper, double cameraX, double cameraY, double cameraZ) {
      Carriage carriage = entity.getCarriage();
      if (carriage != null) {
         for (CarriageBogey bogey : carriage.bogeys) {
            if (bogey != null) {
               bogey.couplingAnchors.replace(v -> null);
            }
         }
      }

      return super.shouldRender(entity, clippingHelper, cameraX, cameraY, cameraZ);
   }

   public void render(CarriageContraptionEntity entity, float yaw, float partialTicks, PoseStack ms, MultiBufferSource buffers, int overlay) {
      if (entity.validForRender && !entity.firstPositionUpdate) {
         super.render(entity, yaw, partialTicks, ms, buffers, overlay);
         Carriage carriage = entity.getCarriage();
         if (carriage != null) {
            Vec3 position = entity.getPosition(partialTicks);
            float viewYRot = entity.getViewYRot(partialTicks);
            float viewXRot = entity.getViewXRot(partialTicks);
            int bogeySpacing = carriage.bogeySpacing;
            carriage.bogeys
               .forEach(
                  bogey -> {
                     if (bogey != null) {
                        BlockPos bogeyPos = bogey.isLeading
                           ? BlockPos.ZERO
                           : BlockPos.ZERO.relative(entity.getInitialOrientation().getCounterClockWise(), bogeySpacing);
                        if (!VisualizationManager.supportsVisualization(entity.level()) && !entity.getContraption().isHiddenInPortal(bogeyPos)) {
                           ms.pushPose();
                           translateBogey(ms, bogey, bogeySpacing, viewYRot, viewXRot, partialTicks);
                           int light = getBogeyLightCoords(entity, bogey, partialTicks);
                           bogey.getStyle()
                              .render(
                                 bogey.getSize(), partialTicks, ms, buffers, light, overlay, bogey.wheelAngle.getValue(partialTicks), bogey.bogeyData, true
                              );
                           ms.popPose();
                        }

                        bogey.updateCouplingAnchor(position, viewXRot, viewYRot, bogeySpacing, partialTicks, bogey.isLeading);
                        if (!carriage.isOnTwoBogeys()) {
                           bogey.updateCouplingAnchor(position, viewXRot, viewYRot, bogeySpacing, partialTicks, !bogey.isLeading);
                        }
                     }
                  }
               );
         }
      }
   }

   public static void translateBogey(PoseStack ms, CarriageBogey bogey, int bogeySpacing, float viewYRot, float viewXRot, float partialTicks) {
      boolean selfUpsideDown = bogey.isUpsideDown();
      boolean leadingUpsideDown = bogey.carriage.leadingBogey().isUpsideDown();
      ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(
                                       ms
                                    )
                                    .rotateYDegrees(viewYRot + 90.0F))
                                 .rotateXDegrees(-viewXRot))
                              .rotateYDegrees(180.0F))
                           .translate(0.0F, 0.0F, bogey.isLeading ? 0.0F : (float)(-bogeySpacing))
                           .rotateYDegrees(-180.0F))
                        .rotateXDegrees(viewXRot))
                     .rotateYDegrees(-viewYRot - 90.0F))
                  .rotateYDegrees(bogey.yaw.getValue(partialTicks)))
               .rotateXDegrees(bogey.pitch.getValue(partialTicks)))
            .translate(0.0F, 0.5F, 0.0F)
            .rotateZDegrees(selfUpsideDown ? 180.0F : 0.0F))
         .translateY(selfUpsideDown != leadingUpsideDown ? 2.0F : 0.0F);
   }

   public static int getBogeyLightCoords(CarriageContraptionEntity entity, CarriageBogey bogey, float partialTicks) {
      Vec3 anchorPosition = bogey.getAnchorPosition();
      BlockPos lightPos = BlockPos.containing(anchorPosition == null ? entity.getLightProbePosition(partialTicks) : anchorPosition);
      return LightTexture.pack(entity.level().getBrightness(LightLayer.BLOCK, lightPos), entity.level().getBrightness(LightLayer.SKY, lightPos));
   }
}
