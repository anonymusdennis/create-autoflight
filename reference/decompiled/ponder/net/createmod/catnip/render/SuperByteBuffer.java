package net.createmod.catnip.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import org.joml.Matrix4f;

public interface SuperByteBuffer extends TransformStack<SuperByteBuffer> {
   static int maxLight(int packedLight1, int packedLight2) {
      int blockLight1 = LightTexture.block(packedLight1);
      int skyLight1 = LightTexture.sky(packedLight1);
      int blockLight2 = LightTexture.block(packedLight2);
      int skyLight2 = LightTexture.sky(packedLight2);
      return LightTexture.pack(Math.max(blockLight1, blockLight2), Math.max(skyLight1, skyLight2));
   }

   void renderInto(PoseStack var1, VertexConsumer var2);

   boolean isEmpty();

   PoseStack getTransforms();

   <Self extends SuperByteBuffer> Self reset();

   <Self extends SuperByteBuffer> Self color(int var1);

   <Self extends SuperByteBuffer> Self color(int var1, int var2, int var3, int var4);

   <Self extends SuperByteBuffer> Self disableDiffuse();

   <Self extends SuperByteBuffer> Self shiftUV(SpriteShiftEntry var1);

   <Self extends SuperByteBuffer> Self shiftUVScrolling(SpriteShiftEntry var1, float var2, float var3);

   <Self extends SuperByteBuffer> Self shiftUVtoSheet(SpriteShiftEntry var1, float var2, float var3, int var4);

   <Self extends SuperByteBuffer> Self overlay(int var1);

   <Self extends SuperByteBuffer> Self light(int var1);

   <Self extends SuperByteBuffer> Self useLevelLight(BlockAndTintGetter var1);

   <Self extends SuperByteBuffer> Self useLevelLight(BlockAndTintGetter var1, Matrix4f var2);

   default void delete() {
   }

   default <Self extends SuperByteBuffer> Self rotate(Axis axis, float radians) {
      return (Self)this.rotate(radians, axis);
   }

   default <Self extends SuperByteBuffer> Self color(Color color) {
      return this.color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
   }

   default <Self extends SuperByteBuffer> Self shiftUVScrolling(SpriteShiftEntry entry, float scrollV) {
      return this.shiftUVScrolling(entry, 0.0F, scrollV);
   }

   public static class ShiftOutput implements SuperByteBuffer.SpriteShiftFunc.Output {
      public float u;
      public float v;

      @Override
      public void accept(float u, float v) {
         this.u = u;
         this.v = v;
      }
   }

   @FunctionalInterface
   public interface SpriteShiftFunc {
      void shift(float var1, float var2, SuperByteBuffer.SpriteShiftFunc.Output var3);

      public interface Output {
         void accept(float var1, float var2);
      }
   }
}
