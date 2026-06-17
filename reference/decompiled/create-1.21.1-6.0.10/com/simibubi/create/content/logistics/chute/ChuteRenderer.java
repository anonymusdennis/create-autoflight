package com.simibubi.create.content.logistics.chute;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

public class ChuteRenderer extends SafeBlockEntityRenderer<ChuteBlockEntity> {
   public ChuteRenderer(Context context) {
   }

   protected void renderSafe(ChuteBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!be.item.isEmpty()) {
         BlockState blockState = be.getBlockState();
         if (blockState.getValue(ChuteBlock.FACING) == Direction.DOWN) {
            if (blockState.getValue(ChuteBlock.SHAPE) == ChuteBlock.Shape.WINDOW
               || be.bottomPullDistance != 0.0F && !(be.itemPosition.getValue(partialTicks) > 0.5F)) {
               renderItem(be, partialTicks, ms, buffer, light, overlay);
            }
         }
      }
   }

   public static void renderItem(ChuteBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
      PoseTransformStack msr = TransformStack.of(ms);
      ms.pushPose();
      msr.center();
      float itemScale = 0.5F;
      float itemPosition = be.itemPosition.getValue(partialTicks);
      ms.translate(0.0, -0.5 + (double)itemPosition, 0.0);
      if (PackageItem.isPackage(be.item)) {
         ms.scale(1.5F, 1.5F, 1.5F);
      } else {
         ms.scale(itemScale, itemScale, itemScale);
         msr.rotateXDegrees(itemPosition * 180.0F);
         msr.rotateYDegrees(itemPosition * 180.0F);
      }

      itemRenderer.renderStatic(be.item, ItemDisplayContext.FIXED, light, overlay, ms, buffer, be.getLevel(), 0);
      ms.popPose();
   }
}
