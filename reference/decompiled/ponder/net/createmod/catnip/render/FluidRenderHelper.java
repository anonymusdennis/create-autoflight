package net.createmod.catnip.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.platform.services.ModFluidHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class FluidRenderHelper<T> {
   public void renderFluidBox(
      FluidState fluid,
      float xMin,
      float yMin,
      float zMin,
      float xMax,
      float yMax,
      float zMax,
      MultiBufferSource buffer,
      PoseStack ms,
      int light,
      boolean renderBottom,
      boolean invertGasses
   ) {
      this.renderFluidBox(fluid, xMin, yMin, zMin, xMax, yMax, zMax, getFluidBuilder(buffer), ms, light, renderBottom, invertGasses);
   }

   public void renderFluidBox(
      FluidState fluid,
      float xMin,
      float yMin,
      float zMin,
      float xMax,
      float yMax,
      float zMax,
      VertexConsumer builder,
      PoseStack ms,
      int light,
      boolean renderBottom,
      boolean invertGasses
   ) {
      this.renderFluidBox(this.helper().toStack(fluid), xMin, yMin, zMin, xMax, yMax, zMax, builder, ms, light, renderBottom, invertGasses);
   }

   public void renderFluidBox(
      T fluid,
      float xMin,
      float yMin,
      float zMin,
      float xMax,
      float yMax,
      float zMax,
      MultiBufferSource buffer,
      PoseStack ms,
      int light,
      boolean renderBottom,
      boolean invertGasses
   ) {
      this.renderFluidBox(fluid, xMin, yMin, zMin, xMax, yMax, zMax, getFluidBuilder(buffer), ms, light, renderBottom, invertGasses);
   }

   public void renderFluidBox(
      T fluid,
      float xMin,
      float yMin,
      float zMin,
      float xMax,
      float yMax,
      float zMax,
      VertexConsumer builder,
      PoseStack ms,
      int light,
      boolean renderBottom,
      boolean invertGasses
   ) {
      ModFluidHelper<T> helper = this.helper();
      TextureAtlasSprite fluidTexture = helper.getStillTextureOrMissing(fluid);
      int color = helper.getColor(fluid, null, null);
      int blockLightIn = light >> 4 & 15;
      int luminosity = Math.max(blockLightIn, helper.getLuminosity(fluid));
      light = light & 15728640 | luminosity << 4;
      Vec3 center = new Vec3((double)(xMin + (xMax - xMin) / 2.0F), (double)(yMin + (yMax - yMin) / 2.0F), (double)(zMin + (zMax - zMin) / 2.0F));
      ms.pushPose();
      if (invertGasses && helper.isLighterThanAir(fluid)) {
         ms.translate(center.x, center.y, center.z);
         ms.mulPose(Axis.XP.rotationDegrees(180.0F));
         ms.translate(-center.x, -center.y, -center.z);
      }

      for (Direction side : Iterate.directions) {
         if (side != Direction.DOWN || renderBottom) {
            boolean positive = side.getAxisDirection() == AxisDirection.POSITIVE;
            if (side.getAxis().isHorizontal()) {
               if (side.getAxis() == net.minecraft.core.Direction.Axis.X) {
                  renderStillTiledFace(side, zMin, yMin, zMax, yMax, positive ? xMax : xMin, builder, ms, light, color, fluidTexture);
               } else {
                  renderStillTiledFace(side, xMin, yMin, xMax, yMax, positive ? zMax : zMin, builder, ms, light, color, fluidTexture);
               }
            } else {
               renderStillTiledFace(side, xMin, zMin, xMax, zMax, positive ? yMax : yMin, builder, ms, light, color, fluidTexture);
            }
         }
      }

      ms.popPose();
   }

   public static VertexConsumer getFluidBuilder(MultiBufferSource buffer) {
      return buffer.getBuffer(PonderRenderTypes.fluid());
   }

   public static void renderStillTiledFace(
      Direction dir,
      float left,
      float down,
      float right,
      float up,
      float depth,
      VertexConsumer builder,
      PoseStack ms,
      int light,
      int color,
      TextureAtlasSprite texture
   ) {
      renderTiledFace(dir, left, down, right, up, depth, builder, ms, light, color, texture, 1.0F);
   }

   public static void renderTiledFace(
      Direction dir,
      float left,
      float down,
      float right,
      float up,
      float depth,
      VertexConsumer builder,
      PoseStack ms,
      int light,
      int color,
      TextureAtlasSprite texture,
      float textureScale
   ) {
      boolean positive = dir.getAxisDirection() == AxisDirection.POSITIVE;
      boolean horizontal = dir.getAxis().isHorizontal();
      boolean x = dir.getAxis() == net.minecraft.core.Direction.Axis.X;
      float shrink = texture.uvShrinkRatio() * 0.25F * textureScale;
      float centerU = texture.getU0() + (texture.getU1() - texture.getU0()) * 0.5F * textureScale;
      float centerV = texture.getV0() + (texture.getV1() - texture.getV0()) * 0.5F * textureScale;
      float x2 = 0.0F;
      float y2 = 0.0F;
      float x1 = left;

      while (x1 < right) {
         float f = (float)Mth.floor(x1);
         x2 = Math.min(f + 1.0F, right);
         float u1;
         float u2;
         if (dir != Direction.NORTH && dir != Direction.EAST) {
            u1 = texture.getU((x1 - f) * textureScale);
            u2 = texture.getU((x2 - f) * textureScale);
         } else {
            f = (float)Mth.ceil(x2);
            u1 = texture.getU((f - x2) * textureScale);
            u2 = texture.getU((f - x1) * textureScale);
         }

         u1 = Mth.lerp(shrink, u1, centerU);
         u2 = Mth.lerp(shrink, u2, centerU);
         float y1 = down;

         while (y1 < up) {
            f = (float)Mth.floor(y1);
            y2 = Math.min(f + 1.0F, up);
            float v1;
            float v2;
            if (dir == Direction.UP) {
               v1 = texture.getV((y1 - f) * textureScale);
               v2 = texture.getV((y2 - f) * textureScale);
            } else {
               f = (float)Mth.ceil(y2);
               v1 = texture.getV((f - y2) * textureScale);
               v2 = texture.getV((f - y1) * textureScale);
            }

            v1 = Mth.lerp(shrink, v1, centerV);
            v2 = Mth.lerp(shrink, v2, centerV);
            if (horizontal) {
               if (x) {
                  putVertex(builder, ms, depth, y2, positive ? x2 : x1, color, u1, v1, dir, light);
                  putVertex(builder, ms, depth, y1, positive ? x2 : x1, color, u1, v2, dir, light);
                  putVertex(builder, ms, depth, y1, positive ? x1 : x2, color, u2, v2, dir, light);
                  putVertex(builder, ms, depth, y2, positive ? x1 : x2, color, u2, v1, dir, light);
               } else {
                  putVertex(builder, ms, positive ? x1 : x2, y2, depth, color, u1, v1, dir, light);
                  putVertex(builder, ms, positive ? x1 : x2, y1, depth, color, u1, v2, dir, light);
                  putVertex(builder, ms, positive ? x2 : x1, y1, depth, color, u2, v2, dir, light);
                  putVertex(builder, ms, positive ? x2 : x1, y2, depth, color, u2, v1, dir, light);
               }
            } else {
               putVertex(builder, ms, x1, depth, positive ? y1 : y2, color, u1, v1, dir, light);
               putVertex(builder, ms, x1, depth, positive ? y2 : y1, color, u1, v2, dir, light);
               putVertex(builder, ms, x2, depth, positive ? y2 : y1, color, u2, v2, dir, light);
               putVertex(builder, ms, x2, depth, positive ? y1 : y2, color, u2, v1, dir, light);
            }

            y1 = y2;
         }

         x1 = x2;
      }
   }

   private static void putVertex(VertexConsumer builder, PoseStack ms, float x, float y, float z, int color, float u, float v, Direction face, int light) {
      Vec3i normal = face.getNormal();
      Pose peek = ms.last();
      int a = color >> 24 & 0xFF;
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      builder.addVertex(peek.pose(), x, y, z)
         .setColor(r, g, b, a)
         .setUv(u, v)
         .setLight(light)
         .setNormal(peek.copy(), (float)normal.getX(), (float)normal.getY(), (float)normal.getZ());
   }

   private ModFluidHelper<T> helper() {
      return (ModFluidHelper<T>)CatnipServices.FLUID_HELPER;
   }
}
