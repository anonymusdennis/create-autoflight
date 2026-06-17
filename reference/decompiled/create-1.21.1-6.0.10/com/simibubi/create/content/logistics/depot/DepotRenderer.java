package com.simibubi.create.content.logistics.depot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.Random;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class DepotRenderer extends SafeBlockEntityRenderer<DepotBlockEntity> {
   public DepotRenderer(Context context) {
   }

   protected void renderSafe(DepotBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      renderItemsOf(be, partialTicks, ms, buffer, light, overlay, be.depotBehaviour);
   }

   public static void renderItemsOf(
      SmartBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay, DepotBehaviour depotBehaviour
   ) {
      TransportedItemStack transported = depotBehaviour.heldItem;
      PoseTransformStack msr = TransformStack.of(ms);
      Vec3 itemPosition = VecHelper.getCenterOf(be.getBlockPos());
      ms.pushPose();
      ms.translate(0.5F, 0.9375F, 0.5F);
      if (transported != null) {
         depotBehaviour.incoming.add(transported);
      }

      for (TransportedItemStack tis : depotBehaviour.incoming) {
         ms.pushPose();
         msr.nudge(0);
         float offset = Mth.lerp(partialTicks, tis.prevBeltPosition, tis.beltPosition);
         float sideOffset = Mth.lerp(partialTicks, tis.prevSideOffset, tis.sideOffset);
         if (tis.insertedFrom.getAxis().isHorizontal()) {
            Vec3 offsetVec = Vec3.atLowerCornerOf(tis.insertedFrom.getOpposite().getNormal()).scale((double)(0.5F - offset));
            ms.translate(offsetVec.x, offsetVec.y, offsetVec.z);
            boolean alongX = tis.insertedFrom.getClockWise().getAxis() == Axis.X;
            if (!alongX) {
               sideOffset *= -1.0F;
            }

            ms.translate(alongX ? sideOffset : 0.0F, 0.0F, alongX ? 0.0F : sideOffset);
         }

         ItemStack itemStack = tis.stack;
         int angle = tis.angle;
         Random r = new Random(0L);
         renderItem(ms, buffer, light, overlay, itemStack, angle, r, itemPosition, false);
         ms.popPose();
      }

      if (transported != null) {
         depotBehaviour.incoming.remove(transported);
      }

      for (int i = 0; i < depotBehaviour.processingOutputBuffer.getSlots(); i++) {
         ItemStack stack = depotBehaviour.processingOutputBuffer.getStackInSlot(i);
         if (!stack.isEmpty()) {
            ms.pushPose();
            msr.nudge(i);
            boolean renderUpright = BeltHelper.isItemUpright(stack);
            msr.rotateYDegrees(45.0F * (float)i);
            ms.translate(0.35F, 0.0F, 0.0F);
            if (renderUpright) {
               msr.rotateYDegrees(-(45.0F * (float)i));
            }

            Random r = new Random((long)(i + 1));
            int angle = (int)(360.0F * r.nextFloat());
            renderItem(ms, buffer, light, overlay, stack, renderUpright ? angle + 90 : angle, r, itemPosition, false);
            ms.popPose();
         }
      }

      ms.popPose();
   }

   public static void renderItem(
      PoseStack ms, MultiBufferSource buffer, int light, int overlay, ItemStack itemStack, int angle, Random r, Vec3 itemPosition, boolean alwaysUpright
   ) {
      ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
      PoseTransformStack msr = TransformStack.of(ms);
      int count = Mth.log2(itemStack.getCount()) / 2;
      BakedModel bakedModel = itemRenderer.getModel(itemStack, null, null, 0);
      boolean blockItem = bakedModel.isGui3d();
      boolean renderUpright = BeltHelper.isItemUpright(itemStack) || alwaysUpright && !blockItem;
      ms.pushPose();
      msr.rotateYDegrees((float)angle);
      if (renderUpright) {
         Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
         Vec3 diff = itemPosition.subtract(cameraPosition);
         float yRot = (float)(Mth.atan2(diff.x, diff.z) + Math.PI);
         ms.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
         ms.translate(0.0, 0.09375, -0.0625);
      }

      for (int i = 0; i <= count; i++) {
         ms.pushPose();
         if (blockItem && r != null) {
            ms.translate(r.nextFloat() * 0.0625F * (float)i, 0.0F, r.nextFloat() * 0.0625F * (float)i);
         }

         if (PackageItem.isPackage(itemStack) && !alwaysUpright) {
            ms.translate(0.0F, 0.25F, 0.0F);
            ms.scale(1.5F, 1.5F, 1.5F);
         } else if (blockItem && alwaysUpright) {
            ms.translate(0.0F, 0.0625F, 0.0F);
            ms.scale(0.755F, 0.755F, 0.755F);
         } else {
            ms.scale(0.5F, 0.5F, 0.5F);
         }

         if (!blockItem && !renderUpright) {
            ms.translate(0.0F, -0.1875F, 0.0F);
            msr.rotateXDegrees(90.0F);
         }

         itemRenderer.render(itemStack, ItemDisplayContext.FIXED, false, ms, buffer, light, overlay, bakedModel);
         ms.popPose();
         if (!renderUpright) {
            if (!blockItem) {
               msr.rotateYDegrees(10.0F);
            }

            ms.translate(0.0, blockItem ? 0.015625 : 0.0625, 0.0);
         } else {
            ms.translate(0.0F, 0.0F, -0.0625F);
         }
      }

      ms.popPose();
   }
}
