package dev.simulated_team.simulated.index;

import java.util.function.BiFunction;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SimBlockShapes {
   public static final VoxelShaper PLUNGER_BLOCK = shape(4.0, 12.0, 4.0, 12.0, 16.0, 12.0)
      .add(7.0, -6.0, 7.0, 9.0, 10.0, 9.0)
      .add(6.0, 10.0, 6.0, 10.0, 12.0, 10.0)
      .forDirectional();
   public static final VoxelShaper SYMMETRIC_SAIL = shape(0.0, 6.0, 0.0, 16.0, 10.0, 16.0).forDirectional();
   public static final VoxelShaper OPTICAL_SENSOR = shape(0.0, 0.0, 0.0, 16.0, 16.0, 6.0)
      .add(0.0, 0.0, 10.0, 16.0, 16.0, 16.0)
      .add(1.0, 1.0, 6.0, 15.0, 15.0, 10.0)
      .forDirectional(Direction.NORTH);
   public static final VoxelShaper REDSTONE_INDUCTOR = shape(0.0, 0.0, 0.0, 16.0, 2.0, 16.0).add(4.0, 2.0, 5.0, 12.0, 6.0, 11.0).forHorizontal(Direction.NORTH);
   public static final VoxelShaper MODULATING_DIRECTIONAL_LINK = shape(1.0, 0.0, 1.0, 15.0, 3.0, 15.0).forDirectional();
   public static final VoxelShaper LINKED_TYPEWRITER = shape(0.0, 0.0, 0.0, 16.0, 3.0, 7.0).add(0.0, 0.0, 7.0, 16.0, 7.0, 16.0).forHorizontal(Direction.NORTH);
   public static final VoxelShaper SWIVEL_BEARING_ASSEMBLED = shape(0.0, 0.0, 0.0, 16.0, 11.9, 16.0).forDirectional(Direction.UP);
   public static final VoxelShaper SWIVEL_BEARING_PLATE = shape(0.0, 12.1, 0.0, 16.0, 16.0, 16.0).forDirectional(Direction.UP);
   public static final VoxelShaper SWIVEL_BEARING_PLATE_COLLISION = shape(3.0, 12.0, 3.0, 13.0, 16.0, 13.0).forDirectional(Direction.UP);
   public static final VoxelShaper PORTABLE_ENGINE = shape(0.0, 0.0, 0.0, 16.0, 4.0, 16.0).add(3.0, 2.0, 1.0, 13.0, 14.0, 15.0).forDirectional(Direction.NORTH);
   public static final VoxelShaper PHYSICS_ASSEMBLER_COLLISION = shape(0.0, 0.0, 0.0, 16.0, 3.0, 16.0)
      .add(2.0, 3.0, 2.0, 14.0, 12.0, 14.0)
      .forDirectional(Direction.NORTH);
   public static final VoxelShaper PHYSICS_ASSEMBLER_CEILING_COLLISION = shape(0.0, 13.0, 0.0, 16.0, 16.0, 16.0)
      .add(2.0, 4.0, 2.0, 14.0, 13.0, 14.0)
      .forDirectional(Direction.SOUTH);
   public static final VoxelShaper PHYSICS_ASSEMBLER_WALL_COLLISION = shape(0.0, 0.0, 0.0, 16.0, 3.0, 16.0)
      .add(2.0, 3.0, 2.0, 14.0, 12.0, 14.0)
      .forDirectional(Direction.DOWN);
   public static final VoxelShaper PHYSICS_ASSEMBLER_OUTLINE = shape(0.0, 0.0, 0.0, 16.0, 4.0, 16.0)
      .add(2.0, 3.0, 2.0, 5.0, 13.0, 14.0)
      .add(2.0, 3.0, 2.0, 14.0, 6.0, 14.0)
      .add(11.0, 3.0, 2.0, 14.0, 13.0, 14.0)
      .forDirectional(Direction.NORTH);
   public static final VoxelShaper PHYSICS_ASSEMBLER_CEILING_OUTLINE = shape(0.0, 12.0, 0.0, 16.0, 16.0, 16.0)
      .add(2.0, 3.0, 2.0, 5.0, 13.0, 14.0)
      .add(2.0, 10.0, 2.0, 14.0, 13.0, 14.0)
      .add(11.0, 3.0, 2.0, 14.0, 13.0, 14.0)
      .forDirectional(Direction.SOUTH);
   public static final VoxelShaper PHYSICS_ASSEMBLER_WALL_OUTLINE = shape(0.0, 0.0, 0.0, 16.0, 4.0, 16.0)
      .add(2.0, 3.0, 2.0, 5.0, 13.0, 14.0)
      .add(2.0, 3.0, 2.0, 14.0, 6.0, 14.0)
      .add(11.0, 3.0, 2.0, 14.0, 13.0, 14.0)
      .forDirectional(Direction.DOWN);
   public static final VoxelShaper ALTITUDE_SENSOR_FLOOR = shape(1.0, 2.0, 6.0, 15.0, 10.0, 10.0)
      .add(2.0, 2.0, 4.0, 14.0, 14.0, 9.0)
      .add(4.0, 4.0, 9.0, 12.0, 12.0, 13.0)
      .add(0.0, 0.0, 0.0, 16.0, 2.0, 16.0)
      .forHorizontal(Direction.NORTH);
   public static final VoxelShaper ALTITUDE_SENSOR_CEILING = shape(1.0, 6.0, 6.0, 15.0, 14.0, 10.0)
      .add(2.0, 2.0, 4.0, 14.0, 14.0, 9.0)
      .add(4.0, 4.0, 9.0, 12.0, 12.0, 13.0)
      .add(0.0, 14.0, 0.0, 16.0, 16.0, 16.0)
      .forHorizontal(Direction.NORTH);
   public static final VoxelShaper ALTITUDE_SENSOR_WALL = shape(1.0, 6.0, 6.0, 15.0, 10.0, 14.0)
      .add(2.0, 2.0, 4.0, 14.0, 14.0, 9.0)
      .add(4.0, 4.0, 9.0, 12.0, 12.0, 14.0)
      .add(0.0, 0.0, 14.0, 16.0, 16.0, 16.0)
      .forHorizontal(Direction.NORTH);
   public static final VoxelShaper NAV_TABLE = shape(0.0, 9.0, 0.0, 16.0, 13.0, 16.0)
      .add(0.0, 0.0, 0.0, 16.0, 2.0, 16.0)
      .add(2.0, 2.0, 2.0, 14.0, 9.0, 14.0)
      .forDirectional();
   public static final VoxelShaper STEERING_WHEEL_MOUNT = shape(2.0, 2.0, 0.0, 14.0, 12.0, 16.0).forDirectional();
   public static final VoxelShaper STEERING_WHEEL_FLOOR = shape(-1.0, 13.5, -6.0, 17.0, 15.5, 12.0).forDirectional();
   public static final VoxelShaper STEERING_WHEEL_CEILING = shape(-1.0, 13.5, 4.0, 17.0, 15.5, 22.0).forDirectional();
   public static final VoxelShaper STEERING_WHEEL_FULL_FLOOR = shape(STEERING_WHEEL_MOUNT.get(Direction.UP))
      .add(STEERING_WHEEL_FLOOR.get(Direction.UP))
      .forDirectional();
   public static final VoxelShaper STEERING_WHEEL_FULL_CEILING = shape(STEERING_WHEEL_MOUNT.get(Direction.UP))
      .add(STEERING_WHEEL_CEILING.get(Direction.UP))
      .forDirectional();
   public static final VoxelShaper FOURTEEN_VOXEL_POLE = shape(1.0, 0.0, 1.0, 15.0, 16.0, 15.0).forAxis();
   public static final VoxelShaper AUGER_END_SHAPE = shape(1.0, 0.0, 1.0, 15.0, 16.0, 15.0).add(0.0, 0.0, 0.0, 16.0, 2.0, 16.0).forDirectional();
   public static final VoxelShaper THROTTLE_LEVER = shape(4.0, 0.0, 3.0, 12.0, 3.0, 13.0).add(4.0, 0.0, 6.0, 12.0, 5.0, 10.0).forDirectional(Direction.UP);
   public static final VoxelShaper THROTTLE_LEVER_SWAP = shape(3.0, 0.0, 4.0, 13.0, 3.0, 12.0).add(6.0, 0.0, 4.0, 10.0, 5.0, 12.0).forDirectional(Direction.UP);
   public static final VoxelShaper THROTTLE_LEVER_HANDLE = shape(7.0, 3.0, 7.0, 9.0, 15.0, 9.0)
      .add(6.8, 15.0, 6.8, 9.2, 21.4, 9.2)
      .forDirectional(Direction.UP);
   public static final VoxelShaper THROTTLE_LEVER_HANDLE_SWAP = shape(7.0, 3.0, 7.0, 9.0, 15.0, 9.0)
      .add(6.8, 15.0, 6.8, 9.2, 21.4, 9.2)
      .forDirectional(Direction.UP);
   public static final VoxelShaper TORSION_SPRING = shape(0.0, 0.0, 0.0, 16.0, 6.0, 16.0).add(2.0, 0.0, 2.0, 14.0, 16.0, 14.0).forDirectional(Direction.UP);
   public static final VoxelShaper SMALL_SPRING = shape(5.0, 0.0, 5.0, 11.0, 4.0, 11.0).forDirectional(Direction.UP);
   public static final VoxelShaper SPRING = shape(4.0, 0.0, 4.0, 12.0, 4.0, 12.0).forDirectional(Direction.UP);
   public static final VoxelShaper LARGE_SPRING = shape(3.0, 0.0, 3.0, 13.0, 4.0, 13.0).forDirectional(Direction.UP);
   public static final VoxelShaper MERGING_GlUE = shape(0.0, 0.0, 0.0, 16.0, 1.0, 16.0).forDirectional(Direction.UP);
   public static final VoxelShaper LASER_POINTER = shape(0.0, 0.0, 0.0, 16.0, 10.0, 16.0).add(1.0, 10.0, 1.0, 15.0, 12.0, 15.0).forDirectional();
   public static final VoxelShaper NAMEPLATE = shape(0.0, 3.0, 12.0, 16.0, 13.0, 16.0).forDirectional(Direction.NORTH);
   public static final VoxelShape HANDLE = shape(4.0, 0.0, 0.0, 12.0, 6.0, 16.0).build();
   public static final VoxelShape VELOCITY_SENSOR = shape(0.0, 0.0, 0.0, 16.0, 2.0, 16.0).add(0.0, 2.0, 2.0, 16.0, 16.0, 14.0).build();
   public static final VoxelShape REDSTONE_ACCUMULATOR = shape(0.0, 0.0, 0.0, 16.0, 2.0, 16.0).add(4.0, 2.0, 4.0, 12.0, 7.0, 12.0).build();
   public static final VoxelShape ROPE_WINCH = shape(4.0, 0.0, 0.0, 12.0, 2.0, 16.0).add(2.0, 2.0, 0.0, 14.0, 14.0, 16.0).build();
   public static final VoxelShape ROPE_CONNECTOR = shape(3.0, 0.0, 3.0, 13.0, 2.0, 13.0).add(6.0, 2.0, 3.0, 10.0, 6.0, 13.0).build();
   public static final VoxelShape ROPE_CONNECTOR_COLLIDER = shape(1.0, 0.0, 1.0, 15.0, 0.25, 15.0).build();
   public static final VoxelShape GIMBAL_SENSOR = shape(0.0, 0.0, 0.0, 16.0, 10.0, 16.0).build();
   public static final VoxelShape EVAPORATOR = shape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0).build();

   private static SimBlockShapes.Builder shape(VoxelShape shape) {
      return new SimBlockShapes.Builder(shape);
   }

   private static SimBlockShapes.Builder shape(double x1, double y1, double z1, double x2, double y2, double z2) {
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

      public SimBlockShapes.Builder add(VoxelShape shape) {
         this.shape = Shapes.or(this.shape, shape);
         return this;
      }

      public SimBlockShapes.Builder add(double x1, double y1, double z1, double x2, double y2, double z2) {
         return this.add(SimBlockShapes.cuboid(x1, y1, z1, x2, y2, z2));
      }

      public SimBlockShapes.Builder erase(double x1, double y1, double z1, double x2, double y2, double z2) {
         this.shape = Shapes.join(this.shape, SimBlockShapes.cuboid(x1, y1, z1, x2, y2, z2), BooleanOp.ONLY_FIRST);
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
