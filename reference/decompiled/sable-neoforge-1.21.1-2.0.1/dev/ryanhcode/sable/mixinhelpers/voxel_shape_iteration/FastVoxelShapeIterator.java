package dev.ryanhcode.sable.mixinhelpers.voxel_shape_iteration;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import java.util.Iterator;
import java.util.NoSuchElementException;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class FastVoxelShapeIterator implements Iterator<BoundingBox3dc> {
   private final BoundingBox3d boundingBox;
   private final LongArrayDiscreteVoxelShape shape;
   private final int xSize;
   private final int ySize;
   private final int zSize;
   private final double[] xValues;
   private final double[] yValues;
   private final double[] zValues;
   private int x;
   private int y;
   private int z;
   private int k;
   private boolean hasNext;

   public FastVoxelShapeIterator(DiscreteVoxelShape shape, double[] xValues, double[] yValues, double[] zValues) {
      BitSetDiscreteVoxelShape sourceShape = new BitSetDiscreteVoxelShape(shape);
      this.xSize = shape.getSize(Axis.X);
      this.ySize = shape.getSize(Axis.Y);
      this.zSize = shape.getSize(Axis.Z);
      this.shape = new LongArrayDiscreteVoxelShape(sourceShape, this.xSize, this.ySize, this.zSize);
      this.xValues = xValues;
      this.yValues = yValues;
      this.zValues = zValues;
      this.boundingBox = new BoundingBox3d();
   }

   public void reset() {
      this.shape.reset();
      this.x = 0;
      this.y = 0;
      this.z = 0;
      this.k = -1;
      this.hasNext = false;
   }

   private void findNext() {
      while (this.y < this.ySize) {
         while (this.x < this.xSize) {
            for (; this.z <= this.zSize; this.z++) {
               if (this.shape.isFullWide(this.x, this.y, this.z)) {
                  if (this.k == -1) {
                     this.k = this.z;
                  }
               } else if (this.k != -1) {
                  int m = this.x;
                  int n = this.y;
                  this.shape.clearZStrip(this.k, this.z, this.x, this.y);

                  while (this.shape.isZStripFull(this.k, this.z, m + 1, this.y)) {
                     this.shape.clearZStrip(this.k, this.z, m + 1, this.y);
                     m++;
                  }

                  while (this.shape.isXZRectangleFull(this.x, m + 1, this.k, this.z, n + 1)) {
                     for (int o = this.x; o <= m; o++) {
                        this.shape.clearZStrip(this.k, this.z, o, n + 1);
                     }

                     n++;
                  }

                  this.boundingBox
                     .set(this.xValues[this.x], this.yValues[this.y], this.zValues[this.k], this.xValues[m + 1], this.yValues[n + 1], this.zValues[this.z]);
                  this.hasNext = true;
                  this.k = -1;
                  return;
               }
            }

            this.k = -1;
            this.z = 0;
            this.x++;
         }

         this.x = 0;
         this.y++;
      }

      this.y = 0;
   }

   @Override
   public boolean hasNext() {
      if (!this.hasNext) {
         this.findNext();
      }

      return this.hasNext;
   }

   public BoundingBox3dc next() {
      if (!this.hasNext) {
         this.findNext();
         if (!this.hasNext) {
            throw new NoSuchElementException();
         }
      }

      this.hasNext = false;
      return this.boundingBox;
   }
}
