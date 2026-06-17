package dev.simulated_team.simulated.content.blocks.analog_transmission;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;

public class AnalogTransmissionRenderer extends KineticBlockEntityRenderer<AnalogTransmissionBlockEntity> {
   public AnalogTransmissionRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(AnalogTransmissionBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
         BlockState state = be.getBlockState();
         Axis axis = ((IRotate)state.getBlock()).getRotationAxis(state);
         SuperByteBuffer cogwheel = kineticRotationTransform(
            CachedBuffers.partialFacingVertical(
               SimPartialModels.ANALOG_TRANSMISSION_COG,
               state,
               Direction.fromAxisAndDirection((Axis)state.getValue(AnalogTransmissionBlock.AXIS), AxisDirection.POSITIVE)
            ),
            be.getExtraKinetics(),
            axis,
            getAngleForBe(be.getExtraKinetics(), be.getBlockPos(), axis),
            light
         );
         cogwheel.renderInto(ms, buffer.getBuffer(RenderType.solid()));
         VertexConsumer vb = buffer.getBuffer(RenderType.solid());
         KineticBlockEntityRenderer.renderRotatingKineticBlock(be, shaft(getRotationAxisOf(be)), ms, vb, light);
      }
   }

   protected BlockState getRenderedBlockState(AnalogTransmissionBlockEntity be) {
      return shaft(getRotationAxisOf(be));
   }
}
