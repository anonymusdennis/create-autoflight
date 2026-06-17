package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegionBuilder;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegionBuilder.Cube;
import dev.ryanhcode.sable.util.LevelAccelerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class HeatedCulledRenderRegionBuilder extends SimpleCulledRenderRegionBuilder {
   private final BlockPos worldOrigin;
   private final LevelAccelerator accelerator;

   public HeatedCulledRenderRegionBuilder(BlockPos worldOrigin, LevelAccelerator accelerator, int gridSize) {
      super(gridSize);
      this.worldOrigin = worldOrigin;
      this.accelerator = accelerator;
   }

   public void render(@NotNull Matrix4f matrix4f, @NotNull VertexConsumer consumer) {
      MutableBlockPos pos = new MutableBlockPos();

      for (Cube cube : this.getCubes()) {
         int x0 = cube.x();
         int y0 = cube.y();
         int z0 = cube.z();
         int x1 = cube.x() + cube.sizeX();
         int y1 = cube.y() + cube.sizeY();
         int z1 = cube.z() + cube.sizeZ();
         if (this.shouldFaceRender(cube, Direction.NORTH)) {
            Direction dir = Direction.NORTH;
            consumer.addVertex(matrix4f, (float)x0, (float)y0, (float)z0)
               .setColor(this.getColor(x0, y0, z0))
               .setUv(0.0F, 0.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x0, (float)y1, (float)z0)
               .setColor(this.getColor(x0, y1, z0))
               .setUv(0.0F, 1.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x1, (float)y1, (float)z0)
               .setColor(this.getColor(x1, y1, z0))
               .setUv(1.0F, 1.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x1, (float)y0, (float)z0)
               .setColor(this.getColor(x1, y0, z0))
               .setUv(1.0F, 0.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
         }

         if (this.shouldFaceRender(cube, Direction.EAST)) {
            Direction dir = Direction.NORTH;
            consumer.addVertex(matrix4f, (float)x1, (float)y0, (float)z0)
               .setColor(this.getColor(x1, y0, z0))
               .setUv(0.0F, 0.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x1, (float)y1, (float)z0)
               .setColor(this.getColor(x1, y1, z0))
               .setUv(0.0F, 1.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x1, (float)y1, (float)z1)
               .setColor(this.getColor(x1, y1, z1))
               .setUv(1.0F, 1.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x1, (float)y0, (float)z1)
               .setColor(this.getColor(x1, y0, z1))
               .setUv(1.0F, 0.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
         }

         if (this.shouldFaceRender(cube, Direction.SOUTH)) {
            Direction dir = Direction.NORTH;
            consumer.addVertex(matrix4f, (float)x1, (float)y0, (float)z1)
               .setColor(this.getColor(x1, y0, z1))
               .setUv(1.0F, 0.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x1, (float)y1, (float)z1)
               .setColor(this.getColor(x1, y1, z1))
               .setUv(1.0F, 1.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x0, (float)y1, (float)z1)
               .setColor(this.getColor(x0, y1, z1))
               .setUv(0.0F, 1.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x0, (float)y0, (float)z1)
               .setColor(this.getColor(x0, y0, z1))
               .setUv(0.0F, 0.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
         }

         if (this.shouldFaceRender(cube, Direction.WEST)) {
            Direction dir = Direction.NORTH;
            consumer.addVertex(matrix4f, (float)x0, (float)y0, (float)z1)
               .setColor(this.getColor(x0, y0, z1))
               .setUv(1.0F, 0.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x0, (float)y1, (float)z1)
               .setColor(this.getColor(x0, y1, z1))
               .setUv(1.0F, 1.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x0, (float)y1, (float)z0)
               .setColor(this.getColor(x0, y1, z0))
               .setUv(0.0F, 1.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x0, (float)y0, (float)z0)
               .setColor(this.getColor(x0, y0, z0))
               .setUv(0.0F, 0.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
         }

         if (this.shouldFaceRender(cube, Direction.DOWN)) {
            Direction dir = Direction.DOWN;
            if (this.accelerator.getBlockState(pos.set(cube.x(), cube.y() - 1, cube.z()).offset(this.worldOrigin)).is(AeroTags.BlockTags.AIRTIGHT)) {
               consumer.addVertex(matrix4f, (float)x0, (float)y0, (float)z0)
                  .setColor(this.getColor(x0, y0, z0))
                  .setUv(0.0F, 1.0F)
                  .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
               consumer.addVertex(matrix4f, (float)x1, (float)y0, (float)z0)
                  .setColor(this.getColor(x1, y0, z0))
                  .setUv(1.0F, 1.0F)
                  .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
               consumer.addVertex(matrix4f, (float)x1, (float)y0, (float)z1)
                  .setColor(this.getColor(x1, y0, z1))
                  .setUv(1.0F, 0.0F)
                  .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
               consumer.addVertex(matrix4f, (float)x0, (float)y0, (float)z1)
                  .setColor(this.getColor(x0, y0, z1))
                  .setUv(0.0F, 0.0F)
                  .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            }
         }

         if (this.shouldFaceRender(cube, Direction.UP)) {
            Direction dir = Direction.UP;
            consumer.addVertex(matrix4f, (float)x0, (float)y1, (float)z1)
               .setColor(this.getColor(x0, y1, z1))
               .setUv(0.0F, 1.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x1, (float)y1, (float)z1)
               .setColor(this.getColor(x1, y1, z1))
               .setUv(1.0F, 1.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x1, (float)y1, (float)z0)
               .setColor(this.getColor(x1, y1, z0))
               .setUv(1.0F, 0.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
            consumer.addVertex(matrix4f, (float)x0, (float)y1, (float)z0)
               .setColor(this.getColor(x0, y1, z0))
               .setUv(0.0F, 0.0F)
               .setNormal((float)dir.getStepX(), (float)dir.getStepY(), (float)dir.getStepZ());
         }
      }
   }

   private int getColor(int x, int y, int z) {
      return -1;
   }
}
