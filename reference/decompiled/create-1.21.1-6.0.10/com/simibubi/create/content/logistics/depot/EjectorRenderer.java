package com.simibubi.create.content.logistics.depot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.logistics.box.PackageItem;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.Rotate;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.transform.Translate;
import net.createmod.catnip.data.IntAttached;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class EjectorRenderer extends ShaftRenderer<EjectorBlockEntity> {
   static final Vec3 pivot = VecHelper.voxelSpace(0.0, 11.25, 0.75);

   public EjectorRenderer(Context context) {
      super(context);
   }

   public boolean shouldRenderOffScreen(EjectorBlockEntity p_188185_1_) {
      return true;
   }

   protected void renderSafe(EjectorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      float lidProgress = be.getLidProgress(partialTicks);
      float angle = lidProgress * 70.0F;
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         SuperByteBuffer model = CachedBuffers.partial(AllPartialModels.EJECTOR_TOP, be.getBlockState());
         applyLidAngle(be, angle, model);
         model.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }

      PoseTransformStack msr = TransformStack.of(ms);
      float maxTime = (float)(be.earlyTarget != null ? (double)be.earlyTargetTime : be.launcher.getTotalFlyingTicks());

      for (IntAttached<ItemStack> intAttached : be.launchedItems) {
         float time = (float)((Integer)intAttached.getFirst()).intValue() + partialTicks;
         if (!(time > maxTime)) {
            ms.pushPose();
            Vec3 launchedItemLocation = be.getLaunchedItemLocation(time);
            msr.translate(launchedItemLocation.subtract(Vec3.atLowerCornerOf(be.getBlockPos())));
            Vec3 itemRotOffset = VecHelper.voxelSpace(0.0, 2.0, -1.0);
            msr.translate(itemRotOffset);
            if (PackageItem.isPackage((ItemStack)intAttached.getValue())) {
               ms.translate(0.0F, 0.25F, 0.0F);
               ms.scale(1.5F, 1.5F, 1.5F);
               msr.rotateYDegrees(time * 20.0F);
            } else {
               ms.scale(0.5F, 0.5F, 0.5F);
               msr.rotateYDegrees(AngleHelper.horizontalAngle(be.getFacing()));
               msr.rotateXDegrees(time * 40.0F);
            }

            msr.translateBack(itemRotOffset);
            Minecraft.getInstance()
               .getItemRenderer()
               .renderStatic((ItemStack)intAttached.getValue(), ItemDisplayContext.FIXED, light, overlay, ms, buffer, be.getLevel(), 0);
            ms.popPose();
         }
      }

      DepotBehaviour behaviour = be.getBehaviour(DepotBehaviour.TYPE);
      if (behaviour != null && !behaviour.isEmpty()) {
         ms.pushPose();
         applyLidAngle(be, angle, msr);
         ((PoseTransformStack)((PoseTransformStack)msr.center())
               .rotateYDegrees(-180.0F - AngleHelper.horizontalAngle((Direction)be.getBlockState().getValue(EjectorBlock.HORIZONTAL_FACING))))
            .uncenter();
         DepotRenderer.renderItemsOf(be, partialTicks, ms, buffer, light, overlay, behaviour);
         ms.popPose();
      }
   }

   static <T extends Translate<T> & Rotate<T>> void applyLidAngle(KineticBlockEntity be, float angle, T tr) {
      applyLidAngle(be, pivot, angle, tr);
   }

   static <T extends Translate<T> & Rotate<T>> void applyLidAngle(KineticBlockEntity be, Vec3 rotationOffset, float angle, T tr) {
      ((Translate)((Rotate)((Translate)((Rotate)tr.center())
                  .rotateYDegrees(180.0F + AngleHelper.horizontalAngle((Direction)be.getBlockState().getValue(EjectorBlock.HORIZONTAL_FACING))))
               .uncenter()
               .translate(rotationOffset))
            .rotateXDegrees(-angle))
         .translateBack(rotationOffset);
   }
}
