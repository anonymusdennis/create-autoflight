package com.simibubi.create.content.logistics.packagePort.frogport;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class FrogportRenderer extends SmartBlockEntityRenderer<FrogportBlockEntity> {
   public FrogportRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(FrogportBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      SuperByteBuffer body = CachedBuffers.partial(AllPartialModels.FROGPORT_BODY, blockEntity.getBlockState());
      float yaw = blockEntity.getYaw();
      float headPitch = 80.0F;
      float tonguePitch = 0.0F;
      float tongueLength = 0.0F;
      float headPitchModifier = 1.0F;
      boolean hasTarget = blockEntity.target != null;
      boolean animating = blockEntity.isAnimationInProgress();
      boolean depositing = blockEntity.currentlyDepositing;
      Vec3 diff = Vec3.ZERO;
      if (blockEntity.addressFilter != null && !blockEntity.addressFilter.isBlank()) {
         this.renderNameplateOnHover(blockEntity, Component.literal(blockEntity.addressFilter), 1.0F, ms, buffer, light);
      }

      if (!VisualizationManager.supportsVisualization(blockEntity.getLevel())) {
         if (hasTarget) {
            diff = blockEntity.target
               .getExactTargetLocation(blockEntity, blockEntity.getLevel(), blockEntity.getBlockPos())
               .subtract(0.0, animating && depositing ? 0.0 : 0.75, 0.0)
               .subtract(Vec3.atCenterOf(blockEntity.getBlockPos()));
            tonguePitch = (float)Mth.atan2(diff.y, diff.multiply(1.0, 0.0, 1.0).length() + 0.1875) * (180.0F / (float)Math.PI);
            tongueLength = Math.max((float)diff.length(), 1.0F);
            headPitch = Mth.clamp(tonguePitch * 2.0F, 60.0F, 100.0F);
         }

         if (animating) {
            float progress = blockEntity.animationProgress.getValue(partialTicks);
            float scale = 1.0F;
            float itemDistance = 0.0F;
            if (depositing) {
               double modifier = Math.max(0.0, 1.0 - Math.pow(((double)progress - 0.25) * 4.0 - 1.0, 4.0));
               itemDistance = (float)Math.max((double)tongueLength * Math.min(1.0, ((double)progress - 0.25) * 3.0), (double)tongueLength * modifier);
               tongueLength = (float)((double)tongueLength * Math.max(0.0, 1.0 - Math.pow(((double)progress * 1.25 - 0.25) * 4.0 - 1.0, 4.0)));
               headPitchModifier = (float)Math.max(0.0, 1.0 - Math.pow((double)progress * 1.25 * 2.0 - 1.0, 4.0));
               scale = 0.25F + progress * 3.0F / 4.0F;
            } else {
               tongueLength = (float)((double)tongueLength * Math.pow(Math.max(0.0, 1.0 - (double)progress * 1.25), 5.0));
               headPitchModifier = 1.0F - (float)Math.min(1.0, Math.max(0.0, (Math.pow((double)progress * 1.5, 2.0) - 0.5) * 2.0));
               scale = (float)Math.max(0.5, 1.0 - (double)progress * 1.25);
               itemDistance = tongueLength;
            }

            this.renderPackage(blockEntity, ms, buffer, light, overlay, diff, scale, itemDistance);
         } else {
            tongueLength = 0.0F;
            float anticipation = blockEntity.anticipationProgress.getValue(partialTicks);
            headPitchModifier = anticipation > 0.0F ? (float)Math.max(0.0, 1.0 - Math.pow((double)anticipation * 1.25 * 2.0 - 1.0, 4.0)) : 0.0F;
         }

         headPitch *= headPitchModifier;
         headPitch = Math.max(headPitch, blockEntity.manualOpenAnimationProgress.getValue(partialTicks) * 60.0F);
         tongueLength = Math.max(tongueLength, blockEntity.manualOpenAnimationProgress.getValue(partialTicks) * 0.25F);
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)body.center()).rotateYDegrees(yaw)).uncenter())
            .light(light)
            .overlay(overlay)
            .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
         SuperByteBuffer head = CachedBuffers.partial(
            blockEntity.goggles ? AllPartialModels.FROGPORT_HEAD_GOGGLES : AllPartialModels.FROGPORT_HEAD, blockEntity.getBlockState()
         );
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)head.center()).rotateYDegrees(yaw)).uncenter())
                  .translate(0.5F, 0.625F, 0.6875F))
               .rotateXDegrees(headPitch))
            .translateBack(0.5F, 0.625F, 0.6875F);
         head.light(light).overlay(overlay).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
         SuperByteBuffer tongue = CachedBuffers.partial(AllPartialModels.FROGPORT_TONGUE, blockEntity.getBlockState());
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)tongue.center()).rotateYDegrees(yaw))
                        .uncenter())
                     .translate(0.5F, 0.625F, 0.6875F))
                  .rotateXDegrees(tonguePitch))
               .scale(1.0F, 1.0F, tongueLength / 0.4375F))
            .translateBack(0.5F, 0.625F, 0.6875F);
         tongue.light(light).overlay(overlay).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
      }
   }

   private void renderPackage(
      FrogportBlockEntity blockEntity, PoseStack ms, MultiBufferSource buffer, int light, int overlay, Vec3 diff, float scale, float itemDistance
   ) {
      if (blockEntity.animatedPackage != null) {
         if (!((double)scale < 0.45)) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(blockEntity.animatedPackage.getItem());
            if (key != BuiltInRegistries.ITEM.getDefaultKey()) {
               SuperByteBuffer rigBuffer = CachedBuffers.partial(AllPartialModels.PACKAGE_RIGGING.get(key), blockEntity.getBlockState());
               SuperByteBuffer boxBuffer = CachedBuffers.partial(AllPartialModels.PACKAGES.get(key), blockEntity.getBlockState());
               boolean animating = blockEntity.isAnimationInProgress();
               boolean depositing = blockEntity.currentlyDepositing;

               for (SuperByteBuffer buf : new SuperByteBuffer[]{boxBuffer, rigBuffer}) {
                  ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)buf.translate(0.0F, 0.1875F, 0.0F))
                                 .translate(diff.normalize().scale((double)itemDistance).subtract(0.0, animating && depositing ? 0.75 : 0.0, 0.0)))
                              .center())
                           .scale(scale))
                        .uncenter())
                     .light(light)
                     .overlay(overlay)
                     .renderInto(ms, buffer.getBuffer(RenderType.cutout()));
                  if (!blockEntity.currentlyDepositing) {
                     break;
                  }
               }
            }
         }
      }
   }
}
