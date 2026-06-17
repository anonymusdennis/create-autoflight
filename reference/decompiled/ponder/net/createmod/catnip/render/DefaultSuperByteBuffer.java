package net.createmod.catnip.render;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Deprecated(
   forRemoval = true
)
public class DefaultSuperByteBuffer implements SuperByteBuffer {
   protected ByteBuffer template;
   protected int formatSize;
   protected PoseStack transforms;
   protected boolean shouldColor;
   protected int r;
   protected int g;
   protected int b;
   protected int a;
   protected boolean disableDiffuse;
   @Nullable
   protected SuperByteBuffer.SpriteShiftFunc spriteShiftFunc;
   protected boolean hasOverlay;
   protected int overlay = OverlayTexture.NO_OVERLAY;
   protected boolean useWorldLight;
   @Nullable
   protected Matrix4f lightTransform;
   protected boolean hasCustomLight;
   protected int packedLightCoordinates;
   protected boolean hybridLight;
   protected boolean fullNormalTransform;
   protected static final Long2IntMap WORLD_LIGHT_CACHE = new Long2IntOpenHashMap();
   private final SuperByteBuffer.ShiftOutput shiftOutput = new SuperByteBuffer.ShiftOutput();

   public DefaultSuperByteBuffer(MeshData data) {
      ByteBuffer rendered = data.vertexBuffer();
      DrawState drawState = data.drawState();
      rendered.order(ByteOrder.nativeOrder());
      drawState.format().getVertexSize();
      this.formatSize = drawState.format().getVertexSize();
      int size = drawState.vertexCount() * this.formatSize;
      this.template = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
      this.template.order(rendered.order());
      this.template.limit(rendered.limit());
      this.template.put(rendered);
      this.transforms = new PoseStack();
      this.transforms.pushPose();
   }

   @Override
   public void renderInto(PoseStack ms, VertexConsumer consumer) {
      if (!this.isEmpty()) {
         Matrix4f modelMatrix = new Matrix4f(ms.last().pose());
         Matrix4f localTransforms = this.transforms.last().pose();
         modelMatrix.mul(localTransforms);
         Matrix3f normalMatrix;
         if (this.fullNormalTransform) {
            normalMatrix = new Matrix3f(ms.last().normal());
            normalMatrix.mul(this.transforms.last().normal());
         } else {
            normalMatrix = new Matrix3f(this.transforms.last().normal());
         }

         for (int i = 0; i < this.vertexCount(); i++) {
            float x = this.getX(i);
            float y = this.getY(i);
            float z = this.getZ(i);
            float normalX = (float)this.getNX(i);
            float normalY = (float)this.getNY(i);
            float normalZ = (float)this.getNZ(i);
            Vector4f pos = new Vector4f(x, y, z, 1.0F);
            Vector3f normal = new Vector3f(normalX, normalY, normalZ);
            Vector4f lightPos = new Vector4f(x, y, z, 1.0F);
            pos.mul(modelMatrix);
            normal.mul(normalMatrix);
            lightPos.mul(localTransforms);
            consumer.addVertex(pos.x(), pos.y(), pos.z());
            byte r;
            byte g;
            byte b;
            byte a;
            if (this.shouldColor) {
               r = (byte)this.r;
               g = (byte)this.g;
               b = (byte)this.b;
               a = (byte)this.a;
            } else {
               r = this.getR(i);
               g = this.getG(i);
               b = this.getB(i);
               a = this.getA(i);
            }

            if (this.disableDiffuse) {
               consumer.setColor(r, g, b, a);
            } else {
               consumer.setColor(r, g, b, a);
            }

            float u = this.getU(i);
            float v = this.getV(i);
            if (this.spriteShiftFunc != null) {
               this.spriteShiftFunc.shift(u, v, this.shiftOutput);
               u = this.shiftOutput.u;
               v = this.shiftOutput.v;
            }

            consumer.setUv(u, v);
            int light;
            if (this.useWorldLight) {
               lightPos.set((x - 0.5F) * 15.0F / 16.0F + 0.5F, (y - 0.5F) * 15.0F / 16.0F + 0.5F, (z - 0.5F) * 15.0F / 16.0F + 0.5F, 1.0F);
               lightPos.mul(localTransforms);
               if (this.lightTransform != null) {
                  lightPos.mul(this.lightTransform);
               }

               light = getLight(Minecraft.getInstance().level, lightPos);
               if (this.hasCustomLight) {
                  light = SuperByteBuffer.maxLight(light, this.packedLightCoordinates);
               }
            } else if (this.hasCustomLight) {
               light = this.packedLightCoordinates;
            } else {
               light = this.getLight(i);
            }

            if (this.hybridLight) {
               consumer.setLight(SuperByteBuffer.maxLight(light, this.getLight(i)));
            } else {
               consumer.setLight(light);
            }

            consumer.setNormal(normal.x(), normal.y(), normal.z());
         }

         this.reset();
      }
   }

   public DefaultSuperByteBuffer reset() {
      while (!this.transforms.clear()) {
         this.transforms.popPose();
      }

      this.transforms.pushPose();
      this.shouldColor = false;
      this.r = 0;
      this.g = 0;
      this.b = 0;
      this.a = 0;
      this.disableDiffuse = false;
      this.spriteShiftFunc = null;
      this.hasOverlay = false;
      this.overlay = OverlayTexture.NO_OVERLAY;
      this.useWorldLight = false;
      this.lightTransform = null;
      this.hasCustomLight = false;
      this.packedLightCoordinates = 0;
      this.hybridLight = false;
      this.fullNormalTransform = false;
      WORLD_LIGHT_CACHE.clear();
      return this;
   }

   @Override
   public boolean isEmpty() {
      return this.template.limit() == 0;
   }

