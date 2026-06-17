package dev.simulated_team.simulated.content.blocks.handle;

import dev.simulated_team.simulated.index.SimBlockShapes;
import java.util.Arrays;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HandleShaper extends VoxelShaper {
   private VoxelShaper axisFalse;
   private VoxelShaper axisTrue;

   public static HandleShaper make() {
      HandleShaper shaper = new HandleShaper();
      shaper.axisFalse = forDirectional(SimBlockShapes.HANDLE, Direction.UP);
      shaper.axisTrue = forDirectional(rotatedCopy(SimBlockShapes.HANDLE, new Vec3(0.0, 90.0, 0.0)), Direction.UP);
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
