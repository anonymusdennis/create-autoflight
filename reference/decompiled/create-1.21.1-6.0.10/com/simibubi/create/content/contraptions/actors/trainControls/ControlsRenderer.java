package com.simibubi.create.content.contraptions.actors.trainControls;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class ControlsRenderer {
   public static void render(
      MovementContext context,
      VirtualRenderWorld renderWorld,
      ContraptionMatrices matrices,
      MultiBufferSource buffer,
      float equipAnimation,
      float firstLever,
      float secondLever
   ) {
      BlockState state = context.state;
      Direction facing = (Direction)state.getValue(ControlsBlock.FACING);
      SuperByteBuffer cover = CachedBuffers.partial(AllPartialModels.TRAIN_CONTROLS_COVER, state);
      float hAngle = 180.0F + AngleHelper.horizontalAngle(facing);
      PoseStack ms = matrices.getModel();
      ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)cover.transform(ms)).center()).rotateYDegrees(hAngle)).uncenter())
         .light(LevelRenderer.getLightColor(renderWorld, context.localPos))
         .useLevelLight(context.world, matrices.getWorld())
         .renderInto(matrices.getViewProjection(), buffer.getBuffer(RenderType.cutoutMipped()));
      double yOffset = (double)Mth.lerp(equipAnimation * equipAnimation, -0.15F, 0.05F);

      for (boolean first : Iterate.trueAndFalse) {
         float vAngle = Mth.clamp(first ? firstLever * 70.0F - 25.0F : secondLever * 15.0F, -45.0F, 45.0F);
         SuperByteBuffer lever = CachedBuffers.partial(AllPartialModels.TRAIN_CONTROLS_LEVER, state);
         ms.pushPose();
         ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(ms)
                              .center())
                           .rotateYDegrees(hAngle))
                        .translate(0.0F, 0.25F, 0.25F)
                        .rotateXDegrees(vAngle - 45.0F))
                     .translate(0.0, yOffset, 0.0))
                  .rotateXDegrees(45.0F))
               .uncenter())
            .translate(0.0F, -0.375F, -0.1875F)
            .translate(first ? 0.0F : 0.375F, 0.0F, 0.0F);
         ((SuperByteBuffer)lever.transform(ms))
            .light(LevelRenderer.getLightColor(renderWorld, context.localPos))
            .useLevelLight(context.world, matrices.getWorld())
            .renderInto(matrices.getViewProjection(), buffer.getBuffer(RenderType.solid()));
         ms.popPose();
      }
   }
}
