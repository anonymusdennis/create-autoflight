package com.simibubi.create.content.kinetics.saw;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class SawRenderer extends SafeBlockEntityRenderer<SawBlockEntity> {
   public SawRenderer(Context context) {
   }

   protected void renderSafe(SawBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      this.renderBlade(be, ms, buffer, light);
      this.renderItems(be, partialTicks, ms, buffer, light, overlay);
      FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         this.renderShaft(be, ms, buffer, light, overlay);
      }
   }

   protected void renderBlade(SawBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light) {
      BlockState blockState = be.getBlockState();
      float speed = be.getSpeed();
      boolean rotate = false;
      PartialModel partial;
      if (SawBlock.isHorizontal(blockState)) {
         if (speed > 0.0F) {
            partial = AllPartialModels.SAW_BLADE_HORIZONTAL_ACTIVE;
         } else if (speed < 0.0F) {
            partial = AllPartialModels.SAW_BLADE_HORIZONTAL_REVERSED;
         } else {
            partial = AllPartialModels.SAW_BLADE_HORIZONTAL_INACTIVE;
         }
      } else {
         if (be.getSpeed() > 0.0F) {
            partial = AllPartialModels.SAW_BLADE_VERTICAL_ACTIVE;
         } else if (speed < 0.0F) {
            partial = AllPartialModels.SAW_BLADE_VERTICAL_REVERSED;
         } else {
            partial = AllPartialModels.SAW_BLADE_VERTICAL_INACTIVE;
         }

         if ((Boolean)blockState.getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE)) {
            rotate = true;
         }
      }

      SuperByteBuffer superBuffer = CachedBuffers.partialFacing(partial, blockState);
      if (rotate) {
         superBuffer.rotateCentered(AngleHelper.rad(90.0), Direction.UP);
      }

      superBuffer.color(16777215).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
   }

   protected void renderShaft(SawBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      KineticBlockEntityRenderer.renderRotatingBuffer(be, this.getRotatedModel(be), ms, buffer.getBuffer(RenderType.solid()), light);
   }

   protected void renderItems(SawBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (be.getBlockState().getValue(SawBlock.FACING) == Direction.UP) {
         if (!be.inventory.isEmpty()) {
            boolean alongZ = !(Boolean)be.getBlockState().getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE);
            float duration = be.inventory.recipeDuration;
            boolean moving = duration != 0.0F;
            float offset = moving ? be.inventory.remainingTime / duration : 0.0F;
            float processingSpeed = Mth.clamp(Math.abs(be.getSpeed()) / 32.0F, 1.0F, 128.0F);
            if (moving) {
               offset = Mth.clamp(offset + (-partialTicks + 0.5F) * processingSpeed / duration, 0.125F, 1.0F);
               if (!be.inventory.appliedRecipe) {
                  offset++;
               }

               offset /= 2.0F;
            }

            if (be.getSpeed() == 0.0F) {
               offset = 0.5F;
            }

            if (be.getSpeed() < 0.0F ^ alongZ) {
               offset = 1.0F - offset;
            }

            int outputs = 0;

            for (int i = 1; i < be.inventory.getSlots(); i++) {
               if (!be.inventory.getStackInSlot(i).isEmpty()) {
                  outputs++;
               }
            }

            ms.pushPose();
            if (alongZ) {
               ms.mulPose(Axis.YP.rotationDegrees(90.0F));
            }

            ms.translate(outputs <= 1 ? 0.5 : 0.25, 0.0, (double)offset);
            ms.translate(alongZ ? -1.0F : 0.0F, 0.0F, 0.0F);
            int renderedI = 0;

            for (int ix = 0; ix < be.inventory.getSlots(); ix++) {
               ItemStack stack = be.inventory.getStackInSlot(ix);
               if (!stack.isEmpty()) {
                  ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
                  BakedModel modelWithOverrides = itemRenderer.getModel(stack, be.getLevel(), null, 0);
                  boolean blockItem = modelWithOverrides.isGui3d();
                  ms.pushPose();
                  ms.translate(0.0F, blockItem ? 0.925F : 0.8125F, 0.0F);
                  if (ix > 0 && outputs > 1) {
                     ms.translate(0.5 / (double)(outputs - 1) * (double)renderedI, 0.0, 0.0);
                     TransformStack.of(ms).nudge(ix * 133);
                  }

                  boolean box = PackageItem.isPackage(stack);
                  if (box) {
                     ms.translate(0.0F, 0.25F, 0.0F);
                     ms.scale(1.5F, 1.5F, 1.5F);
                  } else {
                     ms.scale(0.5F, 0.5F, 0.5F);
                  }

                  if (!box) {
                     ms.mulPose(Axis.XP.rotationDegrees(90.0F));
                  }

                  itemRenderer.render(stack, ItemDisplayContext.FIXED, false, ms, buffer, light, overlay, modelWithOverrides);
                  renderedI++;
                  ms.popPose();
               }
            }

            ms.popPose();
         }
      }
   }

   protected SuperByteBuffer getRotatedModel(KineticBlockEntity be) {
      BlockState state = be.getBlockState();
      return ((Direction)state.getValue(BlockStateProperties.FACING)).getAxis().isHorizontal()
         ? CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state.rotate(be.getLevel(), be.getBlockPos(), Rotation.CLOCKWISE_180))
         : CachedBuffers.block(KineticBlockEntityRenderer.KINETIC_BLOCK, this.getRenderedBlockState(be));
   }

   protected BlockState getRenderedBlockState(KineticBlockEntity be) {
      return KineticBlockEntityRenderer.shaft(KineticBlockEntityRenderer.getRotationAxisOf(be));
   }

   public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
      BlockState state = context.state;
      Direction facing = (Direction)state.getValue(SawBlock.FACING);
      Vec3 facingVec = Vec3.atLowerCornerOf(((Direction)context.state.getValue(SawBlock.FACING)).getNormal());
      facingVec = context.rotation.apply(facingVec);
      Direction closestToFacing = Direction.getNearest(facingVec.x, facingVec.y, facingVec.z);
      boolean horizontal = closestToFacing.getAxis().isHorizontal();
      boolean backwards = VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite());
      boolean moving = context.getAnimationSpeed() != 0.0F;
      boolean shouldAnimate = context.contraption.stalled && horizontal || !context.contraption.stalled && !backwards && moving;
      SuperByteBuffer superBuffer;
      if (SawBlock.isHorizontal(state)) {
         if (shouldAnimate) {
            superBuffer = CachedBuffers.partial(AllPartialModels.SAW_BLADE_HORIZONTAL_ACTIVE, state);
         } else {
            superBuffer = CachedBuffers.partial(AllPartialModels.SAW_BLADE_HORIZONTAL_INACTIVE, state);
         }
      } else if (shouldAnimate) {
         superBuffer = CachedBuffers.partial(AllPartialModels.SAW_BLADE_VERTICAL_ACTIVE, state);
      } else {
         superBuffer = CachedBuffers.partial(AllPartialModels.SAW_BLADE_VERTICAL_INACTIVE, state);
      }

      ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)superBuffer.transform(matrices.getModel())).center())
            .rotateYDegrees(AngleHelper.horizontalAngle(facing)))
         .rotateXDegrees(AngleHelper.verticalAngle(facing));
      if (!SawBlock.isHorizontal(state)) {
         superBuffer.rotateZDegrees(state.getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE) ? 90.0F : 0.0F);
      }

      ((SuperByteBuffer)superBuffer.uncenter())
         .light(LevelRenderer.getLightColor(renderWorld, context.localPos))
         .useLevelLight(context.world, matrices.getWorld())
         .renderInto(matrices.getViewProjection(), buffer.getBuffer(RenderType.cutoutMipped()));
   }
}
