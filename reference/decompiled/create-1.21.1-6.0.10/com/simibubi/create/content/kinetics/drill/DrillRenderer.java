package com.simibubi.create.content.kinetics.drill;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class DrillRenderer extends KineticBlockEntityRenderer<DrillBlockEntity> {
   public DrillRenderer(Context context) {
      super(context);
   }

   protected SuperByteBuffer getRotatedModel(DrillBlockEntity be, BlockState state) {
      return CachedBuffers.partialFacing(AllPartialModels.DRILL_HEAD, state);
   }

   public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
      BlockState state = context.state;
      SuperByteBuffer superBuffer = CachedBuffers.partial(AllPartialModels.DRILL_HEAD, state);
      Direction facing = (Direction)state.getValue(DrillBlock.FACING);
      float speed = !context.contraption.stalled && VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite())
         ? 0.0F
         : context.getAnimationSpeed();
      float time = AnimationTickHolder.getRenderTime() / 20.0F;
      float angle = time * speed % 360.0F;
      ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)superBuffer.transform(matrices.getModel()))
                        .center())
                     .rotateYDegrees(AngleHelper.horizontalAngle(facing)))
                  .rotateXDegrees(AngleHelper.verticalAngle(facing)))
               .rotateZDegrees(angle))
            .uncenter())
         .light(LevelRenderer.getLightColor(renderWorld, context.localPos))
         .useLevelLight(context.world, matrices.getWorld())
         .renderInto(matrices.getViewProjection(), buffer.getBuffer(RenderType.solid()));
   }
}
