package com.simibubi.create.content.redstone.analogLever;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class AnalogLeverRenderer extends SafeBlockEntityRenderer<AnalogLeverBlockEntity> {
   public AnalogLeverRenderer(Context context) {
   }

   protected void renderSafe(AnalogLeverBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         BlockState leverState = be.getBlockState();
         float state = be.clientState.getValue(partialTicks);
         VertexConsumer vb = buffer.getBuffer(RenderType.solid());
         SuperByteBuffer handle = CachedBuffers.partial(AllPartialModels.ANALOG_LEVER_HANDLE, leverState);
         float angle = (float)((double)(state / 15.0F * 90.0F / 180.0F) * Math.PI);
         ((SuperByteBuffer)((SuperByteBuffer)this.transform(handle, leverState).translate(0.5F, 0.0625F, 0.5F)).rotate(angle, Direction.EAST))
            .translate(-0.5F, -0.0625F, -0.5F);
         handle.light(light).renderInto(ms, vb);
         int color = Color.mixColors(2884352, 13434880, state / 15.0F);
         SuperByteBuffer indicator = this.transform(CachedBuffers.partial(AllPartialModels.ANALOG_LEVER_INDICATOR, leverState), leverState);
         indicator.light(light).color(color).renderInto(ms, vb);
      }
   }

   private SuperByteBuffer transform(SuperByteBuffer buffer, BlockState leverState) {
      AttachFace face = (AttachFace)leverState.getValue(AnalogLeverBlock.FACE);
      float rX = face == AttachFace.FLOOR ? 0.0F : (face == AttachFace.WALL ? 90.0F : 180.0F);
      float rY = AngleHelper.horizontalAngle((Direction)leverState.getValue(AnalogLeverBlock.FACING));
      buffer.rotateCentered((float)((double)(rY / 180.0F) * Math.PI), Direction.UP);
      buffer.rotateCentered((float)((double)(rX / 180.0F) * Math.PI), Direction.EAST);
      return buffer;
   }
}
