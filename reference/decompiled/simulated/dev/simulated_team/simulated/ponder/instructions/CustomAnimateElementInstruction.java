package dev.simulated_team.simulated.ponder.instructions;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.PonderSceneElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.world.phys.Vec3;

public class CustomAnimateElementInstruction<T extends PonderSceneElement> extends TickingInstruction {
   protected Vec3 deltaPerTick;
   protected Vec3 totalDelta;
   protected Vec3 target;
   protected ElementLink<T> link;
   protected T element;
   private final BiConsumer<T, Vec3> setter;
   private final Function<T, Vec3> getter;
   private final UnaryOperator<Float> positionFunc;

   protected CustomAnimateElementInstruction(
      ElementLink<T> link, Vec3 totalDelta, int ticks, BiConsumer<T, Vec3> setter, Function<T, Vec3> getter, UnaryOperator<Float> positionFunc
   ) {
      super(false, ticks);
      this.link = link;
      this.setter = setter;
      this.getter = getter;
      this.totalDelta = totalDelta;
      this.deltaPerTick = totalDelta.scale(1.0 / (double)ticks);
      this.target = totalDelta;
      this.positionFunc = positionFunc;
   }

   protected final void firstTick(PonderScene scene) {
      super.firstTick(scene);
      this.element = (T)scene.resolve(this.link);
      if (this.element != null) {
         this.target = this.getter.apply(this.element).add(this.totalDelta);
      }
   }

   public void tick(PonderScene scene) {
      super.tick(scene);
      if (this.element != null) {
         if (this.totalTicks == 0) {
            this.setter.accept(this.element, this.getter.apply(this.element).add(this.totalDelta.scale(1.0)));
         } else {
            int time = this.totalTicks - this.remainingTicks - 1;
            float P1 = this.positionFunc.apply(Float.valueOf((float)time / (float)this.totalTicks));
            float P2 = this.positionFunc.apply(Float.valueOf((float)(time + 1) / (float)this.totalTicks));
            float delta = P2 - P1;
            this.setter.accept(this.element, this.getter.apply(this.element).add(this.totalDelta.scale((double)delta)));
         }
      }
   }
}
