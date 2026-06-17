package dev.eriksonn.aeronautics.content.ponder.instructions;

import dev.eriksonn.aeronautics.mixinterface.TickingInstructionExtension;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.instruction.TickingInstruction;

public class TickingStoppingInstruction extends PonderInstruction {
   final TickingInstruction instruction;

   public TickingStoppingInstruction(TickingInstruction instruction) {
      this.instruction = instruction;
   }

   public boolean isComplete() {
      return true;
   }

   public void tick(PonderScene scene) {
      ((TickingInstructionExtension)this.instruction).aeronautics$stopInstruction();
   }
}
