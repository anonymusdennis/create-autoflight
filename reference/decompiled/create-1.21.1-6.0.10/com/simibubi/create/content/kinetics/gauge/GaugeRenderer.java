package com.simibubi.create.content.kinetics.gauge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class GaugeRenderer extends ShaftRenderer<GaugeBlockEntity> {
   protected GaugeBlock.Type type;

   public static GaugeRenderer speed(Context context) {
      return new GaugeRenderer(context, GaugeBlock.Type.SPEED);
   }

   public static GaugeRenderer stress(Context context) {
      return new GaugeRenderer(context, GaugeBlock.Type.STRESS);
   }

   protected GaugeRenderer(Context context, GaugeBlock.Type type) {
      super(context);
      this.type = type;
   }

   protected void renderSafe(GaugeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
         BlockState gaugeState = be.getBlockState();
         PartialModel partialModel = this.type == GaugeBlock.Type.SPEED ? AllPartialModels.GAUGE_HEAD_SPEED : AllPartialModels.GAUGE_HEAD_STRESS;
         SuperByteBuffer headBuffer = CachedBuffers.partial(partialModel, gaugeState);
         SuperByteBuffer dialBuffer = CachedBuffers.partial(AllPartialModels.GAUGE_DIAL, gaugeState);
         float dialPivot = 0.359375F;
         float progress = Mth.lerp(partialTicks, be.prevDialState, be.dialState);

         for (Direction facing : Iterate.directions) {
            if (((GaugeBlock)gaugeState.getBlock()).shouldRenderHeadOnFace(be.getLevel(), be.getBlockPos(), gaugeState, facing)) {
               VertexConsumer vb = buffer.getBuffer(RenderType.solid());
               ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)this.rotateBufferTowards(dialBuffer, facing).translate(0.0F, dialPivot, dialPivot))
                        .rotate((float)((Math.PI / 2) * (double)(-progress)), Direction.EAST))
                     .translate(0.0F, -dialPivot, -dialPivot))
                  .light(light)
                  .renderInto(ms, vb);
               this.rotateBufferTowards(headBuffer, facing).light(light).renderInto(ms, vb);
            }
         }
      }
   }

   protected SuperByteBuffer rotateBufferTowards(SuperByteBuffer buffer, Direction target) {
      return (SuperByteBuffer)buffer.rotateCentered((float)((double)((-target.toYRot() - 90.0F) / 180.0F) * Math.PI), Direction.UP);
   }
}
