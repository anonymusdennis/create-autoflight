package com.simibubi.create.content.equipment.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.mixin.accessor.EntityRenderDispatcherAccessor;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class BacktankArmorLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
   public BacktankArmorLayer(RenderLayerParent<T, M> renderer) {
      super(renderer);
   }

   public void render(
      PoseStack ms,
      MultiBufferSource buffer,
      int light,
      T entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (entity.getPose() != Pose.SLEEPING) {
         BacktankItem item = BacktankItem.getWornBy(entity);
         if (item != null) {
            if (this.getParentModel() instanceof HumanoidModel<?> model) {
               boolean hasGlint = entity.getItemBySlot(BacktankItem.SLOT).hasFoil();
               VertexConsumer vc = ItemRenderer.getFoilBuffer(buffer, Sheets.cutoutBlockSheet(), false, true);
               BlockState renderedState = (BlockState)item.getBlock().defaultBlockState().setValue(BacktankBlock.HORIZONTAL_FACING, Direction.SOUTH);
               SuperByteBuffer backtank = CachedBuffers.block(renderedState);
               SuperByteBuffer cogs = CachedBuffers.partial(BacktankRenderer.getCogsModel(renderedState), renderedState);
               SuperByteBuffer nob = CachedBuffers.partial(BacktankRenderer.getShaftModel(renderedState), renderedState);
               ms.pushPose();
               model.body.translateAndRotate(ms);
               ms.translate(-0.5F, 0.625F, 1.0F);
               ms.scale(1.0F, -1.0F, -1.0F);
               backtank.disableDiffuse().light(light).renderInto(ms, vc);
               ((SuperByteBuffer)nob.disableDiffuse().translate(0.0F, -0.1875F, 0.0F)).light(light).renderInto(ms, vc);
               ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)cogs.center()).rotateYDegrees(180.0F)).uncenter())
                        .translate(0.0F, 0.40625F, 0.6875F))
                     .rotate(AngleHelper.rad((double)(2.0F * AnimationTickHolder.getRenderTime(entity.level()) % 360.0F)), Direction.EAST))
                  .translate(0.0F, -0.40625F, -0.6875F);
               cogs.disableDiffuse().light(light).renderInto(ms, vc);
               ms.popPose();
            }
         }
      }
   }

   public static void registerOnAll(EntityRenderDispatcher renderManager) {
      for (EntityRenderer<? extends Player> renderer : renderManager.getSkinMap().values()) {
         registerOn(renderer);
      }

      for (EntityRenderer<?> renderer : ((EntityRenderDispatcherAccessor)renderManager).create$getRenderers().values()) {
         registerOn(renderer);
      }
   }

   public static void registerOn(EntityRenderer<?> entityRenderer) {
      if (entityRenderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
         if (livingRenderer.getModel() instanceof HumanoidModel) {
            BacktankArmorLayer<?, ?> layer = new BacktankArmorLayer((RenderLayerParent<T, M>)livingRenderer);
            livingRenderer.addLayer(layer);
         }
      }
   }
}
