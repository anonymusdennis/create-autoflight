package com.simibubi.create.content.fluids;

import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.QuadMesh;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public class FluidMesh {
   private static final RendererReloadCache<TextureAtlasSprite, Model> STREAM = new RendererReloadCache(
      sprite -> new SingleMeshModel(new FluidMesh.FluidStreamMesh(sprite), material(sprite))
   );
   private static final RendererReloadCache<FluidMesh.SurfaceKey, Model> SURFACE = new RendererReloadCache(
      sprite -> new SingleMeshModel(new FluidMesh.FluidSurfaceMesh(sprite.texture(), sprite.width()), material(sprite.texture()))
   );
   public static final float PIPE_RADIUS = 0.1875F;

   public static Model stream(TextureAtlasSprite sprite) {
      return (Model)STREAM.get(sprite);
   }

   public static Model surface(TextureAtlasSprite sprite, float width) {
      return (Model)SURFACE.get(new FluidMesh.SurfaceKey(sprite, width));
   }

   private static SimpleMaterial material(TextureAtlasSprite sprite) {
      return SimpleMaterial.builder()
         .cardinalLightingMode(CardinalLightingMode.OFF)
         .texture(sprite.atlasLocation())
         .transparency(Transparency.ORDER_INDEPENDENT)
         .build();
   }

   public static record FluidStreamMesh(TextureAtlasSprite texture) implements QuadMesh {
      public int vertexCount() {
         return 32;
      }

      public void write(MutableVertexList vertexList) {
         for (int i = 0; i < this.vertexCount(); i++) {
            vertexList.r(i, 1.0F);
            vertexList.g(i, 1.0F);
            vertexList.b(i, 1.0F);
            vertexList.a(i, 1.0F);
            vertexList.light(i, 0);
            vertexList.overlay(i, OverlayTexture.NO_OVERLAY);
            vertexList.v(i, 0.0F);
         }

         float textureScale = 0.03125F;
         float shrink = this.texture.uvShrinkRatio() * 0.25F * textureScale;
         float centerU = this.texture.getU0() + (this.texture.getU1() - this.texture.getU0()) * 0.5F;
         float radius = 0.1875F;
         float left = -radius;
         float right = radius;
         int vertex = 0;

         for (Direction horizontalDirection : Iterate.horizontalDirections) {
            float x1 = left;

            while (x1 < right) {
               float x1floor = (float)Mth.floor(x1);
               float x2 = Math.min(x1floor + 1.0F, right);
               float u1 = this.texture.getU((x1 - x1floor) * 16.0F * textureScale);
               float u2 = this.texture.getU((x2 - x1floor) * 16.0F * textureScale);
               u1 = Mth.lerp(shrink, u1, centerU);
               u2 = Mth.lerp(shrink, u2, centerU);
               putQuad(vertexList, vertex, horizontalDirection, radius, x1, x2, u1, u2);
               vertex += 4;
               x1 = x2;
            }
         }
      }

      private static void putQuad(MutableVertexList vertexList, int i, Direction horizontal, float radius, float p0, float p1, float u0, float u1) {
         float xStart;
         float xEnd;
         float zStart;
         float zEnd;
         switch (horizontal) {
            case NORTH:
               xStart = p1;
               xEnd = p0;
               zStart = zEnd = -radius;
               break;
            case SOUTH:
               xStart = p0;
               xEnd = p1;
               zEnd = radius;
               zStart = radius;
               break;
            case WEST:
               zStart = p0;
               zEnd = p1;
               xStart = xEnd = -radius;
               break;
            case EAST:
               zStart = p1;
               zEnd = p0;
               xEnd = radius;
               xStart = radius;
               break;
            default:
               throw new IllegalStateException("Unexpected value: " + horizontal);
         }

         vertexList.x(i, xStart);
         vertexList.y(i, 1.0F);
         vertexList.z(i, zStart);
         vertexList.u(i, u0);
         vertexList.x(i + 1, xStart);
         vertexList.y(i + 1, 0.0F);
         vertexList.z(i + 1, zStart);
         vertexList.u(i + 1, u0);
         vertexList.x(i + 2, xEnd);
         vertexList.y(i + 2, 0.0F);
         vertexList.z(i + 2, zEnd);
         vertexList.u(i + 2, u1);
         vertexList.x(i + 3, xEnd);
         vertexList.y(i + 3, 1.0F);
         vertexList.z(i + 3, zEnd);
         vertexList.u(i + 3, u1);

         for (int j = 0; j < 4; j++) {
            vertexList.normalX(i + j, (float)horizontal.getStepX());
            vertexList.normalY(i + j, (float)horizontal.getStepY());
            vertexList.normalZ(i + j, (float)horizontal.getStepZ());
         }
      }

      public Vector4fc boundingSphere() {
         return new Vector4f(0.0F, 0.5F, 0.0F, 1.0F);
      }
   }

   public static record FluidSurfaceMesh(TextureAtlasSprite texture, float width) implements QuadMesh {
      public int vertexCount() {
         int quadWidth = Mth.ceil(this.width) - Mth.floor(-this.width);
         return 4 * quadWidth * quadWidth;
      }

      public void write(MutableVertexList vertexList) {
         for (int i = 0; i < this.vertexCount(); i++) {
            vertexList.r(i, 1.0F);
            vertexList.g(i, 1.0F);
            vertexList.b(i, 1.0F);
            vertexList.a(i, 1.0F);
            vertexList.light(i, 0);
            vertexList.overlay(i, OverlayTexture.NO_OVERLAY);
            vertexList.normalX(i, 0.0F);
            vertexList.normalY(i, 1.0F);
            vertexList.normalZ(i, 0.0F);
            vertexList.y(i, 0.0F);
         }

         float textureScale = 0.0625F;
         float left = -this.width;
         float right = this.width;
         float down = -this.width;
         float up = this.width;
         int vertex = 0;
         float shrink = this.texture.uvShrinkRatio() * 0.25F * textureScale;
         float centerU = this.texture.getU0() + (this.texture.getU1() - this.texture.getU0()) * 0.5F;
         float centerV = this.texture.getV0() + (this.texture.getV1() - this.texture.getV0()) * 0.5F;
         float x1 = left;

         while (x1 < right) {
            float x1floor = (float)Mth.floor(x1);
            float x2 = Math.min(x1floor + 1.0F, right);
            float u1 = this.texture.getU((x1 - x1floor) * 16.0F * textureScale);
            float u2 = this.texture.getU((x2 - x1floor) * 16.0F * textureScale);
            u1 = Mth.lerp(shrink, u1, centerU);
            u2 = Mth.lerp(shrink, u2, centerU);
            float y1 = down;

            while (y1 < up) {
               float y1floor = (float)Mth.floor(y1);
               float y2 = Math.min(y1floor + 1.0F, up);
               float v1 = this.texture.getV((y1 - y1floor) * 16.0F * textureScale);
               float v2 = this.texture.getV((y2 - y1floor) * 16.0F * textureScale);
               v1 = Mth.lerp(shrink, v1, centerV);
               v2 = Mth.lerp(shrink, v2, centerV);
               vertexList.x(vertex, x1);
               vertexList.z(vertex, y1);
               vertexList.u(vertex, u1);
               vertexList.v(vertex, v1);
               vertexList.x(vertex + 1, x1);
               vertexList.z(vertex + 1, y2);
               vertexList.u(vertex + 1, u1);
               vertexList.v(vertex + 1, v2);
               vertexList.x(vertex + 2, x2);
               vertexList.z(vertex + 2, y2);
               vertexList.u(vertex + 2, u2);
               vertexList.v(vertex + 2, v2);
               vertexList.x(vertex + 3, x2);
               vertexList.z(vertex + 3, y1);
               vertexList.u(vertex + 3, u2);
               vertexList.v(vertex + 3, v1);
               vertex += 4;
               y1 = y2;
            }

            x1 = x2;
         }
      }

      public Vector4fc boundingSphere() {
         return new Vector4f(0.0F, 0.0F, 0.0F, this.width / Mth.SQRT_OF_TWO);
      }
   }

   private static record SurfaceKey(TextureAtlasSprite texture, float width) {
   }
}
