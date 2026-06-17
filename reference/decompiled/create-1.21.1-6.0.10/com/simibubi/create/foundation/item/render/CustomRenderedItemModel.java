package com.simibubi.create.foundation.item.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

public class CustomRenderedItemModel extends BakedModelWrapper<BakedModel> {
   public CustomRenderedItemModel(BakedModel originalModel) {
      super(originalModel);
   }

   public boolean isCustomRenderer() {
      return true;
   }

   public BakedModel applyTransform(ItemDisplayContext cameraItemDisplayContext, PoseStack mat, boolean leftHand) {
      super.applyTransform(cameraItemDisplayContext, mat, leftHand);
      return this;
   }

   public BakedModel getOriginalModel() {
      return this.originalModel;
   }
}
