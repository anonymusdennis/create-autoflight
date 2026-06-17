package com.simibubi.create.content.trains.station;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.Transform;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class StationRenderer extends SafeBlockEntityRenderer<StationBlockEntity> {
   public StationRenderer(Context context) {
   }

   protected void renderSafe(StationBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockPos pos = be.getBlockPos();
      TrackTargetingBehaviour<GlobalStation> target = be.edgePoint;
      BlockPos targetPosition = target.getGlobalPosition();
      Level level = be.getLevel();
      DepotRenderer.renderItemsOf(be, partialTicks, ms, buffer, light, overlay, be.depotBehaviour);
      BlockState trackState = level.getBlockState(targetPosition);
      if (trackState.getBlock() instanceof ITrackBlock track) {
         GlobalStation station = be.getStation();
         boolean isAssembling = (Boolean)be.getBlockState().getValue(StationBlock.ASSEMBLING);
         if (isAssembling && (station != null && station.getPresentTrain() == null || be.isVirtual())) {
            renderFlag(AllPartialModels.STATION_ASSEMBLE, be, partialTicks, ms, buffer, light, overlay);
            Direction direction = be.assemblyDirection;
            if (be.isVirtual() && be.bogeyLocations == null) {
               be.refreshAssemblyInfo();
            }

            if (direction != null && be.assemblyLength != 0 && be.bogeyLocations != null) {
               ms.pushPose();
               BlockPos offset = targetPosition.subtract(pos);
               ms.translate((float)offset.getX(), (float)offset.getY(), (float)offset.getZ());
               MutableBlockPos currentPos = targetPosition.mutable();
               PartialModel assemblyOverlay = track.prepareAssemblyOverlay(level, targetPosition, trackState, direction, ms);
               int colorWhenValid = 9876991;
               int colorWhenCarriage = 13303702;
               VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
               currentPos.move(direction, 1);
               ms.translate(0.0F, 0.0F, 1.0F);

               for (int i = 0; i < be.assemblyLength; i++) {
                  int valid = be.isValidBogeyOffset(i) ? colorWhenValid : -1;

                  for (int j : be.bogeyLocations) {
                     if (i == j) {
                        valid = colorWhenCarriage;
                        break;
                     }
                  }

                  if (valid != -1) {
                     int lightColor = LevelRenderer.getLightColor(level, currentPos);
                     SuperByteBuffer sbb = CachedBuffers.partial(assemblyOverlay, trackState);
                     sbb.color(valid);
                     sbb.light(lightColor);
                     sbb.renderInto(ms, vb);
                  }

                  ms.translate(0.0F, 0.0F, 1.0F);
                  currentPos.move(direction);
               }

               ms.popPose();
            }
         } else {
            renderFlag(
               be.flag.getValue(partialTicks) > 0.75F ? AllPartialModels.STATION_ON : AllPartialModels.STATION_OFF,
               be,
               partialTicks,
               ms,
               buffer,
               light,
               overlay
            );
            ms.pushPose();
            TransformStack.of(ms).translate(targetPosition.subtract(pos));
            TrackTargetingBehaviour.render(
               level,
               targetPosition,
               target.getTargetDirection(),
               target.getTargetBezier(),
               ms,
               buffer,
               light,
               overlay,
               TrackTargetingBehaviour.RenderedTrackOverlayType.STATION,
               1.0F
            );
            ms.popPose();
         }
      }
   }

   public static void renderFlag(PartialModel flag, StationBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (be.resolveFlagAngle()) {
         SuperByteBuffer flagBB = CachedBuffers.partial(flag, be.getBlockState());
         transformFlag(flagBB, be, partialTicks, be.flagYRot, be.flagFlipped);
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)flagBB.translate(0.03125F, 0.0F, 0.0F)).rotateYDegrees(be.flagFlipped ? 0.0F : 180.0F))
               .translate(-0.03125F, 0.0F, 0.0F))
            .light(light)
            .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
      }
   }

   public static void transformFlag(Transform<?> flag, StationBlockEntity be, float partialTicks, int yRot, boolean flipped) {
      float value = be.flag.getValue(partialTicks);
      float progress = (float)Math.pow((double)Math.min(value * 5.0F, 1.0F), 2.0);
      if (be.flag.getChaseTarget() > 0.0F && !be.flag.settled() && progress == 1.0F) {
         float wiggleProgress = (value - 0.2F) / 0.8F;
         progress = (float)(
            (double)progress + Math.sin((double)(wiggleProgress * (float) (Math.PI * 2) * 4.0F)) / 8.0 / (double)Math.max(1.0F, 8.0F * wiggleProgress)
         );
      }

      float nudge = 0.001953125F;
      ((Transform)((Transform)((Transform)((Transform)flag.center()).rotateYDegrees((float)yRot))
               .translate(nudge, 0.59375F, flipped ? 0.875F - nudge : 0.125F + nudge))
            .uncenter())
         .rotateXDegrees((float)(flipped ? 1 : -1) * (progress * 90.0F + 270.0F));
   }

   public boolean shouldRenderOffScreen(StationBlockEntity pBlockEntity) {
      return true;
   }

   public int getViewDistance() {
      return 192;
   }
}
