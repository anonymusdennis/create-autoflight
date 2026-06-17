package com.simibubi.create.content.kinetics.crank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class HandCrankRenderer extends KineticBlockEntityRenderer<HandCrankBlockEntity> {
   public HandCrankRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(HandCrankBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (be.shouldRenderShaft()) {
         super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      }

      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         Direction facing = (Direction)be.getBlockState().getValue(BlockStateProperties.FACING);
         kineticRotationTransform(be.getRenderedHandle(), be, facing.getAxis(), AngleHelper.rad((double)be.getIndependentAngle(partialTicks)), light)
            .renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }
   }
}
