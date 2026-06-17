package net.createmod.ponder.api.scene;

import java.util.function.Consumer;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;

public interface DebugInstructions {
   void debugSchematic();

   void addInstructionInstance(PonderInstruction var1);

   void enqueueCallback(Consumer<PonderScene> var1);
}
