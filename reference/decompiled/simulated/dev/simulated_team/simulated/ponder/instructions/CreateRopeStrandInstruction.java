package dev.simulated_team.simulated.ponder.instructions;

import dev.simulated_team.simulated.ponder.elements.rope.RopeStrandElement;
import net.createmod.ponder.foundation.instruction.FadeIntoSceneInstruction;
import net.minecraft.core.Direction;

public class CreateRopeStrandInstruction extends FadeIntoSceneInstruction<RopeStrandElement> {
   public CreateRopeStrandInstruction(int fadeInTicks, Direction fadeInFrom, RopeStrandElement element) {
      super(fadeInTicks, fadeInFrom, element);
   }

   public CreateRopeStrandInstruction(RopeStrandElement element) {
      super(0, Direction.DOWN, element);
   }

   protected Class<RopeStrandElement> getElementClass() {
      return RopeStrandElement.class;
   }
}
