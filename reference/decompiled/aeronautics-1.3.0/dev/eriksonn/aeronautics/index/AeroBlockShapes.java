package dev.eriksonn.aeronautics.index;

import java.util.function.BiFunction;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AeroBlockShapes {
   public static final VoxelShaper STEAM_VENT = shape(1.0, 0.0, 1.0, 15.0, 3.0, 15.0).add(3.0, 2.0, 3.0, 13.0, 16.0, 13.0).forAxis();
   public static final VoxelShaper PROPELLER_BEARING = shape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
      .erase(0.0, 0.0, 0.0, 5.0, 12.0, 5.0)
      .erase(11.0, 0.0, 0.0, 16.0, 12.0, 5.0)
      .erase(0.0, 0.0, 11.0, 5.0, 12.0, 16.0)
      .erase(11.0, 0.0, 11.0, 16.0, 12.0, 16.0)
      .forDirectional();
   public static final VoxelShaper PROPELLER = shape(0.0, 0.0, 0.0, 16.0, 6.0, 16.0).add(4.0, 5.0, 4.0, 12.0, 12.0, 12.0).forDirectional();
   public static final VoxelShaper SMART_PROPELLER = shape(0.0, 0.0, 0.0, 16.0, 4.0, 16.0).add(2.0, 4.0, 2.0, 14.0, 10.0, 14.0).forHorizontal(Direction.NORTH);
   public static final VoxelShaper SMART_PROPELLER_CEILING = shape(0.0, 12.0, 0.0, 16.0, 16.0, 16.0)
      .add(2.0, 6.0, 2.0, 14.0, 12.0, 14.0)
      .forHorizontal(Direction.NORTH);
   public static final VoxelShape HOT_AIR_BURNER = shape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0).add(3.0, 8.0, 3.0, 13.0, 16.0, 13.0).build();
   public static final VoxelShape HOT_AIR_BURNER_PLAYER_COLLISION = shape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0).add(3.0, 8.0, 3.0, 13.0, 15.99, 13.0).build();
   public static final VoxelShape HOT_AIR_BURNER_SMOKE_CLIP = shape(0.0, 0.0, 0.0, 16.0, 7.0, 16.0).build();
   public static final VoxelShape MOUNTED_POTATO_CANNON = shape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0)
      .add(1.0, 8.0, 1.0, 15.0, 12.0, 15.0)
      .add(4.0, 12.0, 4.0, 12.0, 28.0, 12.0)
      .erase(5.0, 18.0, 5.0, 11.0, 28.0, 11.0)
      .build();
   public static final VoxelShape MOUNTED_POTATO_CANNON_BLOCKED = shape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0).add(1.0, 8.0, 1.0, 15.0, 12.0, 15.0).build();

   private static AeroBlockShapes.Builder shape(VoxelShape shape) {
      return new AeroBlockShapes.Builder(shape);
   }

   private static AeroBlockShapes.Builder shape(double x1, double y1, double z1, double x2, double y2, double z2) {
      return shape(cuboid(x1, y1, z1, x2, y2, z2));
   }

   private static VoxelShape cuboid(double x1, double y1, double z1, double x2, double y2, double z2) {
      return Block.box(x1, y1, z1, x2, y2, z2);
   }

   public static class Builder {
      private VoxelShape shape;

      public Builder(VoxelShape shape) {
         this.shape = shape;
      }

      public AeroBlockShapes.Builder add(VoxelShape shape) {
         this.shape = Shapes.or(this.shape, shape);
         return this;
      }

      public AeroBlockShapes.Builder add(double x1, double y1, double z1, double x2, double y2, double z2) {
         return this.add(AeroBlockShapes.cuboid(x1, y1, z1, x2, y2, z2));
      }

      public AeroBlockShapes.Builder erase(double x1, double y1, double z1, double x2, double y2, double z2) {
         this.shape = Shapes.join(this.shape, AeroBlockShapes.cuboid(x1, y1, z1, x2, y2, z2), BooleanOp.ONLY_FIRST);
         return this;
      }

      public VoxelShape build() {
         return this.shape;
      }

      public VoxelShaper build(BiFunction<VoxelShape, Direction, VoxelShaper> factory, Direction direction) {
         return factory.apply(this.shape, direction);
      }

      public VoxelShaper build(BiFunction<VoxelShape, Axis, VoxelShaper> factory, Axis axis) {
         return factory.apply(this.shape, axis);
      }

      public VoxelShaper forAxis() {
         return this.build(VoxelShaper::forAxis, Axis.Y);
      }

      public VoxelShaper forHorizontalAxis() {
         return this.build(VoxelShaper::forHorizontalAxis, Axis.Z);
      }

      public VoxelShaper forHorizontal(Direction direction) {
         return this.build(VoxelShaper::forHorizontal, direction);
      }

      public VoxelShaper forDirectional(Direction direction) {
         return this.build(VoxelShaper::forDirectional, direction);
      }

      public VoxelShaper forDirectional() {
         return this.forDirectional(Direction.UP);
      }
   }
}
