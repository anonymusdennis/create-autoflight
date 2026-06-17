package net.createmod.ponder.foundation;

import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.scene.PositionUtil;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.api.scene.SelectionUtil;
import net.createmod.ponder.api.scene.VectorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

public class PonderSceneBuildingUtil implements SceneBuildingUtil {
   private final SelectionUtil select;
   private final VectorUtil vector;
   private final PositionUtil grid;
   private final BoundingBox sceneBounds;

   PonderSceneBuildingUtil(BoundingBox sceneBounds) {
      this.sceneBounds = sceneBounds;
      this.select = new PonderSceneBuildingUtil.PonderSelectionUtil();
      this.vector = new PonderSceneBuildingUtil.PonderVectorUtil();
      this.grid = new PonderSceneBuildingUtil.PonderPositionUtil();
   }

   @Override
   public SelectionUtil select() {
      return this.select;
   }

   @Override
   public VectorUtil vector() {
      return this.vector;
   }

   @Override
   public PositionUtil grid() {
      return this.grid;
   }

   public class PonderPositionUtil implements PositionUtil {
      @Override
      public BlockPos at(int x, int y, int z) {
         return new BlockPos(x, y, z);
      }

      @Override
      public BlockPos zero() {
         return this.at(0, 0, 0);
      }
   }

   public class PonderSelectionUtil implements SelectionUtil {
      @Override
      public Selection everywhere() {
         return SelectionImpl.of(PonderSceneBuildingUtil.this.sceneBounds);
      }

      @Override
      public Selection position(int x, int y, int z) {
         return this.position(PonderSceneBuildingUtil.this.grid().at(x, y, z));
      }

      @Override
      public Selection position(BlockPos pos) {
         return this.cuboid(pos, BlockPos.ZERO);
      }

      @Override
      public Selection fromTo(int x, int y, int z, int x2, int y2, int z2) {
         return this.fromTo(new BlockPos(x, y, z), new BlockPos(x2, y2, z2));
      }

      @Override
      public Selection fromTo(BlockPos pos1, BlockPos pos2) {
         return this.cuboid(pos1, pos2.subtract(pos1));
      }

      @Override
      public Selection column(int x, int z) {
         return this.cuboid(new BlockPos(x, 1, z), new Vec3i(0, PonderSceneBuildingUtil.this.sceneBounds.getYSpan(), 0));
      }

      @Override
      public Selection layer(int y) {
         return this.layers(y, 1);
      }

      @Override
      public Selection layersFrom(int y) {
         return this.layers(y, PonderSceneBuildingUtil.this.sceneBounds.getYSpan() - y);
      }

      @Override
      public Selection layers(int y, int height) {
         return this.cuboid(
            new BlockPos(0, y, 0),
            new Vec3i(
               PonderSceneBuildingUtil.this.sceneBounds.getXSpan() - 1,
               Math.min(PonderSceneBuildingUtil.this.sceneBounds.getYSpan() - y, height) - 1,
               PonderSceneBuildingUtil.this.sceneBounds.getZSpan() - 1
            )
         );
      }

      @Override
      public Selection cuboid(BlockPos origin, Vec3i size) {
         return SelectionImpl.of(BoundingBox.fromCorners(origin, origin.offset(size)));
      }
   }

   public class PonderVectorUtil implements VectorUtil {
      @Override
      public Vec3 centerOf(int x, int y, int z) {
         return this.centerOf(PonderSceneBuildingUtil.this.grid().at(x, y, z));
      }

      @Override
      public Vec3 centerOf(BlockPos pos) {
         return VecHelper.getCenterOf(pos);
      }

      @Override
      public Vec3 topOf(int x, int y, int z) {
         return this.blockSurface(PonderSceneBuildingUtil.this.grid().at(x, y, z), Direction.UP);
      }

      @Override
      public Vec3 topOf(BlockPos pos) {
         return this.blockSurface(pos, Direction.UP);
      }

      @Override
      public Vec3 blockSurface(BlockPos pos, Direction face) {
         return this.blockSurface(pos, face, 0.0F);
      }

      @Override
      public Vec3 blockSurface(BlockPos pos, Direction face, float margin) {
         return this.centerOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale((double)(0.5F + margin)));
      }

      @Override
      public Vec3 of(double x, double y, double z) {
         return new Vec3(x, y, z);
      }
   }
}
