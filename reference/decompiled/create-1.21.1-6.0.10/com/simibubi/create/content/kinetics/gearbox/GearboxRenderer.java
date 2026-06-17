package com.simibubi.create.content.kinetics.gearbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class GearboxRenderer extends KineticBlockEntityRenderer<GearboxBlockEntity> {
   public GearboxRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(GearboxBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         Axis boxAxis = (Axis)be.getBlockState().getValue(BlockStateProperties.AXIS);
         BlockPos pos = be.getBlockPos();
         float time = AnimationTickHolder.getRenderTime(be.getLevel());

         for (Direction direction : Iterate.directions) {
            Axis axis = direction.getAxis();
            if (boxAxis != axis) {
               SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction);
               float offset = getRotationOffsetForPosition(be, pos, axis);
               float angle = time * be.getSpeed() * 3.0F / 10.0F % 360.0F;
               if (be.getSpeed() != 0.0F && be.hasSource()) {
                  BlockPos source = be.source.subtract(be.getBlockPos());
                  Direction sourceFacing = Direction.getNearest((float)source.getX(), (float)source.getY(), (float)source.getZ());
                  if (sourceFacing.getAxis() == direction.getAxis()) {
                     angle *= sourceFacing == direction ? 1.0F : -1.0F;
                  } else if (sourceFacing.getAxisDirection() == direction.getAxisDirection()) {
                     angle *= -1.0F;
                  }
               }

               angle += offset;
               angle = angle / 180.0F * (float) Math.PI;
               kineticRotationTransform(shaft, be, axis, angle, light);
               shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
            }
         }
      }
   }
}