   @Override
   public PoseStack getTransforms() {
      return this.transforms;
   }

   public DefaultSuperByteBuffer translate(float x, float y, float z) {
      this.transforms.translate(x, y, z);
      return this;
   }

   public DefaultSuperByteBuffer translate(double x, double y, double z) {
      this.transforms.translate(x, y, z);
      return this;
   }

   public DefaultSuperByteBuffer scale(float factorX, float factorY, float factorZ) {
      this.transforms.scale(factorX, factorY, factorZ);
      return this;
   }

   public DefaultSuperByteBuffer pushPose() {
      this.transforms.pushPose();
      return this;
   }

   public DefaultSuperByteBuffer popPose() {
      this.transforms.popPose();
      return this;
   }

   public DefaultSuperByteBuffer mulPose(Matrix4fc matrix4fc) {
      this.transforms.last().pose().mul(matrix4fc);
      return this;
   }

   public DefaultSuperByteBuffer mulNormal(Matrix3fc matrix3fc) {
      this.transforms.last().normal().mul(matrix3fc);
      return this;
   }

   public DefaultSuperByteBuffer transform(PoseStack ms) {
      this.transforms.last().pose().mul(ms.last().pose());
      this.transforms.last().normal().mul(ms.last().normal());
      return this;
   }

   public DefaultSuperByteBuffer color(int color) {
      this.shouldColor = true;
      this.r = color >> 16 & 0xFF;
      this.g = color >> 8 & 0xFF;
      this.b = color & 0xFF;
      this.a = 255;
      return this;
   }

   public DefaultSuperByteBuffer color(int r, int g, int b, int a) {
      this.shouldColor = true;
      this.r = r;
      this.g = g;
      this.b = b;
      this.a = a;
      return this;
   }

   public DefaultSuperByteBuffer disableDiffuse() {
      this.disableDiffuse = true;
      return this;
   }

   public DefaultSuperByteBuffer shiftUV(SpriteShiftEntry entry) {
      this.spriteShiftFunc = (u, v, output) -> output.accept(entry.getTargetU(u), entry.getTargetV(v));
      return this;
   }

   public DefaultSuperByteBuffer shiftUVScrolling(SpriteShiftEntry entry, float scrollU, float scrollV) {
      this.spriteShiftFunc = (u, v, output) -> {
         float targetU = u - entry.getOriginal().getU0() + entry.getTarget().getU0() + scrollU;
         float targetV = v - entry.getOriginal().getV0() + entry.getTarget().getV0() + scrollV;
         output.accept(targetU, targetV);
      };
      return this;
   }

   public DefaultSuperByteBuffer shiftUVtoSheet(SpriteShiftEntry entry, float uTarget, float vTarget, int sheetSize) {
      this.spriteShiftFunc = (u, v, output) -> {
         float targetU = entry.getTarget().getU(SpriteShiftEntry.getUnInterpolatedU(entry.getOriginal(), u) / (float)sheetSize + uTarget);
         float targetV = entry.getTarget().getV(SpriteShiftEntry.getUnInterpolatedV(entry.getOriginal(), v) / (float)sheetSize + vTarget);
         output.accept(targetU, targetV);
      };
      return this;
   }

   public DefaultSuperByteBuffer overlay(int overlay) {
      this.hasOverlay = true;
      this.overlay = overlay;
      return this;
   }

   public DefaultSuperByteBuffer useLevelLight(BlockAndTintGetter level) {
      return this;
   }

   public DefaultSuperByteBuffer useLevelLight(BlockAndTintGetter level, Matrix4f lightTransform) {
      return this;
   }

   public DefaultSuperByteBuffer light(int packedLight) {
      this.hasCustomLight = true;
      this.packedLightCoordinates = packedLight;
      return this;
   }

   protected int vertexCount() {
      return this.template.limit() / this.formatSize;
   }

   protected int getBufferPosition(int vertexIndex) {
      return vertexIndex * this.formatSize;
   }

   protected float getX(int index) {
      return this.template.getFloat(this.getBufferPosition(index));
   }

   protected float getY(int index) {
      return this.template.getFloat(this.getBufferPosition(index) + 4);
   }

   protected float getZ(int index) {
      return this.template.getFloat(this.getBufferPosition(index) + 8);
   }

   protected byte getR(int index) {
      return this.template.get(this.getBufferPosition(index) + 12);
   }

   protected byte getG(int index) {
      return this.template.get(this.getBufferPosition(index) + 13);
   }

   protected byte getB(int index) {
      return this.template.get(this.getBufferPosition(index) + 14);
   }

   protected byte getA(int index) {
      return this.template.get(this.getBufferPosition(index) + 15);
   }

   protected float getU(int index) {
      return this.template.getFloat(this.getBufferPosition(index) + 16);
   }

   protected float getV(int index) {
      return this.template.getFloat(this.getBufferPosition(index) + 20);
   }

   protected int getLight(int index) {
      return this.template.getInt(this.getBufferPosition(index) + 24);
   }

   protected byte getNX(int index) {
      return this.template.get(this.getBufferPosition(index) + 28);
   }

   protected byte getNY(int index) {
      return this.template.get(this.getBufferPosition(index) + 29);
   }

   protected byte getNZ(int index) {
      return this.template.get(this.getBufferPosition(index) + 30);
   }

   private static int getLight(Level world, Vector4f lightPos) {
      BlockPos pos = BlockPos.containing((double)lightPos.x(), (double)lightPos.y(), (double)lightPos.z());
      return WORLD_LIGHT_CACHE.computeIfAbsent(pos.asLong(), $ -> LevelRenderer.getLightColor(world, pos));
   }

   public SuperByteBuffer rotate(Quaternionfc quaternionfc) {
      return null;
   }
}
