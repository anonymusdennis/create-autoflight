package com.simibubi.create.content.fluids.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class FluidTankRenderer extends SafeBlockEntityRenderer<FluidTankBlockEntity> {
   public FluidTankRenderer(Context context) {
   }

   protected void renderSafe(FluidTankBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (be.isController()) {
         if (!be.window) {
            if (be.boiler.isActive()) {
               this.renderAsBoiler(be, partialTicks, ms, buffer, light, overlay);
            }
         } else {
            LerpedFloat fluidLevel = be.getFluidLevel();
            if (fluidLevel != null) {
               float capHeight = 0.25F;
               float tankHullWidth = 0.0703125F;
               float minPuddleHeight = 0.0625F;
               float totalHeight = (float)be.height - 2.0F * capHeight - minPuddleHeight;
               float level = fluidLevel.getValue(partialTicks);
               if (!(level < 1.0F / (512.0F * totalHeight))) {
                  float clampedLevel = Mth.clamp(level * totalHeight, 0.0F, totalHeight);
                  FluidTank tank = be.tankInventory;
                  FluidStack fluidStack = tank.getFluid();
                  if (!fluidStack.isEmpty()) {
                     boolean top = fluidStack.getFluid().getFluidType().isLighterThanAir();
                     float xMax = tankHullWidth + (float)be.width - 2.0F * tankHullWidth;
                     float yMin = totalHeight + capHeight + minPuddleHeight - clampedLevel;
                     float yMax = yMin + clampedLevel;
                     if (top) {
                        yMin += totalHeight - clampedLevel;
                        yMax += totalHeight - clampedLevel;
                     }

                     float zMax = tankHullWidth + (float)be.width - 2.0F * tankHullWidth;
                     ms.pushPose();
                     ms.translate(0.0F, clampedLevel - totalHeight, 0.0F);
                     NeoForgeCatnipServices.FLUID_RENDERER
                        .renderFluidBox(fluidStack, tankHullWidth, yMin, tankHullWidth, xMax, yMax, zMax, buffer, ms, light, false, true);
                     ms.popPose();
                  }
               }
            }
         }
      }
   }

   protected void renderAsBoiler(FluidTankBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState blockState = be.getBlockState();
      VertexConsumer vb = buffer.getBuffer(RenderType.cutout());
      ms.pushPose();
      PoseTransformStack msr = TransformStack.of(ms);
      msr.translate((double)((float)be.width / 2.0F), 0.5, (double)((float)be.width / 2.0F));
      float dialPivotY = 0.375F;
      float dialPivotZ = 0.5F;
      float progress = be.boiler.gauge.getValue(partialTicks);

      for (Direction d : Iterate.horizontalDirections) {
         if (!be.boiler.occludedDirections[d.get2DDataValue()]) {
            ms.pushPose();
            float yRot = -d.toYRot() - 90.0F;
            ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.BOILER_GAUGE, blockState).rotateYDegrees(yRot))
                     .uncenter())
                  .translate((float)be.width / 2.0F - 0.375F, 0.0F, 0.0F))
               .light(light)
               .renderInto(ms, vb);
            ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(
                                    AllPartialModels.BOILER_GAUGE_DIAL, blockState
                                 )
                                 .rotateYDegrees(yRot))
                              .uncenter())
                           .translate((float)be.width / 2.0F - 0.375F, 0.0F, 0.0F))
                        .translate(0.0F, dialPivotY, dialPivotZ))
                     .rotateXDegrees(-145.0F * progress + 90.0F))
                  .translate(0.0F, -dialPivotY, -dialPivotZ))
               .light(light)
               .renderInto(ms, vb);
            ms.popPose();
         }
      }

      ms.popPose();
   }

   public boolean shouldRenderOffScreen(FluidTankBlockEntity be) {
      return be.isController();
   }
}
