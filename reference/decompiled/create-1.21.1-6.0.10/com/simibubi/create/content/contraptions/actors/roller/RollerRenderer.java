package com.simibubi.create.content.contraptions.actors.roller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterRenderer;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class RollerRenderer extends SmartBlockEntityRenderer<RollerBlockEntity> {
   public RollerRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(RollerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      BlockState blockState = be.getBlockState();
      VertexConsumer vc = buffer.getBuffer(RenderType.cutoutMipped());
      ms.pushPose();
      ms.translate(0.0, -0.25, 0.0);
      SuperByteBuffer superBuffer = CachedBuffers.partial(AllPartialModels.ROLLER_WHEEL, blockState);
      Direction facing = (Direction)blockState.getValue(RollerBlock.FACING);
      superBuffer.translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(1.0625));
      HarvesterRenderer.transform(be.getLevel(), facing, superBuffer, be.getAnimatedSpeed(), Vec3.ZERO);
      ((SuperByteBuffer)((SuperByteBuffer)superBuffer.translate(0.0, -0.5, 0.5)).rotateYDegrees(90.0F)).light(light).renderInto(ms, vc);
      ms.popPose();
      ((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.ROLLER_FRAME, blockState)
            .rotateCentered(AngleHelper.rad((double)(AngleHelper.horizontalAngle(facing) + 180.0F)), Direction.UP))
         .light(light)
         .renderInto(ms, vc);
   }

   public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffers) {
      BlockState blockState = context.state;
      Direction facing = (Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
      VertexConsumer vc = buffers.getBuffer(RenderType.cutoutMipped());
      SuperByteBuffer superBuffer = CachedBuffers.partial(AllPartialModels.ROLLER_WHEEL, blockState);
      float speed = !VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite()) ? context.getAnimationSpeed() : -context.getAnimationSpeed();
      if (context.contraption.stalled) {
         speed = 0.0F;
      }

      ((SuperByteBuffer)superBuffer.transform(matrices.getModel())).translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(1.0625));
      HarvesterRenderer.transform(context.world, facing, superBuffer, speed, Vec3.ZERO);
      PoseStack viewProjection = matrices.getViewProjection();
      viewProjection.pushPose();
      viewProjection.translate(0.0, -0.25, 0.0);
      int contraptionWorldLight = LevelRenderer.getLightColor(renderWorld, context.localPos);
      ((SuperByteBuffer)((SuperByteBuffer)superBuffer.translate(0.0, -0.5, 0.5)).rotateYDegrees(90.0F))
         .light(contraptionWorldLight)
         .useLevelLight(context.world, matrices.getWorld())
         .renderInto(viewProjection, vc);
      viewProjection.popPose();
      ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.ROLLER_FRAME, blockState).transform(matrices.getModel()))
            .rotateCentered(AngleHelper.rad((double)(AngleHelper.horizontalAngle(facing) + 180.0F)), Direction.UP))
         .light(contraptionWorldLight)
         .useLevelLight(context.world, matrices.getWorld())
         .renderInto(viewProjection, vc);
   }
}
