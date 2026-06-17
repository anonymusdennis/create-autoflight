package com.simibubi.create.content.decoration.placard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class PlacardRenderer extends SafeBlockEntityRenderer<PlacardBlockEntity> {
   public PlacardRenderer(Context context) {
   }

   protected void renderSafe(PlacardBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      ItemStack heldItem = be.getHeldItem();
      if (!heldItem.isEmpty()) {
         BlockState blockState = be.getBlockState();
         Direction facing = (Direction)blockState.getValue(PlacardBlock.FACING);
         AttachFace face = (AttachFace)blockState.getValue(PlacardBlock.FACE);
         ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
         BakedModel bakedModel = itemRenderer.getModel(heldItem, null, null, 0);
         boolean blockItem = bakedModel.isGui3d();
         ms.pushPose();
         ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(ms).center())
                     .rotate(
                        (face == AttachFace.CEILING ? (float) Math.PI : 0.0F) + AngleHelper.rad((double)(180.0F + AngleHelper.horizontalAngle(facing))),
                        Direction.UP
                     ))
                  .rotate(face == AttachFace.CEILING ? (float) (-Math.PI / 2) : (face == AttachFace.FLOOR ? (float) (Math.PI / 2) : 0.0F), Direction.EAST))
               .translate(0.0, 0.0, 0.28125))
            .scale(blockItem ? 0.5F : 0.375F);
         itemRenderer.render(heldItem, ItemDisplayContext.FIXED, false, ms, buffer, light, overlay, bakedModel);
         ms.popPose();
      }
   }
}
