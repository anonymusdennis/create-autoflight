package dev.ryanhcode.sable.render.region;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.BitSet;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class SimpleCulledRenderRegionBuilder {
   protected static final Comparator<SimpleCulledRenderRegionBuilder.Cube> Z_SORTER = Comparator.comparingInt(cube -> cube.x + cube.sizeX);
   protected static final Comparator<SimpleCulledRenderRegionBuilder.Cube> Y_SORTER = Z_SORTER.thenComparingInt(cube -> cube.z + cube.sizeZ);
   protected final int gridSize;
   protected final List<SimpleCulledRenderRegionBuilder.Cube> cubes;
   protected final BitSet grid;

   public SimpleCulledRenderRegionBuilder(int gridSize) {
      this.gridSize = gridSize;
      this.cubes = new LinkedList<>();
      this.grid = new BitSet(this.gridSize * this.gridSize * this.gridSize);
   }

   private int getGridIndex(int x, int y, int z) {
      return (z * this.gridSize + y) * this.gridSize + x;
   }

   private boolean hasVoxel(int x, int y, int z) {
      for (int i = 0; i < 1; i++) {
         int vx = x >> i;
         int vy = y >> i;
         int vz = z >> i;
         int sideLength = this.gridSize >> i;
         if (vx < 0 || vx >= sideLength || vy < 0 || vy >= sideLength || vz < 0 || vz >= sideLength) {
            return false;
         }

         if (this.grid.get(this.getGridIndex(vx, vy, vz))) {
            return true;
         }
      }

      return false;
   }

   protected boolean shouldFaceRender(@NotNull SimpleCulledRenderRegionBuilder.Cube cube, @NotNull Direction direction) {
      int x0 = cube.x;
      int y0 = cube.y;
      int z0 = cube.z;
      int x1 = cube.x + cube.sizeX;
      int y1 = cube.y + cube.sizeY;
      int z1 = cube.z + cube.sizeZ;

      return switch (direction) {
         case DOWN -> {
            for (int x = x0; x < x1; x++) {
               for (int z = z0; z < z1; z++) {
                  if (!this.hasVoxel(x, y0 - 1, z)) {
                     yield true;
                  }
               }
            }

            yield false;
         }
         case UP -> {
            for (int x = x0; x < x1; x++) {
               for (int z = z0; z < z1; z++) {
                  if (!this.hasVoxel(x, y1, z)) {
                     yield true;
                  }
               }
            }

            yield false;
         }
         case NORTH -> {
            for (int x = x0; x < x1; x++) {
               for (int y = y0; y < y1; y++) {
                  if (!this.hasVoxel(x, y, z0 - 1)) {
                     yield true;
                  }
               }
            }

            yield false;
         }
         case SOUTH -> {
            for (int x = x0; x < x1; x++) {
               for (int y = y0; y < y1; y++) {
                  if (!this.hasVoxel(x, y, z1)) {
                     yield true;
                  }
               }
            }

            yield false;
         }
         case WEST -> {
            for (int z = z0; z < z1; z++) {
               for (int y = y0; y < y1; y++) {
                  if (!this.hasVoxel(x0 - 1, y, z)) {
                     yield true;
                  }
               }
            }

            yield false;
         }
         case EAST -> {
            for (int z = z0; z < z1; z++) {
               for (int y = y0; y < y1; y++) {
                  if (!this.hasVoxel(x1, y, z)) {
                     yield true;
                  }
               }
            }

            yield false;
         }
         default -> throw new MatchException(null, null);
      };
   }

   private void mergeX() {
      for (int y = 0; y < this.gridSize; y++) {
         for (int z = 0; z < this.gridSize; z++) {
            int startX = -1;

            for (int x = 0; x < this.gridSize; x++) {
               boolean set = this.grid.get(this.getGridIndex(x, y, z));
               if (startX == -1) {
                  if (set) {
                     startX = x;
                  }
               } else if (!set) {
                  this.cubes.add(new SimpleCulledRenderRegionBuilder.Cube(startX, y, z, x - startX, 1, 1));
                  startX = -1;
               }
            }

            if (startX != -1) {
               this.cubes.add(new SimpleCulledRenderRegionBuilder.Cube(startX, y, z, this.gridSize - startX, 1, 1));
            }
         }
      }
   }

   private void mergeZ() {
      this.cubes.sort(Z_SORTER);
      int startIndex = -1;
      int x = 0;
      int y = 0;
      int sizeX = 0;
      int sizeZ = 0;
      int nextZ = 0;

      for (int i = 0; i < this.cubes.size(); i++) {
         SimpleCulledRenderRegionBuilder.Cube cube = this.cubes.get(i);
         if (startIndex == -1) {
            startIndex = i;
            x = cube.x;
            y = cube.y;
            sizeX = cube.sizeX;
            sizeZ = cube.sizeZ;
            nextZ = cube.z + sizeZ;
         } else {
            if (cube.sizeX == sizeX && cube.sizeZ == sizeZ && cube.x == x && cube.y == y && cube.z == nextZ) {
               if (i < this.cubes.size() - 1) {
                  nextZ += sizeZ;
                  continue;
               }

               i++;
            }

            int length = i - startIndex - 1;
            if (length > 0) {
               SimpleCulledRenderRegionBuilder.Cube start = this.cubes.get(startIndex);
               SimpleCulledRenderRegionBuilder.Cube end = this.cubes.get(startIndex + length);

               for (int j = 0; j <= length; j++) {
                  this.cubes.remove(startIndex);
               }

               this.cubes.add(startIndex, new SimpleCulledRenderRegionBuilder.Cube(start.x, start.y, start.z, sizeX, start.sizeY, end.z - start.z + end.sizeZ));
            }

            startIndex = -1;
            i -= length + 1;
         }
      }
   }

   private void mergeY() {
      this.cubes.sort(Y_SORTER);
      int startIndex = -1;
      int x = 0;
      int z = 0;
      int sizeX = 0;
      int sizeY = 0;
      int sizeZ = 0;
      int nextY = 0;

      for (int i = 0; i < this.cubes.size(); i++) {
         SimpleCulledRenderRegionBuilder.Cube cube = this.cubes.get(i);
         if (startIndex == -1) {
            startIndex = i;
            x = cube.x;
            z = cube.z;
            sizeX = cube.sizeX;
            sizeY = cube.sizeY;
            sizeZ = cube.sizeZ;
            nextY = cube.y + sizeY;
         } else {
            if (cube.sizeX == sizeX && cube.sizeZ == sizeZ && cube.x == x && cube.y == nextY && cube.z == z) {
               if (i < this.cubes.size() - 1) {
                  nextY += sizeY;
                  continue;
               }

               i++;
            }

            int length = i - startIndex - 1;
            if (length > 0) {
               SimpleCulledRenderRegionBuilder.Cube start = this.cubes.get(startIndex);
               SimpleCulledRenderRegionBuilder.Cube end = this.cubes.get(startIndex + length);

               for (int j = 0; j <= length; j++) {
                  this.cubes.remove(startIndex);
               }

               this.cubes.add(startIndex, new SimpleCulledRenderRegionBuilder.Cube(start.x, start.y, start.z, sizeX, end.y - start.y + end.sizeY, sizeZ));
            }

            startIndex = -1;
            i -= length + 1;
         }
      }
   }

   public void add(int x, int y, int z) {
      this.grid.set(this.getGridIndex(x, y, z));
   }

   public void build() {
      this.cubes.clear();
      this.mergeX();
      this.mergeZ();
      this.mergeY();
   }

   public void buildNoGreedy() {
      this.cubes.clear();

      for (int y = 0; y < this.gridSize; y++) {
         for (int z = 0; z < this.gridSize; z++) {
            for (int x = 0; x < this.gridSize; x++) {
               if (this.grid.get(this.getGridIndex(x, y, z))) {
                  this.cubes.add(new SimpleCulledRenderRegionBuilder.Cube(x, y, z, 1, 1, 1));
               }
            }
         }
      }
   }

   public void render(@NotNull Matrix4f matrix4f, @NotNull VertexConsumer consumer) {
      for (SimpleCulledRenderRegionBuilder.Cube cube : this.cubes) {
         int x0 = cube.x;
         int y0 = cube.y;
         int z0 = cube.z;
         int x1 = cube.x + cube.sizeX;
         int y1 = cube.y + cube.sizeY;
         int z1 = cube.z + cube.sizeZ;
         if (this.shouldFaceRender(cube, Direction.NORTH)) {
            consumer.addVertex(matrix4f, (float)x0, (float)y0, (float)z0);
            consumer.addVertex(matrix4f, (float)x0, (float)y1, (float)z0);
            consumer.addVertex(matrix4f, (float)x1, (float)y1, (float)z0);
            consumer.addVertex(matrix4f, (float)x1, (float)y0, (float)z0);
         }

         if (this.shouldFaceRender(cube, Direction.EAST)) {
            consumer.addVertex(matrix4f, (float)x1, (float)y0, (float)z0);
            consumer.addVertex(matrix4f, (float)x1, (float)y1, (float)z0);
            consumer.addVertex(matrix4f, (float)x1, (float)y1, (float)z1);
            consumer.addVertex(matrix4f, (float)x1, (float)y0, (float)z1);
         }

         if (this.shouldFaceRender(cube, Direction.SOUTH)) {
            consumer.addVertex(matrix4f, (float)x1, (float)y0, (float)z1);
            consumer.addVertex(matrix4f, (float)x1, (float)y1, (float)z1);
            consumer.addVertex(matrix4f, (float)x0, (float)y1, (float)z1);
            consumer.addVertex(matrix4f, (float)x0, (float)y0, (float)z1);
         }

         if (this.shouldFaceRender(cube, Direction.WEST)) {
            consumer.addVertex(matrix4f, (float)x0, (float)y0, (float)z1);
            consumer.addVertex(matrix4f, (float)x0, (float)y1, (float)z1);
            consumer.addVertex(matrix4f, (float)x0, (float)y1, (float)z0);
            consumer.addVertex(matrix4f, (float)x0, (float)y0, (float)z0);
         }

         if (this.shouldFaceRender(cube, Direction.DOWN)) {
            consumer.addVertex(matrix4f, (float)x0, (float)y0, (float)z0);
            consumer.addVertex(matrix4f, (float)x1, (float)y0, (float)z0);
            consumer.addVertex(matrix4f, (float)x1, (float)y0, (float)z1);
            consumer.addVertex(matrix4f, (float)x0, (float)y0, (float)z1);
         }

         if (this.shouldFaceRender(cube, Direction.UP)) {
            consumer.addVertex(matrix4f, (float)x0, (float)y1, (float)z1);
            consumer.addVertex(matrix4f, (float)x1, (float)y1, (float)z1);
            consumer.addVertex(matrix4f, (float)x1, (float)y1, (float)z0);
            consumer.addVertex(matrix4f, (float)x0, (float)y1, (float)z0);
         }
      }
   }

   @NotNull
   public List<SimpleCulledRenderRegionBuilder.Cube> getCubes() {
      return this.cubes;
   }

   public static record Cube(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
   }
}
