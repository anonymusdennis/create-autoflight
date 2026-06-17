package com.simibubi.create.content.kinetics.transmission;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;

public class SplitShaftRenderer extends KineticBlockEntityRenderer<SplitShaftBlockEntity> {
   public SplitShaftRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(SplitShaftBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         Block block = be.getBlockState().getBlock();
         Axis boxAxis = ((IRotate)block).getRotationAxis(be.getBlockState());
         BlockPos pos = be.getBlockPos();
         float time = AnimationTickHolder.getRenderTime(be.getLevel());

         for (Direction direction : Iterate.directions) {
            Axis axis = direction.getAxis();
            if (boxAxis == axis) {
               float offset = getRotationOffsetForPosition(be, pos, axis);
               float angle = time * be.getSpeed() * 3.0F / 10.0F % 360.0F;
               float modifier = be.getRotationSpeedModifier(direction);
               angle *= modifier;
               angle += offset;
               angle = angle / 180.0F * (float) Math.PI;
               SuperByteBuffer superByteBuffer = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction);
               kineticRotationTransform(superByteBuffer, be, axis, angle, light);
               superByteBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
            }
         }
      }
   }
}
