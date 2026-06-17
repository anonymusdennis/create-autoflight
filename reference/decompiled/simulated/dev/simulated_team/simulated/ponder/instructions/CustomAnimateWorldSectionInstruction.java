package dev.simulated_team.simulated.ponder.instructions;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.minecraft.world.phys.Vec3;

public class CustomAnimateWorldSectionInstruction extends CustomAnimateElementInstruction<WorldSectionElement> {
   protected CustomAnimateWorldSectionInstruction(
      ElementLink<WorldSectionElement> link,
      Vec3 totalDelta,
      int ticks,
      BiConsumer<WorldSectionElement, Vec3> setter,
      Function<WorldSectionElement, Vec3> getter,
      FloatUnaryOperator positionFunc
   ) {
      super(link, totalDelta, ticks, setter, getter, positionFunc);
   }

   public static CustomAnimateWorldSectionInstruction rotate(ElementLink<WorldSectionElement> link, Vec3 rotation, int ticks, FloatUnaryOperator positionFunc) {
      return new CustomAnimateWorldSectionInstruction(
         link, rotation, ticks, (wse, v) -> wse.setAnimatedRotation(v, ticks == 0), WorldSectionElement::getAnimatedRotation, positionFunc
      );
   }

   public static CustomAnimateWorldSectionInstruction move(ElementLink<WorldSectionElement> link, Vec3 offset, int ticks, FloatUnaryOperator positionFunc) {
      return new CustomAnimateWorldSectionInstruction(
         link, offset, ticks, (wse, v) -> wse.setAnimatedOffset(v, ticks == 0), WorldSectionElement::getAnimatedOffset, positionFunc
      );
   }
}
