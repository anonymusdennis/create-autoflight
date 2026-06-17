package dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimPartialModels;
import foundry.veil.api.client.render.VeilRenderBridge;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RedstoneAccumulatorRenderer extends SmartBlockEntityRenderer<RedstoneAccumulatorBlockEntity> {
   public static ResourceLocation SHADER_NAME = Simulated.path("redstone_accumulator/diode");
   public static RenderType DIODE_RENDER_TYPE = RenderType.create(
      "redstone_accumulator_diode",
      DefaultVertexFormat.BLOCK,
      Mode.QUADS,
      131072,
      true,
      false,
      CompositeState.builder()
         .setLightmapState(RenderStateShard.LIGHTMAP)
         .setShaderState(RenderStateShard.RENDERTYPE_CUTOUT_SHADER)
         .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
         .setShaderState(VeilRenderBridge.shaderState(SHADER_NAME))
         .createCompositeState(true)
   );

   public RedstoneAccumulatorRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(RedstoneAccumulatorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      SuperByteBuffer render = CachedBuffers.partial(SimPartialModels.REDSTONE_ACCUMULATOR_DIODE, be.getBlockState())
         .color(255, 255, 255, this.getLitAmount(be, partialTicks));
      Direction facing = (Direction)be.getBlockState().getValue(RedstoneAccumulatorBlock.FACING);
      render.light(light);
      render.translate(0.5, 0.0, 0.5);
      ((SuperByteBuffer)render.rotateYDegrees(AngleHelper.horizontalAngle(facing))).pushPose();
      render.renderInto(ms, buffer.getBuffer(DIODE_RENDER_TYPE));
   }

   private int getLitAmount(RedstoneAccumulatorBlockEntity be, float partialTicks) {
      float state = be.lerpedState.getValue(partialTicks);
      state = 1.0F - (float)Math.pow((double)(state / 15.0F), 1.5);
      return (int)Mth.clamp(state * 255.0F, 0.0F, 255.0F);
   }
}
