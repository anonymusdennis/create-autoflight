package com.simibubi.create.foundation.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.mixin.accessor.HumanoidArmorLayerAccessor;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface LayeredArmorItem extends CustomRenderedArmorItem {
   @OnlyIn(Dist.CLIENT)
   @Override
   default void renderArmorPiece(
      HumanoidArmorLayer<?, ?, ?> layer,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      LivingEntity entity,
      EquipmentSlot slot,
      int light,
      HumanoidModel<?> originalModel,
      ItemStack stack
   ) {
      if (stack.getItem() instanceof ArmorItem item) {
         if (item.canEquip(stack, slot, entity)) {
            HumanoidArmorLayerAccessor accessor = (HumanoidArmorLayerAccessor)layer;
            Map<String, ResourceLocation> locationCache = HumanoidArmorLayerAccessor.create$getArmorLocationCache();
            boolean glint = stack.hasFoil();
            HumanoidModel<?> innerModel = accessor.create$getInnerModel();
            ((HumanoidModel)layer.getParentModel()).copyPropertiesTo(innerModel);
            accessor.create$callSetPartVisibility(innerModel, slot);
            String locationStr2 = this.getArmorTextureLocation(entity, slot, stack, 2);
            ResourceLocation location2 = locationCache.computeIfAbsent(locationStr2, ResourceLocation::parse);
            this.renderModel(poseStack, bufferSource, light, item, innerModel, glint, -1, location2);
            HumanoidModel<?> outerModel = accessor.create$getOuterModel();
            ((HumanoidModel)layer.getParentModel()).copyPropertiesTo(outerModel);
            accessor.create$callSetPartVisibility(outerModel, slot);
            String locationStr1 = this.getArmorTextureLocation(entity, slot, stack, 1);
            ResourceLocation location1 = locationCache.computeIfAbsent(locationStr1, ResourceLocation::parse);
            this.renderModel(poseStack, bufferSource, light, item, outerModel, glint, -1, location1);
         }
      }
   }

   private void renderModel(
      PoseStack poseStack, MultiBufferSource bufferSource, int light, ArmorItem item, Model model, boolean glint, int color, ResourceLocation armorResource
   ) {
      VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.armorCutoutNoCull(armorResource));
      model.renderToBuffer(poseStack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, color);
      if (glint) {
         model.renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY);
      }
   }

   String getArmorTextureLocation(LivingEntity var1, EquipmentSlot var2, ItemStack var3, int var4);
}
