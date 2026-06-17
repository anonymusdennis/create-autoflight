package com.simibubi.create.content.kinetics.deployer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DeployerRenderer extends SafeBlockEntityRenderer<DeployerBlockEntity> {
   public DeployerRenderer(Context context) {
   }

   protected void renderSafe(DeployerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      this.renderItem(be, partialTicks, ms, buffer, light, overlay);
      FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         this.renderComponents(be, partialTicks, ms, buffer, light, overlay);
      }
   }

   protected void renderItem(DeployerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!be.heldItem.isEmpty()) {
         BlockState deployerState = be.getBlockState();
         Vec3 offset = this.getHandOffset(be, partialTicks, deployerState).add(VecHelper.getCenterOf(BlockPos.ZERO));
         ms.pushPose();
         ms.translate(offset.x, offset.y, offset.z);
         Direction facing = (Direction)deployerState.getValue(DirectionalKineticBlock.FACING);
         boolean punching = be.mode == DeployerBlockEntity.Mode.PUNCH;
         float yRot = AngleHelper.horizontalAngle(facing) + 180.0F;
         float xRot = facing == Direction.UP ? 90.0F : (facing == Direction.DOWN ? 270.0F : 0.0F);
         boolean displayMode = facing == Direction.UP && be.getSpeed() == 0.0F && !punching;
         ms.mulPose(Axis.YP.rotationDegrees(yRot));
         if (!displayMode) {
            ms.mulPose(Axis.XP.rotationDegrees(xRot));
            ms.translate(0.0F, 0.0F, -0.6875F);
         }

         if (punching) {
            ms.translate(0.0F, 0.125F, -0.0625F);
         }

         ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
         ItemDisplayContext transform = ItemDisplayContext.NONE;
         BakedModel bakedModel = itemRenderer.getModel(be.heldItem, be.getLevel(), null, 0);
         boolean isBlockItem = be.heldItem.getItem() instanceof BlockItem && bakedModel.isGui3d();
         if (displayMode) {
            float scale = isBlockItem ? 1.25F : 1.0F;
            ms.translate(0.0F, isBlockItem ? 0.5625F : 0.6875F, 0.0F);
            ms.scale(scale, scale, scale);
            transform = ItemDisplayContext.GROUND;
            ms.mulPose(Axis.YP.rotationDegrees(AnimationTickHolder.getRenderTime(be.getLevel())));
         } else {
            float scale = punching ? 0.75F : (isBlockItem ? 0.734375F : 0.5F);
            ms.scale(scale, scale, scale);
            transform = punching ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.FIXED;
         }

         itemRenderer.render(be.heldItem, transform, false, ms, buffer, light, overlay, bakedModel);
         ms.popPose();
      }
   }

   protected void renderComponents(DeployerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      VertexConsumer vb = buffer.getBuffer(RenderType.solid());
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         KineticBlockEntityRenderer.renderRotatingKineticBlock(be, this.getRenderedBlockState(be), ms, vb, light);
      }

      BlockState blockState = be.getBlockState();
      Vec3 offset = this.getHandOffset(be, partialTicks, blockState);
      SuperByteBuffer pole = CachedBuffers.partial(AllPartialModels.DEPLOYER_POLE, blockState);
      SuperByteBuffer hand = CachedBuffers.partial(be.getHandPose(), blockState);
      transform((SuperByteBuffer)pole.translate(offset.x, offset.y, offset.z), blockState, true).light(light).renderInto(ms, vb);
      transform((SuperByteBuffer)hand.translate(offset.x, offset.y, offset.z), blockState, false).light(light).renderInto(ms, vb);
   }

   protected Vec3 getHandOffset(DeployerBlockEntity be, float partialTicks, BlockState blockState) {
      float distance = be.getHandOffset(partialTicks);
      return Vec3.atLowerCornerOf(((Direction)blockState.getValue(DirectionalKineticBlock.FACING)).getNormal()).scale((double)distance);
   }

   protected BlockState getRenderedBlockState(KineticBlockEntity be) {
      return KineticBlockEntityRenderer.shaft(KineticBlockEntityRenderer.getRotationAxisOf(be));
   }

   private static SuperByteBuffer transform(SuperByteBuffer buffer, BlockState deployerState, boolean axisDirectionMatters) {
      Direction facing = (Direction)deployerState.getValue(DirectionalKineticBlock.FACING);
      float yRot = AngleHelper.horizontalAngle(facing);
      float xRot = facing == Direction.UP ? 270.0F : (facing == Direction.DOWN ? 90.0F : 0.0F);
      float zRot = axisDirectionMatters
            && deployerState.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE) ^ facing.getAxis() == net.minecraft.core.Direction.Axis.Z
         ? 90.0F
         : 0.0F;
      buffer.rotateCentered((float)((double)(yRot / 180.0F) * Math.PI), Direction.UP);
      buffer.rotateCentered((float)((double)(xRot / 180.0F) * Math.PI), Direction.EAST);
      buffer.rotateCentered((float)((double)(zRot / 180.0F) * Math.PI), Direction.SOUTH);
      return buffer;
   }

   public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
      VertexConsumer builder = buffer.getBuffer(RenderType.solid());
      BlockState blockState = context.state;
      DeployerBlockEntity.Mode mode = (DeployerBlockEntity.Mode)NBTHelper.readEnum(context.blockEntityData, "Mode", DeployerBlockEntity.Mode.class);
      PartialModel handPose = getHandPose(mode);
      float speed = context.getAnimationSpeed();
      if (context.contraption.stalled) {
         speed = 0.0F;
      }

      SuperByteBuffer shaft = CachedBuffers.block(AllBlocks.SHAFT.getDefaultState());
      SuperByteBuffer pole = CachedBuffers.partial(AllPartialModels.DEPLOYER_POLE, blockState);
      SuperByteBuffer hand = CachedBuffers.partial(handPose, blockState);
      double factor;
      if (!context.contraption.stalled && context.position != null && !context.data.contains("StationaryTimer")) {
         Vec3 center = VecHelper.getCenterOf(BlockPos.containing(context.position));
         double distance = context.position.distanceTo(center);
         double nextDistance = context.position.add(context.motion).distanceTo(center);
         factor = 0.5 - Mth.clamp(Mth.lerp((double)AnimationTickHolder.getPartialTicks(), distance, nextDistance), 0.0, 1.0);
      } else {
         factor = (double)(Mth.sin(AnimationTickHolder.getRenderTime() * 0.5F) * 0.25F + 0.25F);
      }

      Vec3 offset = Vec3.atLowerCornerOf(((Direction)blockState.getValue(DirectionalKineticBlock.FACING)).getNormal()).scale(factor);
      PoseStack m = matrices.getModel();
      m.pushPose();
      m.pushPose();
      net.minecraft.core.Direction.Axis axis = net.minecraft.core.Direction.Axis.Y;
      if (context.state.getBlock() instanceof IRotate def) {
         axis = def.getRotationAxis(context.state);
      }

      float time = AnimationTickHolder.getRenderTime(context.world) / 20.0F;
      float angle = time * speed % 360.0F;
      ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(m).center())
               .rotateYDegrees(axis == net.minecraft.core.Direction.Axis.Z ? 90.0F : 0.0F))
            .rotateZDegrees(axis.isHorizontal() ? 90.0F : 0.0F))
         .uncenter();
      shaft.transform(m);
      shaft.rotateCentered(angle, Direction.get(AxisDirection.POSITIVE, net.minecraft.core.Direction.Axis.Y));
      m.popPose();
      if (!context.disabled) {
         m.translate(offset.x, offset.y, offset.z);
      }

      pole.transform(m);
      hand.transform(m);
      transform(pole, blockState, true);
      transform(hand, blockState, false);
      shaft.light(LevelRenderer.getLightColor(renderWorld, context.localPos))
         .useLevelLight(context.world, matrices.getWorld())
         .renderInto(matrices.getViewProjection(), builder);
      pole.light(LevelRenderer.getLightColor(renderWorld, context.localPos))
         .useLevelLight(context.world, matrices.getWorld())
         .renderInto(matrices.getViewProjection(), builder);
      hand.light(LevelRenderer.getLightColor(renderWorld, context.localPos))
         .useLevelLight(context.world, matrices.getWorld())
         .renderInto(matrices.getViewProjection(), builder);
      m.popPose();
   }

   static PartialModel getHandPose(DeployerBlockEntity.Mode mode) {
      return mode == DeployerBlockEntity.Mode.PUNCH ? AllPartialModels.DEPLOYER_HAND_PUNCHING : AllPartialModels.DEPLOYER_HAND_POINTING;
   }
}
