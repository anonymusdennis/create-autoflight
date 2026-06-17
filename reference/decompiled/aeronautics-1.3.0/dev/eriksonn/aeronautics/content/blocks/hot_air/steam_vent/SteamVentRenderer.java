package dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.eriksonn.aeronautics.content.blocks.hot_air.GasEmitterRenderHandler;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.level.block.state.BlockState;

public class SteamVentRenderer extends SmartBlockEntityRenderer<SteamVentBlockEntity> {
   public SteamVentRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(SteamVentBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay);
      VertexConsumer cutoutConsumer = buffer.getBuffer(RenderType.cutoutMipped());
      float signalStrength = Math.max(0.0F, (float)blockEntity.signalStrength / 15.0F);
      BlockState state = blockEntity.getBlockState();
      CachedBuffers.partial(AeroPartialModels.STEAM_VENT_REDSTONE, state).light(light).color(SimColors.redstone(signalStrength)).renderInto(ms, cutoutConsumer);
      GasEmitterRenderHandler renderHandler = blockEntity.getRenderHandler();
      int alpha = renderHandler.getAlpha(partialTicks);
      if (alpha > 2) {
         float position = renderHandler.getPosition(partialTicks);
         VertexConsumer translucentConsumer = buffer.getBuffer(RenderType.translucent());
         SuperByteBuffer base = CachedBuffers.partial(AeroPartialModels.STEAM_VENT_BASE, state);
         SuperByteBuffer jet = CachedBuffers.partial(AeroPartialModels.STEAM_VENT_JET, state);
         ms.pushPose();
         base.disableDiffuse().light(15728880).color(255, 255, 255, alpha).renderInto(ms, translucentConsumer);
         ms.translate(0.0F, (position - 1.0F) / 3.0F, 0.0F);
         jet.disableDiffuse().light(15728880).color(255, 255, 255, alpha).renderInto(ms, translucentConsumer);
         ms.popPose();
      }
   }
}
