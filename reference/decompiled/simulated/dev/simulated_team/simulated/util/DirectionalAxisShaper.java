package dev.simulated_team.simulated.util;

import java.util.Arrays;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DirectionalAxisShaper extends VoxelShaper {
   private VoxelShaper axisFalse;
   private VoxelShaper axisTrue;

   public static DirectionalAxisShaper make(VoxelShape shape) {
      DirectionalAxisShaper shaper = new DirectionalAxisShaper();
      shaper.axisFalse = forDirectional(shape, Direction.UP);
      shaper.axisTrue = forDirectional(rotatedCopy(shape, new Vec3(0.0, 90.0, 0.0)), Direction.UP);
      Arrays.asList(Direction.EAST, Direction.WEST).forEach(direction -> {
         VoxelShape mem = shaper.axisFalse.get(direction);
         shaper.axisFalse.withShape(shaper.axisTrue.get(direction), direction);
         shaper.axisTrue.withShape(mem, direction);
      });
      return shaper;
   }

   public VoxelShape get(Direction direction, boolean axisAlong) {
      return (axisAlong ? this.axisTrue : this.axisFalse).get(direction);
   }
}
