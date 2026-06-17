package net.createmod.catnip.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.engine_room.flywheel.lib.util.ShadersModHelper;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import javax.annotation.ParametersAreNonnullByDefault;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.mixin.client.accessor.RenderSystemAccessor;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ShadeSeparatingSuperByteBuffer implements SuperByteBuffer {
   private static final Long2IntMap WORLD_LIGHT_CACHE = new Long2IntOpenHashMap();
   private final TemplateMesh template;
   private final int[] shadeSwapVertices;
   private final PoseStack transforms = new PoseStack();
   private float r;
   private float g;
   private float b;
   private float a;
   private boolean disableDiffuse;
   @Nullable
   private SuperByteBuffer.SpriteShiftFunc spriteShiftFunc;
   private boolean hasCustomOverlay;
   private int overlay;
   private boolean hasCustomLight;
   private int packedLight;
   private boolean useLevelLight;
   @Nullable
   private BlockAndTintGetter levelWithLight;
   @Nullable
   private Matrix4f lightTransform;
   private final boolean invertFakeDiffuseNormal;
   private final Matrix4f modelMat = new Matrix4f();
   private final Matrix3f normalMat = new Matrix3f();
   private final Vector4f pos = new Vector4f();
   private final Vector3f normal = new Vector3f();
   private final Vector3f lightDir0 = new Vector3f();
   private final Vector3f lightDir1 = new Vector3f();
   private final SuperByteBuffer.ShiftOutput shiftOutput = new SuperByteBuffer.ShiftOutput();
   private final Vector4f lightPos = new Vector4f();

   public ShadeSeparatingSuperByteBuffer(TemplateMesh template, int[] shadeSwapVertices, boolean invertFakeDiffuseNormal) {
      this.template = template;
      this.shadeSwapVertices = shadeSwapVertices;
      this.invertFakeDiffuseNormal = invertFakeDiffuseNormal;
      this.reset();
   }

   public ShadeSeparatingSuperByteBuffer(TemplateMesh template, int[] shadeSwapVertices) {
      this(template, shadeSwapVertices, false);
   }

   public ShadeSeparatingSuperByteBuffer(TemplateMesh template) {
      this(template, new int[0]);
   }

   @Override
   public void renderInto(PoseStack input, VertexConsumer builder) {
      if (!this.isEmpty()) {
         if (this.useLevelLight) {
            WORLD_LIGHT_CACHE.clear();
         }

         Matrix4f modelMat = this.modelMat.set(input.last().pose());
         Matrix4f localTransforms = this.transforms.last().pose();
         modelMat.mul(localTransforms);
         Matrix3f normalMat = this.normalMat.set(input.last().normal());
         Matrix3f localNormalTransforms = this.transforms.last().normal();
         normalMat.mul(localNormalTransforms);
         Vector4f pos = this.pos;
         Vector3f normal = this.normal;
         SuperByteBuffer.ShiftOutput shiftOutput = this.shiftOutput;
         Vector3f lightDir0 = this.lightDir0;
         Vector3f lightDir1 = this.lightDir1;
         Vector4f lightPos = this.lightPos;
         boolean applyDiffuse = !this.disableDiffuse && !ShadersModHelper.isShaderPackInUse();
         boolean shaded = true;
         int shadeSwapIndex = 0;
         int nextShadeSwapVertex = shadeSwapIndex < this.shadeSwapVertices.length ? this.shadeSwapVertices[shadeSwapIndex] : -1;
         float unshadedDiffuse = 1.0F;
         if (applyDiffuse) {
            lightDir0.set(RenderSystemAccessor.catnip$getShaderLightDirections()[0]).normalize();
            lightDir1.set(RenderSystemAccessor.catnip$getShaderLightDirections()[1]).normalize();
            if (this.shadeSwapVertices.length > 0) {
               normal.set(0.0F, this.invertFakeDiffuseNormal ? -1.0F : 1.0F, 0.0F);
               unshadedDiffuse = calculateDiffuse(normal, lightDir0, lightDir1);
            }
         }

         int vertexCount = this.template.vertexCount();

         for (int i = 0; i < vertexCount; i++) {
            if (i == nextShadeSwapVertex) {
               shaded = !shaded;
               shadeSwapIndex++;
               nextShadeSwapVertex = shadeSwapIndex < this.shadeSwapVertices.length ? this.shadeSwapVertices[shadeSwapIndex] : -1;
            }

            float x = this.template.x(i);
            float y = this.template.y(i);
            float z = this.template.z(i);
            pos.set(x, y, z, 1.0F);
            pos.mul(modelMat);
            int packedNormal = this.template.normal(i);
            float normalX = (float)((byte)(packedNormal & 0xFF)) / 127.0F;
            float normalY = (float)((byte)(packedNormal >>> 8 & 0xFF)) / 127.0F;
            float normalZ = (float)((byte)(packedNormal >>> 16 & 0xFF)) / 127.0F;
            normal.set(normalX, normalY, normalZ);
            normal.mul(normalMat);
            int color = this.template.color(i);
            float r = (float)(color & 0xFF) / 255.0F * this.r;
            float g = (float)(color >>> 8 & 0xFF) / 255.0F * this.g;
            float b = (float)(color >>> 16 & 0xFF) / 255.0F * this.b;
            float a = (float)(color >>> 24 & 0xFF) / 255.0F * this.a;
            if (applyDiffuse) {
               float diffuse = shaded ? calculateDiffuse(normal, lightDir0, lightDir1) : unshadedDiffuse;
               r *= diffuse;
               g *= diffuse;
               b *= diffuse;
            }

            float u = this.template.u(i);
            float v = this.template.v(i);
            if (this.spriteShiftFunc != null) {
               this.spriteShiftFunc.shift(u, v, shiftOutput);
               u = shiftOutput.u;
               v = shiftOutput.v;
            }

            int overlay;
            if (this.hasCustomOverlay) {
               overlay = this.overlay;
            } else {
               overlay = this.template.overlay(i);
            }

            int light = this.template.light(i);
            if (this.hasCustomLight) {
               light = SuperByteBuffer.maxLight(light, this.packedLight);
            }

            if (this.useLevelLight) {
               lightPos.set((x - 0.5F) * 15.0F / 16.0F + 0.5F, (y - 0.5F) * 15.0F / 16.0F + 0.5F, (z - 0.5F) * 15.0F / 16.0F + 0.5F, 1.0F);
               lightPos.mul(localTransforms);
               if (this.lightTransform != null) {
                  lightPos.mul(this.lightTransform);
               }

               light = SuperByteBuffer.maxLight(light, getLight(this.levelWithLight, lightPos));
            }

            builder.addVertex(pos.x(), pos.y(), pos.z())
               .setColor(r, g, b, a)
               .setUv(u, v)
               .setOverlay(overlay)
               .setLight(light)
               .setNormal(normal.x(), normal.y(), normal.z());
         }

         this.reset();
      }
   }

   @Override
   public SuperByteBuffer reset() {
      while (!this.transforms.clear()) {
         this.transforms.popPose();
      }

      this.transforms.pushPose();
      this.r = 1.0F;
      this.g = 1.0F;
      this.b = 1.0F;
      this.a = 1.0F;
      this.disableDiffuse = false;
      this.spriteShiftFunc = null;
      this.hasCustomOverlay = false;
      this.overlay = OverlayTexture.NO_OVERLAY;
      this.hasCustomLight = false;
      this.packedLight = 0;
      this.useLevelLight = false;
      this.levelWithLight = null;
      this.lightTransform = null;
      return this;
   }

   @Override
   public boolean isEmpty() {
      return this.template.isEmpty();
   }

   @Override
   public PoseStack getTransforms() {
      return this.transforms;
   }

   public SuperByteBuffer scale(float factorX, float factorY, float factorZ) {
      this.transforms.scale(factorX, factorY, factorZ);
      return this;
   }

   public SuperByteBuffer rotate(Quaternionfc quaternion) {
      Pose last = this.transforms.last();
      last.pose().rotate(quaternion);
      last.normal().rotate(quaternion);
      return this;
   }

   public SuperByteBuffer translate(float x, float y, float z) {
      this.transforms.translate(x, y, z);
      return this;
   }

   public SuperByteBuffer mulPose(Matrix4fc pose) {
      this.transforms.last().pose().mul(pose);
      return this;
   }

   public SuperByteBuffer mulNormal(Matrix3fc normal) {
      this.transforms.last().normal().mul(normal);
      return this;
   }

   public SuperByteBuffer pushPose() {
      this.transforms.pushPose();
      return this;
   }

   public SuperByteBuffer popPose() {
      this.transforms.popPose();
      return this;
   }

   public SuperByteBuffer color(float r, float g, float b, float a) {
      this.r = r;
      this.g = g;
      this.b = b;
      this.a = a;
      return this;
   }

   @Override
   public SuperByteBuffer color(int r, int g, int b, int a) {
      this.color((float)r / 255.0F, (float)g / 255.0F, (float)b / 255.0F, (float)a / 255.0F);
      return this;
   }

   @Override
   public SuperByteBuffer color(int color) {
      this.color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255);
      return this;
   }

   @Override
   public SuperByteBuffer color(Color c) {
      return this.color(c.getRGB());
   }

   @Override
   public SuperByteBuffer disableDiffuse() {
      this.disableDiffuse = true;
      return this;
   }

   @Override
   public SuperByteBuffer shiftUV(SpriteShiftEntry entry) {
      this.spriteShiftFunc = (u, v, output) -> output.accept(entry.getTargetU(u), entry.getTargetV(v));
      return this;
   }

   @Override
   public SuperByteBuffer shiftUVScrolling(SpriteShiftEntry entry, float scrollV) {
      return this.shiftUVScrolling(entry, 0.0F, scrollV);
   }

   @Override
   public SuperByteBuffer shiftUVScrolling(SpriteShiftEntry entry, float scrollU, float scrollV) {
      this.spriteShiftFunc = (u, v, output) -> {
         float targetU = u - entry.getOriginal().getU0() + entry.getTarget().getU0() + scrollU;
         float targetV = v - entry.getOriginal().getV0() + entry.getTarget().getV0() + scrollV;
         output.accept(targetU, targetV);
      };
      return this;
   }

   @Override
   public SuperByteBuffer shiftUVtoSheet(SpriteShiftEntry entry, float uTarget, float vTarget, int sheetSize) {
      this.spriteShiftFunc = (u, v, output) -> {
         float targetU = entry.getTarget().getU(SpriteShiftEntry.getUnInterpolatedU(entry.getOriginal(), u) / (float)sheetSize + uTarget);
         float targetV = entry.getTarget().getV(SpriteShiftEntry.getUnInterpolatedV(entry.getOriginal(), v) / (float)sheetSize + vTarget);
         output.accept(targetU, targetV);
      };
      return this;
   }

   @Override
   public SuperByteBuffer overlay(int overlay) {
      this.hasCustomOverlay = true;
      this.overlay = overlay;
      return this;
   }

   @Override
   public SuperByteBuffer light(int packedLight) {
      this.hasCustomLight = true;
      this.packedLight = packedLight;
      return this;
   }

   @Override
   public SuperByteBuffer useLevelLight(BlockAndTintGetter level) {
      this.useLevelLight = true;
      this.levelWithLight = level;
      return this;
   }

   @Override
   public SuperByteBuffer useLevelLight(BlockAndTintGetter level, Matrix4f lightTransform) {
      this.useLevelLight = true;
      this.levelWithLight = level;
      this.lightTransform = lightTransform;
      return this;
   }

   private static float calculateDiffuse(Vector3fc normal, Vector3fc lightDir0, Vector3fc lightDir1) {
      float light0 = Math.max(0.0F, lightDir0.dot(normal));
      float light1 = Math.max(0.0F, lightDir1.dot(normal));
      return Math.min(1.0F, (light0 + light1) * 0.6F + 0.4F);
   }

   private static int getLight(BlockAndTintGetter world, Vector4f lightPos) {
      BlockPos pos = BlockPos.containing((double)lightPos.x(), (double)lightPos.y(), (double)lightPos.z());
      return WORLD_LIGHT_CACHE.computeIfAbsent(pos.asLong(), $ -> LevelRenderer.getLightColor(world, pos));
   }
}
