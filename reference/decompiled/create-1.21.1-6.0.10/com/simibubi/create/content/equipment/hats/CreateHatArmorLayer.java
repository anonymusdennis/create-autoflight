package com.simibubi.create.content.equipment.hats;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.schedule.hat.TrainHatInfo;
import com.simibubi.create.content.trains.schedule.hat.TrainHatInfoReloadListener;
import com.simibubi.create.foundation.mixin.accessor.AgeableListModelAccessor;
import com.simibubi.create.foundation.mixin.accessor.EntityRenderDispatcherAccessor;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CreateHatArmorLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
   public CreateHatArmorLayer(RenderLayerParent<T, M> renderer) {
      super(renderer);
   }

   public void render(
      PoseStack ms,
      MultiBufferSource buffer,
      int light,
      LivingEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      PartialModel hat = EntityHats.getHatFor(entity);
      if (hat != null) {
         M entityModel = (M)this.getParentModel();
         ms.pushPose();
         PoseTransformStack msr = TransformStack.of(ms);
         TrainHatInfo info = TrainHatInfoReloadListener.getHatInfoFor(entity);
         List<ModelPart> partsToHead = new ArrayList<>();
         if (entityModel instanceof AgeableListModel<?> model) {
            if (model.young) {
               if (model.scaleHead) {
                  float f = 1.5F / model.babyHeadScale;
                  ms.scale(f, f, f);
               }

               ms.translate(0.0, (double)(model.babyYHeadOffset / 16.0F), (double)(model.babyZHeadOffset / 16.0F));
            }

            ModelPart head = getHeadPart(model);
            if (head != null) {
               partsToHead.addAll(TrainHatInfo.getAdjustedPart(info, head, ""));
            }
         } else if (entityModel instanceof HierarchicalModel<?> model) {
            partsToHead.addAll(TrainHatInfo.getAdjustedPart(info, model.root(), "head"));
         }

         if (!partsToHead.isEmpty()) {
            partsToHead.forEach(part -> part.translateAndRotate(ms));
            ModelPart lastChild = partsToHead.get(partsToHead.size() - 1);
            if (!lastChild.isEmpty()) {
               Cube cube = (Cube)lastChild.cubes.get(Mth.clamp(info.cubeIndex(), 0, lastChild.cubes.size() - 1));
               ms.translate(info.offset().x() / 16.0, ((double)(cube.minY - cube.maxY) + info.offset().y()) / 16.0, info.offset().z() / 16.0);
               float max = Math.max(cube.maxX - cube.minX, cube.maxZ - cube.minZ) / 8.0F * info.scale();
               ms.scale(max, max, max);
            }

            ms.scale(1.0F, -1.0F, -1.0F);
            ms.translate(0.0F, -0.140625F, 0.0F);
            msr.rotateXDegrees(-8.5F);
            BlockState air = Blocks.AIR.defaultBlockState();
            CachedBuffers.partial(hat, air).disableDiffuse().light(light).renderInto(ms, buffer.getBuffer(Sheets.cutoutBlockSheet()));
         }

         ms.popPose();
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
         EntityModel<?> model = livingRenderer.getModel();
         if (model instanceof HierarchicalModel || model instanceof AgeableListModel) {
            CreateHatArmorLayer<?, ?> layer = new CreateHatArmorLayer((RenderLayerParent<T, M>)livingRenderer);
            livingRenderer.addLayer(layer);
         }
      }
   }

   private static ModelPart getHeadPart(AgeableListModel<?> model) {
      Iterator var1 = ((AgeableListModelAccessor)model).create$callHeadParts().iterator();
      if (var1.hasNext()) {
         return (ModelPart)var1.next();
      } else {
         var1 = ((AgeableListModelAccessor)model).create$callBodyParts().iterator();
         return var1.hasNext() ? (ModelPart)var1.next() : null;
      }
   }
}
