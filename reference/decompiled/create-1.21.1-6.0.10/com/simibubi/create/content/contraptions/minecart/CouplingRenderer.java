package com.simibubi.create.content.contraptions.minecart;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import com.simibubi.create.content.kinetics.KineticDebugger;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class CouplingRenderer {
   public static void renderAll(PoseStack ms, MultiBufferSource buffer, Vec3 camera) {
      CouplingHandler.forEachLoadedCoupling(Minecraft.getInstance().level, c -> {
         if (!((MinecartController)c.getFirst()).hasContraptionCoupling(true)) {
            renderCoupling(ms, buffer, camera, c.map(MinecartController::cart));
         }
      });
   }

   public static void tickDebugModeRenders() {
      if (KineticDebugger.isActive()) {
         CouplingHandler.forEachLoadedCoupling(Minecraft.getInstance().level, CouplingRenderer::doDebugRender);
      }
   }

   public static void renderCoupling(PoseStack ms, MultiBufferSource buffer, Vec3 camera, Couple<AbstractMinecart> carts) {
      ClientLevel world = Minecraft.getInstance().level;
      if (carts.getFirst() != null && carts.getSecond() != null) {
         Couple<Integer> lightValues = carts.map(c -> LevelRenderer.getLightColor(world, BlockPos.containing(c.getBoundingBox().getCenter())));
         Vec3 center = ((AbstractMinecart)carts.getFirst()).position().add(((AbstractMinecart)carts.getSecond()).position()).scale(0.5);
         Couple<CouplingRenderer.CartEndpoint> transforms = carts.map(c -> getSuitableCartEndpoint(c, center));
         BlockState renderState = Blocks.AIR.defaultBlockState();
         VertexConsumer builder = buffer.getBuffer(RenderType.solid());
         SuperByteBuffer attachment = CachedBuffers.partial(AllPartialModels.COUPLING_ATTACHMENT, renderState);
         SuperByteBuffer ring = CachedBuffers.partial(AllPartialModels.COUPLING_RING, renderState);
         SuperByteBuffer connector = CachedBuffers.partial(AllPartialModels.COUPLING_CONNECTOR, renderState);
         Vec3 zero = Vec3.ZERO;
         Vec3 firstEndpoint = ((CouplingRenderer.CartEndpoint)transforms.getFirst()).apply(zero);
         Vec3 secondEndpoint = ((CouplingRenderer.CartEndpoint)transforms.getSecond()).apply(zero);
         Vec3 endPointDiff = secondEndpoint.subtract(firstEndpoint);
         double connectorYaw = -Math.atan2(endPointDiff.z, endPointDiff.x) * 180.0 / Math.PI;
         double connectorPitch = Math.atan2(endPointDiff.y, endPointDiff.multiply(1.0, 0.0, 1.0).length()) * 180.0 / Math.PI;
         PoseTransformStack msr = TransformStack.of(ms);
         carts.forEachWithContext((cart, isFirst) -> {
            CouplingRenderer.CartEndpoint cartTransform = (CouplingRenderer.CartEndpoint)transforms.get(isFirst);
            ms.pushPose();
            cartTransform.apply(ms, camera);
            attachment.light((Integer)lightValues.get(isFirst)).renderInto(ms, builder);
            msr.rotateYDegrees((float)connectorYaw - cartTransform.yaw);
            ring.light((Integer)lightValues.get(isFirst)).renderInto(ms, builder);
            ms.popPose();
         });
         int l1 = (Integer)lightValues.getFirst();
         int l2 = (Integer)lightValues.getSecond();
         int meanBlockLight = ((l1 >> 4 & 15) + (l2 >> 4 & 15)) / 2;
         int meanSkyLight = ((l1 >> 20 & 15) + (l2 >> 20 & 15)) / 2;
         ms.pushPose();
         ((PoseTransformStack)((PoseTransformStack)msr.translate(firstEndpoint.subtract(camera))).rotateYDegrees((float)connectorYaw))
            .rotateZDegrees((float)connectorPitch);
         ms.scale((float)endPointDiff.length(), 1.0F, 1.0F);
         connector.light(meanSkyLight << 20 | meanBlockLight << 4).renderInto(ms, builder);
         ms.popPose();
      }
   }

   private static CouplingRenderer.CartEndpoint getSuitableCartEndpoint(AbstractMinecart cart, Vec3 centerOfCoupling) {
      long i = (long)cart.getId() * 493286711L;
      i = i * i * 4392167121L + i * 98761L;
      double x = (double)((((float)(i >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F);
      double y = (double)((((float)(i >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F + 0.375F);
      double z = (double)((((float)(i >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F);
      float pt = AnimationTickHolder.getPartialTicks();
      double xIn = Mth.lerp((double)pt, cart.xOld, cart.getX());
      double yIn = Mth.lerp((double)pt, cart.yOld, cart.getY());
      double zIn = Mth.lerp((double)pt, cart.zOld, cart.getZ());
      float yaw = Mth.lerp(pt, cart.yRotO, cart.getYRot());
      float pitch = Mth.lerp(pt, cart.xRotO, cart.getXRot());
      float roll = (float)cart.getHurtTime() - pt;
      float rollAmplifier = cart.getDamage() - pt;
      if (rollAmplifier < 0.0F) {
         rollAmplifier = 0.0F;
      }

      roll = roll > 0.0F ? Mth.sin(roll) * roll * rollAmplifier / 10.0F * (float)cart.getHurtDir() : 0.0F;
      Vec3 positionVec = new Vec3(xIn, yIn, zIn);
      Vec3 frontVec = positionVec.add(VecHelper.rotate(new Vec3(0.5, 0.0, 0.0), (double)(180.0F - yaw), Axis.Y));
      Vec3 backVec = positionVec.add(VecHelper.rotate(new Vec3(-0.5, 0.0, 0.0), (double)(180.0F - yaw), Axis.Y));
      Vec3 railVecOfPos = cart.getPos(xIn, yIn, zIn);
      boolean flip = false;
      if (railVecOfPos != null) {
         frontVec = cart.getPosOffs(xIn, yIn, zIn, 0.3F);
         backVec = cart.getPosOffs(xIn, yIn, zIn, -0.3F);
         if (frontVec == null) {
            frontVec = railVecOfPos;
         }

         if (backVec == null) {
            backVec = railVecOfPos;
         }

         x += railVecOfPos.x;
         y += (frontVec.y + backVec.y) / 2.0;
         z += railVecOfPos.z;
         Vec3 endPointDiff = backVec.add(-frontVec.x, -frontVec.y, -frontVec.z);
         if (endPointDiff.length() != 0.0) {
            endPointDiff = endPointDiff.normalize();
            yaw = (float)(Math.atan2(endPointDiff.z, endPointDiff.x) * 180.0 / Math.PI);
            pitch = (float)(Math.atan(endPointDiff.y) * 73.0);
         }
      } else {
         x += xIn;
         y += yIn;
         z += zIn;
      }

      float offsetMagnitude = 0.8125F;
      boolean isBackFaceCloser = frontVec.distanceToSqr(centerOfCoupling) > backVec.distanceToSqr(centerOfCoupling);
      float offset = isBackFaceCloser ? -0.8125F : 0.8125F;
      return new CouplingRenderer.CartEndpoint(x, y + 0.125, z, 180.0F - yaw, -pitch, roll, offset, isBackFaceCloser);
   }

   public static void doDebugRender(Couple<MinecartController> c) {
      int yOffset = 1;
      MinecartController first = (MinecartController)c.getFirst();
      AbstractMinecart mainCart = first.cart();
      Vec3 mainCenter = mainCart.position().add(0.0, (double)yOffset, 0.0);
      Vec3 connectedCenter = ((MinecartController)c.getSecond()).cart().position().add(0.0, (double)yOffset, 0.0);
      int color = Color.mixColors(
         11268329, 15631730, (float)Mth.clamp(Math.abs((double)first.getCouplingLength(true) - connectedCenter.distanceTo(mainCenter)) * 8.0, 0.0, 1.0)
      );
      Outliner.getInstance().showLine(mainCart.getId() + "", mainCenter, connectedCenter).colored(color).lineWidth(0.125F);
      Vec3 point = mainCart.position().add(0.0, (double)yOffset, 0.0);
      Outliner.getInstance().showLine(mainCart.getId() + "_dot", point, point.add(0.0, 0.0078125, 0.0)).colored(16777215).lineWidth(0.25F);
   }

   static class CartEndpoint {
      double x;
      double y;
      double z;
      float yaw;
      float pitch;
      float roll;
      float offset;
      boolean flip;

      public CartEndpoint(double x, double y, double z, float yaw, float pitch, float roll, float offset, boolean flip) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.yaw = yaw;
         this.pitch = pitch;
         this.roll = roll;
         this.offset = offset;
         this.flip = flip;
      }

      public Vec3 apply(Vec3 vec) {
         vec = vec.add((double)this.offset, 0.0, 0.0);
         vec = VecHelper.rotate(vec, (double)this.roll, Axis.X);
         vec = VecHelper.rotate(vec, (double)this.pitch, Axis.Z);
         vec = VecHelper.rotate(vec, (double)this.yaw, Axis.Y);
         return vec.add(this.x, this.y, this.z);
      }

      public void apply(PoseStack ms, Vec3 camera) {
         ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(ms)
                        .translate(camera.scale(-1.0).add(this.x, this.y, this.z)))
                     .rotateYDegrees(this.yaw))
                  .rotateZDegrees(this.pitch))
               .rotateXDegrees(this.roll))
            .translate(this.offset, 0.0F, 0.0F)
            .rotateYDegrees(this.flip ? 180.0F : 0.0F);
      }
   }
}
