package com.simibubi.create.content.logistics.tableCloth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.List;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class TableClothRenderer extends SmartBlockEntityRenderer<TableClothBlockEntity> {
   public TableClothRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(TableClothBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay);
      List<ItemStack> stacks = blockEntity.getItemsForRender();
      float rotationInRadians = (float) (Math.PI / 180.0) * (180.0F - blockEntity.facing.toYRot());
      if (blockEntity.isShop()) {
         ((SuperByteBuffer)CachedBuffers.partial(
                  blockEntity.sideOccluded ? AllPartialModels.TABLE_CLOTH_PRICE_TOP : AllPartialModels.TABLE_CLOTH_PRICE_SIDE, blockEntity.getBlockState()
               )
               .rotateCentered(rotationInRadians, Direction.UP))
            .light(light)
            .overlay(overlay)
            .renderInto(ms, buffer.getBuffer(RenderType.cutout()));
      }

      ms.pushPose();
      TransformStack.of(ms).rotateCentered(rotationInRadians, Direction.UP);

      for (int i = 0; i < stacks.size(); i++) {
         ItemStack entry = stacks.get(i);
         ms.pushPose();
         ms.translate(0.5F, 0.1875F, 0.5F);
         if (stacks.size() > 1) {
            ms.mulPose(Axis.YP.rotationDegrees((float)i * (360.0F / (float)stacks.size()) + 45.0F));
            ms.translate(0.0, i % 2 == 0 ? -0.005 : 0.0, 0.3125);
            ms.mulPose(Axis.YP.rotationDegrees((float)(-i) * (360.0F / (float)stacks.size()) - 45.0F));
         }

         BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(entry, null, null, 0);
         boolean blockItem = bakedModel.isGui3d();
         if (!blockItem) {
            TransformStack.of(ms).rotate(-rotationInRadians + (float) Math.PI, Direction.UP);
         }

         DepotRenderer.renderItem(ms, buffer, light, OverlayTexture.NO_OVERLAY, entry, 0, null, Vec3.atCenterOf(blockEntity.getBlockPos()), true);
         ms.popPose();
      }

      ms.popPose();
   }
}
