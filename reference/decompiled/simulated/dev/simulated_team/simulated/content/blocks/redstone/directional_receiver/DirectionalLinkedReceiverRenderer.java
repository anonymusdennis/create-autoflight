package dev.simulated_team.simulated.content.blocks.redstone.directional_receiver;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;

public class DirectionalLinkedReceiverRenderer extends SmartBlockEntityRenderer<DirectionalLinkedReceiverBlockEntity> {
   public DirectionalLinkedReceiverRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(DirectionalLinkedReceiverBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, bufferSource, light, overlay);
   }
}
