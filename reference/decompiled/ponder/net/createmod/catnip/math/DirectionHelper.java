package net.createmod.catnip.math;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

public class DirectionHelper {
   public static Direction rotateAround(Direction dir, Axis axis) {
      switch (axis) {
         case X:
            if (dir != Direction.WEST && dir != Direction.EAST) {
               return rotateX(dir);
            }

            return dir;
         case Y:
            if (dir != Direction.UP && dir != Direction.DOWN) {
               return dir.getClockWise();
            }

            return dir;
         case Z:
            if (dir != Direction.NORTH && dir != Direction.SOUTH) {
               return rotateZ(dir);
            }

            return dir;
         default:
            throw new IllegalStateException("Unable to get CW facing for axis " + axis);
      }
   }

   public static Direction rotateX(Direction dir) {
      return switch (dir) {
         case NORTH -> Direction.DOWN;
         case EAST, WEST -> throw new IllegalStateException("Unable to get X-rotated facing of " + dir);
         case SOUTH -> Direction.UP;
         case UP -> Direction.NORTH;
         case DOWN -> Direction.SOUTH;
         default -> throw new MatchException(null, null);
      };
   }

   public static Direction rotateZ(Direction dir) {
      return switch (dir) {
         case NORTH, SOUTH -> throw new IllegalStateException("Unable to get Z-rotated facing of " + dir);
         case EAST -> Direction.DOWN;
         case WEST -> Direction.UP;
         case UP -> Direction.EAST;
         case DOWN -> Direction.WEST;
         default -> throw new MatchException(null, null);
      };
   }

   public static Direction getPositivePerpendicular(Axis horizontalAxis) {
      return horizontalAxis == Axis.X ? Direction.SOUTH : Direction.EAST;
   }
}
