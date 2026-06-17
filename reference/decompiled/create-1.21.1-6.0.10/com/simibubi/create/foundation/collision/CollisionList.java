package com.simibubi.create.foundation.collision;

import net.minecraft.world.phys.shapes.Shapes.DoubleLineConsumer;

public class CollisionList {
   public static final int DEFAULT_CAPACITY = 16;
   public double[] centerX = new double[16];
   public double[] centerY = new double[16];
   public double[] centerZ = new double[16];
   public double[] extentsX = new double[16];
   public double[] extentsY = new double[16];
   public double[] extentsZ = new double[16];
   public int size = 0;

   public static class Populate implements DoubleLineConsumer {
      private final CollisionList collisionList;
      public int offsetX = 0;
      public int offsetY = 0;
      public int offsetZ = 0;

      public Populate(CollisionList collisionList) {
         this.collisionList = collisionList;
      }

      public void consume(double x1, double y1, double z1, double x2, double y2, double z2) {
         this.append(
            (double)this.offsetX + 0.5 * (x2 + x1),
            (double)this.offsetY + 0.5 * (y2 + y1),
            (double)this.offsetZ + 0.5 * (z2 + z1),
            0.5 * (x2 - x1),
            0.5 * (y2 - y1),
            0.5 * (z2 - z1)
         );
      }

      public void append(double centerX, double centerY, double centerZ, double extentsX, double extentsY, double extentsZ) {
         if (this.collisionList.size == this.collisionList.centerX.length) {
            int newCapacity = this.collisionList.centerX.length * 2;
            double[] newCenterX = new double[newCapacity];
            double[] newCenterY = new double[newCapacity];
            double[] newCenterZ = new double[newCapacity];
            double[] newExtentsX = new double[newCapacity];
            double[] newExtentsY = new double[newCapacity];
            double[] newExtentsZ = new double[newCapacity];
            System.arraycopy(this.collisionList.centerX, 0, newCenterX, 0, this.collisionList.size);
            System.arraycopy(this.collisionList.centerY, 0, newCenterY, 0, this.collisionList.size);
            System.arraycopy(this.collisionList.centerZ, 0, newCenterZ, 0, this.collisionList.size);
            System.arraycopy(this.collisionList.extentsX, 0, newExtentsX, 0, this.collisionList.size);
            System.arraycopy(this.collisionList.extentsY, 0, newExtentsY, 0, this.collisionList.size);
            System.arraycopy(this.collisionList.extentsZ, 0, newExtentsZ, 0, this.collisionList.size);
            this.collisionList.centerX = newCenterX;
            this.collisionList.centerY = newCenterY;
            this.collisionList.centerZ = newCenterZ;
            this.collisionList.extentsX = newExtentsX;
            this.collisionList.extentsY = newExtentsY;
            this.collisionList.extentsZ = newExtentsZ;
         }

         this.collisionList.centerX[this.collisionList.size] = centerX;
         this.collisionList.centerY[this.collisionList.size] = centerY;
         this.collisionList.centerZ[this.collisionList.size] = centerZ;
         this.collisionList.extentsX[this.collisionList.size] = extentsX;
         this.collisionList.extentsY[this.collisionList.size] = extentsY;
         this.collisionList.extentsZ[this.collisionList.size] = extentsZ;
         this.collisionList.size++;
      }

      public void appendFrom(CollisionList collisionList, int bbIdx) {
         this.append(
            collisionList.centerX[bbIdx],
            collisionList.centerY[bbIdx],
            collisionList.centerZ[bbIdx],
            collisionList.extentsX[bbIdx],
            collisionList.extentsY[bbIdx],
            collisionList.extentsZ[bbIdx]
         );
      }
   }
}
