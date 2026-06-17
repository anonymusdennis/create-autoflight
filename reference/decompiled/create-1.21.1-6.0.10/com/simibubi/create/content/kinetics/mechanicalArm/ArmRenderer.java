package com.simibubi.create.content.kinetics.mechanicalArm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ArmRenderer extends KineticBlockEntityRenderer<ArmBlockEntity> {
   public ArmRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(ArmBlockEntity be, float pt, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, pt, ms, buffer, light, overlay);
      ItemStack item = be.heldItem;
      boolean hasItem = !item.isEmpty();
      boolean usingFlywheel = VisualizationManager.supportsVisualization(be.getLevel());
      if (!usingFlywheel || hasItem) {
         ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
         BakedModel bakedModel = itemRenderer.getModel(item, be.getLevel(), null, 0);
         boolean isBlockItem = hasItem && item.getItem() instanceof BlockItem && bakedModel.isGui3d();
         VertexConsumer builder = buffer.getBuffer(be.goggles ? RenderType.cutout() : RenderType.solid());
         BlockState blockState = be.getBlockState();
         PoseStack msLocal = new PoseStack();
         PoseTransformStack msr = TransformStack.of(msLocal);
         boolean inverted = (Boolean)blockState.getValue(ArmBlock.CEILING);
         boolean rave = be.phase == ArmBlockEntity.Phase.DANCING && be.getSpeed() != 0.0F;
         float baseAngle;
         float lowerArmAngle;
         float upperArmAngle;
         float headAngle;
         int color;
         if (rave) {
            float renderTick = AnimationTickHolder.getRenderTime(be.getLevel()) + (float)(be.hashCode() % 64);
            baseAngle = renderTick * 10.0F % 360.0F;
            lowerArmAngle = Mth.lerp((Mth.sin(renderTick / 4.0F) + 1.0F) / 2.0F, -45.0F, 15.0F);
            upperArmAngle = Mth.lerp((Mth.sin(renderTick / 8.0F) + 1.0F) / 4.0F, -45.0F, 95.0F);
            headAngle = -lowerArmAngle;
            color = Color.rainbowColor(AnimationTickHolder.getTicks() * 100).getRGB();
         } else {
            baseAngle = be.baseAngle.getValue(pt);
            lowerArmAngle = be.lowerArmAngle.getValue(pt) - 135.0F;
            upperArmAngle = be.upperArmAngle.getValue(pt) - 90.0F;
            headAngle = be.headAngle.getValue(pt);
            color = 16777215;
         }

         msr.center();
         if (inverted) {
            msr.rotateXDegrees(180.0F);
         }

         if (usingFlywheel) {
            this.doItemTransforms(msr, baseAngle, lowerArmAngle, upperArmAngle, headAngle);
         } else {
            this.renderArm(
               builder,
               ms,
               msLocal,
               msr,
               blockState,
               color,
               baseAngle,
               lowerArmAngle,
               upperArmAngle,
               headAngle,
               be.goggles,
               inverted && be.goggles,
               hasItem,
               isBlockItem,
               light
            );
         }

         if (hasItem) {
            ms.pushPose();
            float itemScale = isBlockItem ? 0.5F : 0.625F;
            msr.rotateXDegrees(90.0F);
            msLocal.translate(0.0F, isBlockItem ? -0.5625F : -0.625F, 0.0F);
            msLocal.scale(itemScale, itemScale, itemScale);
            ms.last().pose().mul(msLocal.last().pose());
            itemRenderer.render(item, ItemDisplayContext.FIXED, false, ms, buffer, light, overlay, bakedModel);
            ms.popPose();
         }
      }
   }

   private void renderArm(
      VertexConsumer builder,
      PoseStack ms,
      PoseStack msLocal,
      TransformStack msr,
      BlockState blockState,
      int color,
      float baseAngle,
      float lowerArmAngle,
      float upperArmAngle,
      float headAngle,
      boolean goggles,
      boolean inverted,
      boolean hasItem,
      boolean isBlockItem,
      int light
   ) {
      SuperByteBuffer base = CachedBuffers.partial(AllPartialModels.ARM_BASE, blockState).light(light);
      SuperByteBuffer lowerBody = CachedBuffers.partial(AllPartialModels.ARM_LOWER_BODY, blockState).light(light);
      SuperByteBuffer upperBody = CachedBuffers.partial(AllPartialModels.ARM_UPPER_BODY, blockState).light(light);
      SuperByteBuffer claw = CachedBuffers.partial(goggles ? AllPartialModels.ARM_CLAW_BASE_GOGGLES : AllPartialModels.ARM_CLAW_BASE, blockState).light(light);
      SuperByteBuffer upperClawGrip = CachedBuffers.partial(AllPartialModels.ARM_CLAW_GRIP_UPPER, blockState).light(light);
      SuperByteBuffer lowerClawGrip = CachedBuffers.partial(AllPartialModels.ARM_CLAW_GRIP_LOWER, blockState).light(light);
      transformBase(msr, baseAngle);
      ((SuperByteBuffer)base.transform(msLocal)).renderInto(ms, builder);
      transformLowerArm(msr, lowerArmAngle);
      ((SuperByteBuffer)lowerBody.color(color).transform(msLocal)).renderInto(ms, builder);
      transformUpperArm(msr, upperArmAngle);
      ((SuperByteBuffer)upperBody.color(color).transform(msLocal)).renderInto(ms, builder);
      transformHead(msr, headAngle);
      if (inverted) {
         msr.rotateZDegrees(180.0F);
      }

      ((SuperByteBuffer)claw.transform(msLocal)).renderInto(ms, builder);
      if (inverted) {
         msr.rotateZDegrees(180.0F);
      }

      for (int flip : Iterate.positiveAndNegative) {
         msLocal.pushPose();
         transformClawHalf(msr, hasItem, isBlockItem, flip);
         ((SuperByteBuffer)(flip > 0 ? lowerClawGrip : upperClawGrip).transform(msLocal)).renderInto(ms, builder);
         msLocal.popPose();
      }
   }

   private void doItemTransforms(TransformStack msr, float baseAngle, float lowerArmAngle, float upperArmAngle, float headAngle) {
      transformBase(msr, baseAngle);
      transformLowerArm(msr, lowerArmAngle);
      transformUpperArm(msr, upperArmAngle);
      transformHead(msr, headAngle);
   }

   public static void transformClawHalf(TransformStack msr, boolean hasItem, boolean isBlockItem, int flip) {
      msr.translate(0.0, (double)((float)(-flip) * (hasItem ? (isBlockItem ? 0.1875F : 0.078125F) : 0.0625F)), -0.375);
   }

   public static void transformHead(TransformStack msr, float headAngle) {
      msr.translate(0.0, 0.0, -0.9375);
      msr.rotateXDegrees(headAngle - 45.0F);
   }

   public static void transformUpperArm(TransformStack msr, float upperArmAngle) {
      msr.translate(0.0, 0.0, -0.875);
      msr.rotateXDegrees(upperArmAngle - 90.0F);
   }

   public static void transformLowerArm(TransformStack msr, float lowerArmAngle) {
      msr.translate(0.0, 0.125, 0.0);
      msr.rotateXDegrees(lowerArmAngle + 135.0F);
   }

   public static void transformBase(TransformStack msr, float baseAngle) {
      msr.translate(0.0, 0.25, 0.0);
      msr.rotateYDegrees(baseAngle);
   }

   public boolean shouldRenderOffScreen(ArmBlockEntity be) {
      return true;
   }

   protected SuperByteBuffer getRotatedModel(ArmBlockEntity be, BlockState state) {
      return CachedBuffers.partial(AllPartialModels.ARM_COG, state);
   }
}
