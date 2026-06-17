package com.simibubi.create.content.redstone.link.controller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class LinkedControllerItemRenderer extends CustomRenderedItemModelRenderer {
   protected static final PartialModel POWERED = PartialModel.of(Create.asResource("item/linked_controller/powered"));
   protected static final PartialModel BUTTON = PartialModel.of(Create.asResource("item/linked_controller/button"));
   static LerpedFloat equipProgress = LerpedFloat.linear().startWithValue(0.0);
   static List<LerpedFloat> buttons = new ArrayList<>(6);

   static void tick() {
      if (!Minecraft.getInstance().isPaused()) {
         boolean active = LinkedControllerClientHandler.MODE != LinkedControllerClientHandler.Mode.IDLE;
         equipProgress.chase(active ? 1.0 : 0.0, 0.2F, Chaser.EXP);
         equipProgress.tickChaser();
         if (active) {
            for (int i = 0; i < buttons.size(); i++) {
               LerpedFloat lerpedFloat = buttons.get(i);
               lerpedFloat.chase(LinkedControllerClientHandler.currentlyPressed.contains(i) ? 1.0 : 0.0, 0.4F, Chaser.EXP);
               lerpedFloat.tickChaser();
            }
         }
      }
   }

   static void resetButtons() {
      for (LerpedFloat button : buttons) {
         button.startWithValue(0.0);
      }
   }

   @Override
   protected void render(
      ItemStack stack,
      CustomRenderedItemModel model,
      PartialItemModelRenderer renderer,
      ItemDisplayContext transformType,
      PoseStack ms,
      MultiBufferSource buffer,
      int light,
      int overlay
   ) {
      renderNormal(stack, model, renderer, transformType, ms, light);
   }

   protected static void renderNormal(
      ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms, int light
   ) {
      render(stack, model, renderer, transformType, ms, light, LinkedControllerItemRenderer.RenderType.NORMAL, false, false);
   }

   public static void renderInLectern(
      ItemStack stack,
      CustomRenderedItemModel model,
      PartialItemModelRenderer renderer,
      ItemDisplayContext transformType,
      PoseStack ms,
      int light,
      boolean active,
      boolean renderDepression
   ) {
      render(stack, model, renderer, transformType, ms, light, LinkedControllerItemRenderer.RenderType.LECTERN, active, renderDepression);
   }

   protected static void render(
      ItemStack stack,
      CustomRenderedItemModel model,
      PartialItemModelRenderer renderer,
      ItemDisplayContext transformType,
      PoseStack ms,
      int light,
      LinkedControllerItemRenderer.RenderType renderType,
      boolean active,
      boolean renderDepression
   ) {
      float pt = AnimationTickHolder.getPartialTicks();
      PoseTransformStack msr = TransformStack.of(ms);
      ms.pushPose();
      if (renderType == LinkedControllerItemRenderer.RenderType.NORMAL) {
         Minecraft mc = Minecraft.getInstance();
         boolean rightHanded = mc.options.mainHand().get() == HumanoidArm.RIGHT;
         ItemDisplayContext mainHand = rightHanded ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
         ItemDisplayContext offHand = rightHanded ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
         active = false;
         boolean noControllerInMain = !AllItems.LINKED_CONTROLLER.isIn(mc.player.getMainHandItem());
         if (transformType == mainHand || transformType == offHand && noControllerInMain) {
            float equip = equipProgress.getValue(pt);
            int handModifier = transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
            msr.translate(0.0F, equip / 4.0F, equip / 4.0F * (float)handModifier);
            msr.rotateYDegrees(equip * -30.0F * (float)handModifier);
            msr.rotateZDegrees(equip * -30.0F);
            active = true;
         }

         if (transformType == ItemDisplayContext.GUI) {
            if (stack == mc.player.getMainHandItem()) {
               active = true;
            }

            if (stack == mc.player.getOffhandItem() && noControllerInMain) {
               active = true;
            }
         }

         active &= LinkedControllerClientHandler.MODE != LinkedControllerClientHandler.Mode.IDLE;
         renderDepression = true;
      }

      renderer.render(active ? POWERED.get() : model.getOriginalModel(), light);
      if (!active) {
         ms.popPose();
      } else {
         BakedModel button = BUTTON.get();
         float s = 0.0625F;
         float b = s * -0.75F;
         int index = 0;
         if (renderType == LinkedControllerItemRenderer.RenderType.NORMAL && LinkedControllerClientHandler.MODE == LinkedControllerClientHandler.Mode.BIND) {
            int i = (int)Mth.lerp((Mth.sin(AnimationTickHolder.getRenderTime() / 4.0F) + 1.0F) / 2.0F, 5.0F, 15.0F);
            light = i << 20;
         }

         ms.pushPose();
         msr.translate(2.0F * s, 0.0F, 8.0F * s);
         renderButton(renderer, ms, light, pt, button, b, index++, renderDepression);
         msr.translate(4.0F * s, 0.0F, 0.0F);
         renderButton(renderer, ms, light, pt, button, b, index++, renderDepression);
         msr.translate(-2.0F * s, 0.0F, 2.0F * s);
         renderButton(renderer, ms, light, pt, button, b, index++, renderDepression);
         msr.translate(0.0F, 0.0F, -4.0F * s);
         renderButton(renderer, ms, light, pt, button, b, index++, renderDepression);
         ms.popPose();
         msr.translate(3.0F * s, 0.0F, 3.0F * s);
         renderButton(renderer, ms, light, pt, button, b, index++, renderDepression);
         msr.translate(2.0F * s, 0.0F, 0.0F);
         renderButton(renderer, ms, light, pt, button, b, index++, renderDepression);
         ms.popPose();
      }
   }

   protected static void renderButton(
      PartialItemModelRenderer renderer, PoseStack ms, int light, float pt, BakedModel button, float b, int index, boolean renderDepression
   ) {
      ms.pushPose();
      if (renderDepression) {
         float depression = b * buttons.get(index).getValue(pt);
         ms.translate(0.0F, depression, 0.0F);
      }

      renderer.renderSolid(button, light);
      ms.popPose();
   }

   static {
      for (int i = 0; i < 6; i++) {
         buttons.add(LerpedFloat.linear().startWithValue(0.0));
      }
   }

   protected static enum RenderType {
      NORMAL,
      LECTERN;
   }
}
