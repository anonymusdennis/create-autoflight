package net.createmod.catnip.render;

public class TemplateMesh {
   public static final int INT_STRIDE = 9;
   public static final int BYTE_STRIDE = 36;
   public static final int X_OFFSET = 0;
   public static final int Y_OFFSET = 1;
   public static final int Z_OFFSET = 2;
   public static final int COLOR_OFFSET = 3;
   public static final int U_OFFSET = 4;
   public static final int V_OFFSET = 5;
   public static final int OVERLAY_OFFSET = 6;
   public static final int LIGHT_OFFSET = 7;
   public static final int NORMAL_OFFSET = 8;
   protected int[] data;
   protected int vertexCount;

   public TemplateMesh(int[] data) {
      if (data.length % 9 != 0) {
         throw new IllegalArgumentException("Received invalid vertex data");
      } else {
         this.data = data;
         this.vertexCount = data.length / 9;
      }
   }

   public TemplateMesh(int vertexCount) {
      this.data = new int[vertexCount * 9];
      this.vertexCount = vertexCount;
   }

   public float x(int index) {
      return Float.intBitsToFloat(this.data[index * 9 + 0]);
   }

   public float y(int index) {
      return Float.intBitsToFloat(this.data[index * 9 + 1]);
   }

   public float z(int index) {
      return Float.intBitsToFloat(this.data[index * 9 + 2]);
   }

   public int color(int index) {
      return this.data[index * 9 + 3];
   }

   public float u(int index) {
      return Float.intBitsToFloat(this.data[index * 9 + 4]);
   }

   public float v(int index) {
      return Float.intBitsToFloat(this.data[index * 9 + 5]);
   }

   public int overlay(int index) {
      return this.data[index * 9 + 6];
   }

   public int light(int index) {
      return this.data[index * 9 + 7];
   }

   public int normal(int index) {
      return this.data[index * 9 + 8];
   }

   public int vertexCount() {
      return this.vertexCount;
   }

   public boolean isEmpty() {
      return this.vertexCount == 0;
   }
}
