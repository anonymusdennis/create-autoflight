package com.simibubi.create.content.kinetics.simpleRelays;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;

public class BracketedKineticBlockEntityRenderer extends KineticBlockEntityRenderer<BracketedKineticBlockEntity> {
   public BracketedKineticBlockEntityRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(BracketedKineticBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         if (!AllBlocks.LARGE_COGWHEEL.has(be.getBlockState())) {
            super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
         } else {
            VertexConsumer vc = buffer.getBuffer(RenderType.solid());
            Axis axis = getRotationAxisOf(be);
            Direction facing = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
            renderRotatingBuffer(be, CachedBuffers.partialFacingVertical(AllPartialModels.SHAFTLESS_LARGE_COGWHEEL, be.getBlockState(), facing), ms, vc, light);
            float angle = getAngleForLargeCogShaft(be, axis);
            SuperByteBuffer shaft = CachedBuffers.partialFacingVertical(AllPartialModels.COGWHEEL_SHAFT, be.getBlockState(), facing);
            kineticRotationTransform(shaft, be, axis, angle, light);
            shaft.renderInto(ms, vc);
         }
      }
   }

   public static float getAngleForLargeCogShaft(SimpleKineticBlockEntity be, Axis axis) {
      BlockPos pos = be.getBlockPos();
      float offset = getShaftAngleOffset(axis, pos);
      float time = AnimationTickHolder.getRenderTime(be.getLevel());
      return (time * be.getSpeed() * 3.0F / 10.0F + offset) % 360.0F / 180.0F * (float) Math.PI;
   }

   public static float getShaftAngleOffset(Axis axis, BlockPos pos) {
      return KineticBlockEntityVisual.shouldOffset(axis, pos) ? 22.5F : 0.0F;
   }
}
