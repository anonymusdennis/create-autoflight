package dev.simulated_team.simulated.content.entities.diagram;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class DiagramEntityRenderer extends EntityRenderer<DiagramEntity> {
   public DiagramEntityRenderer(Context context) {
      super(context);
   }

   public void render(DiagramEntity entity, float yaw, float pt, PoseStack ms, MultiBufferSource buffer, int light) {
      PartialModel partialModel = entity.size == 3
         ? SimPartialModels.CONTRAPTION_DIAGRAM_3x3
         : (entity.size == 2 ? SimPartialModels.CONTRAPTION_DIAGRAM_2x2 : SimPartialModels.CONTRAPTION_DIAGRAM_1x1);
      SuperByteBuffer sbb = CachedBuffers.partial(partialModel, Blocks.AIR.defaultBlockState());
      ((SuperByteBuffer)((SuperByteBuffer)sbb.rotateYDegrees(-yaw)).rotateXDegrees(90.0F + entity.getXRot())).translate(-0.5, -0.03125, -0.5);
      if (entity.size == 2) {
         sbb.translate(0.5, 0.0, -0.5);
      }

      sbb.disableDiffuse().light(light).renderInto(ms, buffer.getBuffer(Sheets.solidBlockSheet()));
      super.render(entity, yaw, pt, ms, buffer, light);
   }

   public ResourceLocation getTextureLocation(DiagramEntity entity) {
      return null;
   }
}
