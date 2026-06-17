package dev.simulated_team.simulated.ponder.instructions;

import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlockEntity;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.core.BlockPos;

public class GimbalSensorVisualRotationInstruction extends PonderInstruction {
   BlockPos location;
   Boolean unlocked;

   public GimbalSensorVisualRotationInstruction(BlockPos location, Boolean unlocked) {
      this.location = location;
      this.unlocked = unlocked;
   }

   public boolean isComplete() {
      return false;
   }

   public void tick(PonderScene scene) {
      PonderLevel world = scene.getWorld();
      if (world.getBlockEntity(this.location) instanceof GimbalSensorBlockEntity be) {
         be.updateVisualRotation = this.unlocked;
      }
   }
}
