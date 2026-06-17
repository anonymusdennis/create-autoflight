package com.simibubi.create.foundation.collision;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Matrix3d {
   double m00;
   double m01;
   double m02;
   double m10;
   double m11;
   double m12;
   double m20;
   double m21;
   double m22;

   public Matrix3d asIdentity() {
      this.m00 = this.m11 = this.m22 = 1.0;
      this.m01 = this.m02 = this.m10 = this.m12 = this.m20 = this.m21 = 0.0;
      return this;
   }

   public Matrix3d asXRotation(float radians) {
      this.asIdentity();
      if (radians == 0.0F) {
         return this;
      } else {
         double s = (double)Mth.sin(radians);
         double c = (double)Mth.cos(radians);
         this.m22 = this.m11 = c;
         this.m21 = s;
         this.m12 = -s;
         return this;
      }
   }

   public Matrix3d asYRotation(float radians) {
      this.asIdentity();
      if (radians == 0.0F) {
         return this;
      } else {
         double s = (double)Mth.sin(radians);
         double c = (double)Mth.cos(radians);
         this.m00 = this.m22 = c;
         this.m02 = s;
         this.m20 = -s;
         return this;
      }
   }

   public Matrix3d asZRotation(float radians) {
      this.asIdentity();
      if (radians == 0.0F) {
         return this;
      } else {
         double s = (double)Mth.sin(radians);
         double c = (double)Mth.cos(radians);
         this.m00 = this.m11 = c;
         this.m01 = -s;
         this.m10 = s;
         return this;
      }
   }

   public Matrix3d scale(double d) {
      this.m00 *= d;
      this.m11 *= d;
      this.m22 *= d;
      return this;
   }

   public Matrix3d multiply(Matrix3d m) {
      double new00 = this.m00 * m.m00 + this.m01 * m.m10 + this.m02 * m.m20;
      double new01 = this.m00 * m.m01 + this.m01 * m.m11 + this.m02 * m.m21;
      double new02 = this.m00 * m.m02 + this.m01 * m.m12 + this.m02 * m.m22;
      double new10 = this.m10 * m.m00 + this.m11 * m.m10 + this.m12 * m.m20;
      double new11 = this.m10 * m.m01 + this.m11 * m.m11 + this.m12 * m.m21;
      double new12 = this.m10 * m.m02 + this.m11 * m.m12 + this.m12 * m.m22;
      double new20 = this.m20 * m.m00 + this.m21 * m.m10 + this.m22 * m.m20;
      double new21 = this.m20 * m.m01 + this.m21 * m.m11 + this.m22 * m.m21;
      double new22 = this.m20 * m.m02 + this.m21 * m.m12 + this.m22 * m.m22;
      this.m00 = new00;
      this.m01 = new01;
      this.m02 = new02;
      this.m10 = new10;
      this.m11 = new11;
      this.m12 = new12;
      this.m20 = new20;
      this.m21 = new21;
      this.m22 = new22;
      return this;
   }

   public Vec3 transform(Vec3 vec) {
      return this.transform(vec.x, vec.y, vec.z);
   }

   public Vec3 transformTransposed(Vec3 vec) {
      return this.transformTransposed(vec.x, vec.y, vec.z);
   }

   public Vec3 transform(double vecX, double vecY, double vecZ) {
      double x = vecX * this.m00 + vecY * this.m01 + vecZ * this.m02;
      double y = vecX * this.m10 + vecY * this.m11 + vecZ * this.m12;
      double z = vecX * this.m20 + vecY * this.m21 + vecZ * this.m22;
      return new Vec3(x, y, z);
   }

   public Vec3 transformTransposed(double vecX, double vecY, double vecZ) {
      double x = vecX * this.m00 + vecY * this.m10 + vecZ * this.m20;
      double y = vecX * this.m01 + vecY * this.m11 + vecZ * this.m21;
      double z = vecX * this.m02 + vecY * this.m12 + vecZ * this.m22;
      return new Vec3(x, y, z);
   }
}
