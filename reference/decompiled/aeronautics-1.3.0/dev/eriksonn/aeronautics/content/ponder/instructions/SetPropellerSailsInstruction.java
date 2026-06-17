package dev.eriksonn.aeronautics.content.ponder.instructions;

import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.core.BlockPos;

public class SetPropellerSailsInstruction extends PonderInstruction {
   BlockPos pos;
   float sails;

   public SetPropellerSailsInstruction(BlockPos pos, float sails) {
      this.pos = pos;
      this.sails = sails;
   }

   public boolean isComplete() {
      return true;
   }

   public void tick(PonderScene scene) {
      if (scene.getWorld().getBlockEntity(this.pos) instanceof PropellerBearingBlockEntity propeller) {
         propeller.totalSailPower = this.sails;
      }
   }
}
