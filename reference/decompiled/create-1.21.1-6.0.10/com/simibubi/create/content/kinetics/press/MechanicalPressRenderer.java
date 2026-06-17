package com.simibubi.create.content.kinetics.press;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class MechanicalPressRenderer extends KineticBlockEntityRenderer<MechanicalPressBlockEntity> {
   public MechanicalPressRenderer(Context context) {
      super(context);
   }

   public boolean shouldRenderOffScreen(MechanicalPressBlockEntity be) {
      return true;
   }

   protected void renderSafe(MechanicalPressBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         BlockState blockState = be.getBlockState();
         PressingBehaviour pressingBehaviour = be.getPressingBehaviour();
         float renderedHeadOffset = pressingBehaviour.getRenderedHeadOffset(partialTicks) * pressingBehaviour.mode.headOffset;
         SuperByteBuffer headRender = CachedBuffers.partialFacing(
            AllPartialModels.MECHANICAL_PRESS_HEAD, blockState, (Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
         );
         ((SuperByteBuffer)headRender.translate(0.0F, -renderedHeadOffset, 0.0F)).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }
   }

   protected BlockState getRenderedBlockState(MechanicalPressBlockEntity be) {
      return shaft(getRotationAxisOf(be));
   }
}
