package com.simibubi.create.content.contraptions.render;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;

public class OrientedContraptionEntityRenderer extends ContraptionEntityRenderer<OrientedContraptionEntity> {
   public OrientedContraptionEntityRenderer(Context context) {
      super(context);
   }

   public boolean shouldRender(OrientedContraptionEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
      return !super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ)
         ? false
         : entity.getVehicle() != null || !AllTags.AllContraptionTypeTags.REQUIRES_VEHICLE_FOR_RENDER.matches(entity.getContraption().getType());
   }
}
