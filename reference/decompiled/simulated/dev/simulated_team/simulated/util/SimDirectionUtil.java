package dev.simulated_team.simulated.util;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;

public class SimDirectionUtil {
   public static final Direction[] VALUES = Direction.values();
   public static Direction[] X_AXIS_PLANE = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP};
   public static Direction[] Y_AXIS_PLANE = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
   public static Direction[] Z_AXIS_PLANE = new Direction[]{Direction.DOWN, Direction.UP, Direction.EAST, Direction.WEST};
   public static BlockPos[] CUBIC_OFFSET = BlockPos.betweenClosedStream(-1, -1, -1, 1, 1, 1).map(BlockPos::immutable).toArray(BlockPos[]::new);

   public static Direction[] getSurroundingDirections(Axis axis) {
      return switch (axis) {
         case X -> X_AXIS_PLANE;
         case Y -> Y_AXIS_PLANE;
         case Z -> Z_AXIS_PLANE;
         default -> throw new MatchException(null, null);
      };
   }

   public static Direction[] getDirectionsExcept(Direction dirToIgnore) {
      return Arrays.stream(Direction.values()).filter(d -> d != dirToIgnore).toArray(Direction[]::new);
   }

   public static Direction directionFromNormal(Vec3i normal) {
      for (Direction dir : Direction.values()) {
         if (dir.getNormal().equals(normal)) {
            return dir;
         }
      }

      return Direction.UP;
   }
}
