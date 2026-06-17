package com.simibubi.create.foundation.render;

public class MutableTemplateMesh extends TemplateMesh {
   public MutableTemplateMesh(int[] data) {
      super(data);
   }

   public MutableTemplateMesh(int vertexCount) {
      super(vertexCount);
   }

   public void copyFrom(int index, TemplateMesh template) {
      System.arraycopy(template.data, 0, this.data, index * 9, template.data.length);
   }

   public void x(int index, float x) {
      this.data[index * 9 + 0] = Float.floatToRawIntBits(x);
   }

   public void y(int index, float y) {
      this.data[index * 9 + 1] = Float.floatToRawIntBits(y);
   }

   public void z(int index, float z) {
      this.data[index * 9 + 2] = Float.floatToRawIntBits(z);
   }

   public void color(int index, int color) {
      this.data[index * 9 + 3] = color;
   }

   public void u(int index, float u) {
      this.data[index * 9 + 4] = Float.floatToRawIntBits(u);
   }

   public void v(int index, float v) {
      this.data[index * 9 + 5] = Float.floatToRawIntBits(v);
   }

   public void overlay(int index, int overlay) {
      this.data[index * 9 + 6] = overlay;
   }

   public void light(int index, int light) {
      this.data[index * 9 + 7] = light;
   }

   public void normal(int index, int normal) {
      this.data[index * 9 + 8] = normal;
   }

   public TemplateMesh toImmutable() {
      return new TemplateMesh(this.data);
   }
}
