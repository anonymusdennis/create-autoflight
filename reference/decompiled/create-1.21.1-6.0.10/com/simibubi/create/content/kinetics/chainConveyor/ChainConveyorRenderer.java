package com.simibubi.create.content.kinetics.chainConveyor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.render.RenderTypes;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.List;
import java.util.Map.Entry;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ChainConveyorRenderer extends KineticBlockEntityRenderer<ChainConveyorBlockEntity> {
   public static final ResourceLocation CHAIN_LOCATION = ResourceLocation.withDefaultNamespace("textures/block/chain.png");
   public static final int MIP_DISTANCE = 48;

   public ChainConveyorRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(ChainConveyorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      BlockPos pos = be.getBlockPos();
      this.renderChains(be, ms, buffer, light, overlay);
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         CachedBuffers.partial(AllPartialModels.CHAIN_CONVEYOR_WHEEL, be.getBlockState())
            .light(light)
            .overlay(overlay)
            .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

         for (ChainConveyorPackage box : be.loopingPackages) {
            this.renderBox(be, ms, buffer, overlay, pos, box, partialTicks);
         }

         for (Entry<BlockPos, List<ChainConveyorPackage>> entry : be.travellingPackages.entrySet()) {
            for (ChainConveyorPackage box : entry.getValue()) {
               this.renderBox(be, ms, buffer, overlay, pos, box, partialTicks);
            }
         }
      }
   }

   private void renderBox(
      ChainConveyorBlockEntity be, PoseStack ms, MultiBufferSource buffer, int overlay, BlockPos pos, ChainConveyorPackage box, float partialTicks
   ) {
      if (box.worldPosition != null) {
         if (box.item != null && !box.item.isEmpty()) {
            ChainConveyorPackage.ChainConveyorPackagePhysicsData physicsData = box.physicsData(be.getLevel());
            if (physicsData.prevPos != null) {
               Vec3 position = physicsData.prevPos.lerp(physicsData.pos, (double)partialTicks);
               Vec3 targetPosition = physicsData.prevTargetPos.lerp(physicsData.targetPos, (double)partialTicks);
               float yaw = AngleHelper.angleLerp((double)partialTicks, (double)physicsData.prevYaw, (double)physicsData.yaw);
               Vec3 offset = new Vec3(targetPosition.x - (double)pos.getX(), targetPosition.y - (double)pos.getY(), targetPosition.z - (double)pos.getZ());
               BlockPos containingPos = BlockPos.containing(position);
               Level level = be.getLevel();
               BlockState blockState = be.getBlockState();
               int light = LightTexture.pack(level.getBrightness(LightLayer.BLOCK, containingPos), level.getBrightness(LightLayer.SKY, containingPos));
               if (physicsData.modelKey == null) {
                  ResourceLocation key = BuiltInRegistries.ITEM.getKey(box.item.getItem());
                  if (key == BuiltInRegistries.ITEM.getDefaultKey()) {
                     return;
                  }

                  physicsData.modelKey = key;
               }

               SuperByteBuffer rigBuffer = CachedBuffers.partial(AllPartialModels.PACKAGE_RIGGING.get(physicsData.modelKey), blockState);
               SuperByteBuffer boxBuffer = CachedBuffers.partial(AllPartialModels.PACKAGES.get(physicsData.modelKey), blockState);
               Vec3 dangleDiff = VecHelper.rotate(targetPosition.add(0.0, 0.5, 0.0).subtract(position), (double)(-yaw), Axis.Y);
               float zRot = Mth.wrapDegrees((float)Mth.atan2(-dangleDiff.x, dangleDiff.y) * (180.0F / (float)Math.PI)) / 2.0F;
               float xRot = Mth.wrapDegrees((float)Mth.atan2(dangleDiff.z, dangleDiff.y) * (180.0F / (float)Math.PI)) / 2.0F;
               zRot = Mth.clamp(zRot, -25.0F, 25.0F);
               xRot = Mth.clamp(xRot, -25.0F, 25.0F);

               for (SuperByteBuffer buf : new SuperByteBuffer[]{rigBuffer, boxBuffer}) {
                  buf.translate(offset);
                  buf.translate(0.0F, 0.625F, 0.0F);
                  buf.rotateYDegrees(yaw);
                  buf.rotateZDegrees(zRot);
                  buf.rotateXDegrees(xRot);
                  if (physicsData.flipped && buf == rigBuffer) {
                     buf.rotateYDegrees(180.0F);
                  }

                  buf.uncenter();
                  buf.translate(0.0F, -PackageItem.getHookDistance(box.item) + 0.4375F, 0.0F);
                  buf.light(light).overlay(overlay).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
               }
            }
         }
      }
   }

   private void renderChains(ChainConveyorBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      float time = AnimationTickHolder.getRenderTime(be.getLevel()) / (360.0F / Math.abs(be.getSpeed()));
      time %= 1.0F;
      if (time < 0.0F) {
         time++;
      }

      float animation = time - 0.5F;

      for (BlockPos blockPos : be.connections) {
         ChainConveyorBlockEntity.ConnectionStats stats = be.connectionStats.get(blockPos);
         if (stats != null) {
            Vec3 diff = stats.end().subtract(stats.start());
            double yaw = 180.0F / (float)Math.PI * Mth.atan2(diff.x, diff.z);
            double pitch = 180.0F / (float)Math.PI * Mth.atan2(diff.y, diff.multiply(1.0, 0.0, 1.0).length());
            Level level = be.getLevel();
            BlockPos tilePos = be.getBlockPos();
            Vec3 startOffset = stats.start().subtract(Vec3.atCenterOf(tilePos));
            if (!VisualizationManager.supportsVisualization(be.getLevel())) {
               SuperByteBuffer guard = CachedBuffers.partial(AllPartialModels.CHAIN_CONVEYOR_GUARD, be.getBlockState());
               guard.center();
               guard.rotateYDegrees((float)yaw);
               guard.uncenter();
               guard.light(light).overlay(overlay).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
            }

            ms.pushPose();
            PoseTransformStack chain = TransformStack.of(ms);
            chain.center();
            chain.translate(startOffset);
            chain.rotateYDegrees((float)yaw);
            chain.rotateXDegrees(90.0F - (float)pitch);
            chain.rotateYDegrees(45.0F);
            chain.translate(0.0F, 0.5F, 0.0F);
            chain.uncenter();
            int light1 = LightTexture.pack(level.getBrightness(LightLayer.BLOCK, tilePos), level.getBrightness(LightLayer.SKY, tilePos));
            int light2 = LightTexture.pack(
               level.getBrightness(LightLayer.BLOCK, tilePos.offset(blockPos)), level.getBrightness(LightLayer.SKY, tilePos.offset(blockPos))
            );
            boolean far = Minecraft.getInstance().level == be.getLevel()
               && !Minecraft.getInstance()
                  .getBlockEntityRenderDispatcher()
                  .camera
                  .getPosition()
                  .closerThan(
                     Vec3.atCenterOf(tilePos)
                        .add((double)((float)blockPos.getX() / 2.0F), (double)((float)blockPos.getY() / 2.0F), (double)((float)blockPos.getZ() / 2.0F)),
                     48.0
                  );
            renderChain(ms, buffer, animation, stats.chainLength(), light1, light2, far);
            ms.popPose();
         }
      }
   }

   public static void renderChain(PoseStack ms, MultiBufferSource buffer, float animation, float length, int light1, int light2, boolean far) {
      float radius = far ? 0.0625F : 0.09375F;
      float minV = far ? 0.0F : animation;
      float maxV = far ? 0.0625F : length + minV;
      float minU = far ? 0.1875F : 0.0F;
      float maxU = far ? 0.25F : 0.1875F;
      ms.pushPose();
      ms.translate(0.5, 0.0, 0.5);
      VertexConsumer vc = buffer.getBuffer(RenderTypes.chain(CHAIN_LOCATION));
      renderPart(ms, vc, length, 0.0F, radius, radius, 0.0F, -radius, 0.0F, 0.0F, -radius, minU, maxU, minV, maxV, light1, light2, far);
      ms.popPose();
   }

   private static void renderPart(
      PoseStack pPoseStack,
      VertexConsumer pConsumer,
      float pMaxY,
      float pX0,
      float pZ0,
      float pX1,
      float pZ1,
      float pX2,
      float pZ2,
      float pX3,
      float pZ3,
      float pMinU,
      float pMaxU,
      float pMinV,
      float pMaxV,
      int light1,
      int light2,
      boolean far
   ) {
      Pose posestack$pose = pPoseStack.last();
      Matrix4f matrix4f = posestack$pose.pose();
      float uO = far ? 0.0F : 0.1875F;
      renderQuad(matrix4f, posestack$pose, pConsumer, 0.0F, pMaxY, pX0, pZ0, pX3, pZ3, pMinU, pMaxU, pMinV, pMaxV, light1, light2);
      renderQuad(matrix4f, posestack$pose, pConsumer, 0.0F, pMaxY, pX3, pZ3, pX0, pZ0, pMinU, pMaxU, pMinV, pMaxV, light1, light2);
      renderQuad(matrix4f, posestack$pose, pConsumer, 0.0F, pMaxY, pX1, pZ1, pX2, pZ2, pMinU + uO, pMaxU + uO, pMinV, pMaxV, light1, light2);
      renderQuad(matrix4f, posestack$pose, pConsumer, 0.0F, pMaxY, pX2, pZ2, pX1, pZ1, pMinU + uO, pMaxU + uO, pMinV, pMaxV, light1, light2);
   }

   private static void renderQuad(
      Matrix4f pPose,
      Pose pNormal,
      VertexConsumer pConsumer,
      float pMinY,
      float pMaxY,
      float pMinX,
      float pMinZ,
      float pMaxX,
      float pMaxZ,
      float pMinU,
      float pMaxU,
      float pMinV,
      float pMaxV,
      int light1,
      int light2
   ) {
      addVertex(pPose, pNormal, pConsumer, pMaxY, pMinX, pMinZ, pMaxU, pMinV, light2);
      addVertex(pPose, pNormal, pConsumer, pMinY, pMinX, pMinZ, pMaxU, pMaxV, light1);
      addVertex(pPose, pNormal, pConsumer, pMinY, pMaxX, pMaxZ, pMinU, pMaxV, light1);
      addVertex(pPose, pNormal, pConsumer, pMaxY, pMaxX, pMaxZ, pMinU, pMinV, light2);
   }

   private static void addVertex(Matrix4f pPose, Pose pNormal, VertexConsumer pConsumer, float pY, float pX, float pZ, float pU, float pV, int light) {
      pConsumer.addVertex(pPose, pX, pY, pZ)
         .setColor(1.0F, 1.0F, 1.0F, 1.0F)
         .setUv(pU, pV)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(pNormal, 0.0F, 1.0F, 0.0F);
   }

   public int getViewDistance() {
      return 256;
   }

   public boolean shouldRenderOffScreen(ChainConveyorBlockEntity be) {
      return true;
   }

   protected SuperByteBuffer getRotatedModel(ChainConveyorBlockEntity be, BlockState state) {
      return CachedBuffers.partial(AllPartialModels.CHAIN_CONVEYOR_SHAFT, state);
   }

   protected RenderType getRenderType(ChainConveyorBlockEntity be, BlockState state) {
      return RenderType.cutoutMipped();
   }
}
