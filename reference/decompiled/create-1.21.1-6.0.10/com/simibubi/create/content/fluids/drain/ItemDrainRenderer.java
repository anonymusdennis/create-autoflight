package com.simibubi.create.content.fluids.drain;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.Random;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public class ItemDrainRenderer extends SmartBlockEntityRenderer<ItemDrainBlockEntity> {
   public ItemDrainRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(ItemDrainBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      this.renderFluid(be, partialTicks, ms, buffer, light);
      this.renderItem(be, partialTicks, ms, buffer, light, overlay);
   }

   protected void renderItem(ItemDrainBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      TransportedItemStack transported = be.heldItem;
      if (transported != null) {
         PoseTransformStack msr = TransformStack.of(ms);
         Vec3 itemPosition = VecHelper.getCenterOf(be.getBlockPos());
         Direction insertedFrom = transported.insertedFrom;
         if (insertedFrom.getAxis().isHorizontal()) {
            ms.pushPose();
            ms.translate(0.5F, 0.9375F, 0.5F);
            msr.nudge(0);
            float offset = Mth.lerp(partialTicks, transported.prevBeltPosition, transported.beltPosition);
            float sideOffset = Mth.lerp(partialTicks, transported.prevSideOffset, transported.sideOffset);
            Vec3 offsetVec = Vec3.atLowerCornerOf(insertedFrom.getOpposite().getNormal()).scale((double)(0.5F - offset));
            ms.translate(offsetVec.x, offsetVec.y, offsetVec.z);
            boolean alongX = insertedFrom.getClockWise().getAxis() == Axis.X;
            if (!alongX) {
               sideOffset *= -1.0F;
            }

            ms.translate(alongX ? sideOffset : 0.0F, 0.0F, alongX ? 0.0F : sideOffset);
            ItemStack itemStack = transported.stack;
            Random r = new Random(0L);
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            int count = Mth.log2(itemStack.getCount()) / 2;
            boolean renderUpright = BeltHelper.isItemUpright(itemStack);
            BakedModel bakedModel = itemRenderer.getModel(itemStack, null, null, 0);
            boolean blockItem = bakedModel.isGui3d();
            if (renderUpright) {
               ms.translate(0.0, 0.09375, 0.0);
            }

            int positive = insertedFrom.getAxisDirection().getStep();
            float verticalAngle = (float)positive * offset * 360.0F;
            if (insertedFrom.getAxis() != Axis.X) {
               msr.rotateXDegrees(verticalAngle);
            }

            if (insertedFrom.getAxis() != Axis.Z) {
               msr.rotateZDegrees(-verticalAngle);
            }

            if (renderUpright) {
               Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
               Vec3 vectorForOffset = itemPosition.add(offsetVec);
               Vec3 diff = vectorForOffset.subtract(cameraPosition);
               if (insertedFrom.getAxis() != Axis.X) {
                  diff = VecHelper.rotate(diff, (double)verticalAngle, Axis.X);
               }

               if (insertedFrom.getAxis() != Axis.Z) {
                  diff = VecHelper.rotate(diff, (double)(-verticalAngle), Axis.Z);
               }

               float yRot = (float)Mth.atan2(diff.z, -diff.x);
               ms.mulPose(com.mojang.math.Axis.YP.rotation((float)((double)yRot - (Math.PI / 2))));
               ms.translate(0.0F, 0.0F, -0.0625F);
            }

            for (int i = 0; i <= count; i++) {
               ms.pushPose();
               if (blockItem) {
                  ms.translate(r.nextFloat() * 0.0625F * (float)i, 0.0F, r.nextFloat() * 0.0625F * (float)i);
               }

               ms.scale(0.5F, 0.5F, 0.5F);
               if (!blockItem && !renderUpright) {
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
   }

   protected void renderFluid(ItemDrainBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
      SmartFluidTankBehaviour tank = be.internalTank;
      if (tank != null) {
         SmartFluidTankBehaviour.TankSegment primaryTank = tank.getPrimaryTank();
         FluidStack fluidStack = primaryTank.getRenderedFluid();
         float level = primaryTank.getFluidLevel().getValue(partialTicks);
         if (!fluidStack.isEmpty() && level != 0.0F) {
            float yMin = 0.3125F;
            float min = 0.125F;
            float max = min + 0.75F;
            float yOffset = 0.4375F * level;
            ms.pushPose();
            ms.translate(0.0F, yOffset, 0.0F);
            NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, min, yMin - yOffset, min, max, yMin, max, buffer, ms, light, false, false);
            ms.popPose();
         }

         ItemStack heldItemStack = be.getHeldItemStack();
         if (!heldItemStack.isEmpty()) {
            FluidStack fluidStack2 = (FluidStack)GenericItemEmptying.emptyItem(be.getLevel(), heldItemStack, true).getFirst();
            if (fluidStack2.isEmpty()) {
               if (fluidStack.isEmpty()) {
                  return;
               }

               fluidStack2 = fluidStack;
            }

            int processingTicks = be.processingTicks;
            float processingPT = (float)be.processingTicks - partialTicks;
            float processingProgress = 1.0F - (processingPT - 5.0F) / 10.0F;
            processingProgress = Mth.clamp(processingProgress, 0.0F, 1.0F);
            float radius = 0.0F;
            if (processingTicks != -1) {
               radius = (float)(Math.pow((double)(2.0F * processingProgress - 1.0F), 2.0) - 1.0);
               AABB bb = new AABB(0.5, 1.0, 0.5, 0.5, 0.25, 0.5).inflate((double)(radius / 32.0F));
               NeoForgeCatnipServices.FLUID_RENDERER
                  .renderFluidBox(
                     fluidStack2,
                     (float)bb.minX,
                     (float)bb.minY,
                     (float)bb.minZ,
                     (float)bb.maxX,
                     (float)bb.maxY,
                     (float)bb.maxZ,
                     buffer,
                     ms,
                     light,
                     true,
                     false
                  );
            }
         }
      }
   }
}
