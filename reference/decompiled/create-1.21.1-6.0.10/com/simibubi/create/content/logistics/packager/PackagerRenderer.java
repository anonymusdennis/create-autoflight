package com.simibubi.create.content.logistics.packager;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PackagerRenderer extends SmartBlockEntityRenderer<PackagerBlockEntity> {
   public PackagerRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(PackagerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      ItemStack renderedBox = be.getRenderedBox();
      float trayOffset = be.getTrayOffset(partialTicks);
      BlockState blockState = be.getBlockState();
      Direction facing = ((Direction)blockState.getValue(PackagerBlock.FACING)).getOpposite();
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         PartialModel hatchModel = getHatchModel(be);
         SuperByteBuffer sbb = CachedBuffers.partial(hatchModel, blockState);
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)sbb.translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.49999F)))
                  .rotateYCenteredDegrees(AngleHelper.horizontalAngle(facing)))
               .rotateXCenteredDegrees(AngleHelper.verticalAngle(facing)))
            .light(light)
            .renderInto(ms, buffer.getBuffer(RenderType.solid()));
         sbb = CachedBuffers.partial(getTrayModel(blockState), blockState);
         ((SuperByteBuffer)((SuperByteBuffer)sbb.translate(Vec3.atLowerCornerOf(facing.getNormal()).scale((double)trayOffset)))
               .rotateYCenteredDegrees(facing.toYRot()))
            .light(light)
            .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
      }

      if (!renderedBox.isEmpty()) {
         ms.pushPose();
         PoseTransformStack msr = TransformStack.of(ms);
         ((PoseTransformStack)((PoseTransformStack)msr.translate(Vec3.atLowerCornerOf(facing.getNormal()).scale((double)trayOffset)))
               .translate(0.5F, 0.5F, 0.5F)
               .rotateYDegrees(facing.toYRot()))
            .translate(0.0F, 0.125F, 0.0F)
            .scale(1.49F, 1.49F, 1.49F);
         Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(null, renderedBox, ItemDisplayContext.FIXED, false, ms, buffer, be.getLevel(), light, overlay, 0);
         ms.popPose();
      }
   }

   public static PartialModel getTrayModel(BlockState blockState) {
      return AllBlocks.PACKAGER.has(blockState) ? AllPartialModels.PACKAGER_TRAY_REGULAR : AllPartialModels.PACKAGER_TRAY_DEFRAG;
   }

   public static PartialModel getHatchModel(PackagerBlockEntity be) {
      return isHatchOpen(be) ? AllPartialModels.PACKAGER_HATCH_OPEN : AllPartialModels.PACKAGER_HATCH_CLOSED;
   }

   public static boolean isHatchOpen(PackagerBlockEntity be) {
      return be.animationTicks > (be.animationInward ? 1 : 5) && be.animationTicks < 20 - (be.animationInward ? 5 : 1);
   }
}
