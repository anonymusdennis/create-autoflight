package dev.simulated_team.simulated.ponder.instructions;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.ParrotElement;
import net.minecraft.world.phys.Vec3;

public class CustomAnimateParrotInstruction extends CustomAnimateElementInstruction<ParrotElement> {
   protected CustomAnimateParrotInstruction(
      ElementLink<ParrotElement> link,
      Vec3 totalDelta,
      int ticks,
      BiConsumer<ParrotElement, Vec3> setter,
      Function<ParrotElement, Vec3> getter,
      FloatUnaryOperator positionFunc
   ) {
      super(link, totalDelta, ticks, setter, getter, positionFunc);
   }

   public static CustomAnimateParrotInstruction move(ElementLink<ParrotElement> link, Vec3 offset, int ticks, FloatUnaryOperator positionFunc) {
      return new CustomAnimateParrotInstruction(
         link, offset, ticks, (wse, v) -> wse.setPositionOffset(v, ticks == 0), ParrotElement::getPositionOffset, positionFunc
      );
   }
}
