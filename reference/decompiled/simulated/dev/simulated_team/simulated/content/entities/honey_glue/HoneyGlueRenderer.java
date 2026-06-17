package dev.simulated_team.simulated.content.entities.honey_glue;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class HoneyGlueRenderer extends EntityRenderer<HoneyGlueEntity> {
   public HoneyGlueRenderer(Context context) {
      super(context);
   }

   public ResourceLocation getTextureLocation(HoneyGlueEntity entity) {
      return ResourceLocation.parse("");
   }

   public boolean shouldRender(HoneyGlueEntity entity, Frustum frustum, double x, double y, double z) {
      return false;
   }
}
