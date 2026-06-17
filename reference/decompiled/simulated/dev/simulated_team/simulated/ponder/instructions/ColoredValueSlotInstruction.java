package dev.simulated_team.simulated.ponder.instructions;

import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ColoredValueSlotInstruction extends PonderInstruction {
   public ColoredValueSlotInstruction(SceneBuilder scene, Vec3 location, Direction side, Vec3 rotation, PonderPalette color, int duration) {
      Vec3 vec = location.add(Vec3.atLowerCornerOf(side.getNormal()).scale(-0.0234375));
      Vec3 expands = VecHelper.axisAlingedPlaneOf(side).scale(0.0859375);
      AABB point = new AABB(vec, vec);
      AABB expanded = point.inflate(expands.x, expands.y, expands.z);
      scene.addInstruction(new OBBOutlineInstruction(expanded, rotation, false, color, expanded.toString(), duration));
   }

   public boolean isComplete() {
      return true;
   }

   public void tick(PonderScene scene) {
   }
}
