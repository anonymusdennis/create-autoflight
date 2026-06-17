package dev.simulated_team.simulated.ponder.instructions;

import dev.simulated_team.simulated.mixin_interface.ponder.PonderSceneExtension;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.world.phys.Vec3;

public class CustomMoveBaseShadowInstruction extends TickingInstruction {
   protected Vec3 previousPos;
   protected Function<Vec3, Function<Float, Vec3>> initialPosFunc;
   protected Function<Float, Vec3> posFunc;

   protected CustomMoveBaseShadowInstruction(Function<Vec3, Function<Float, Vec3>> initialPosFunc, int ticks) {
      super(false, ticks);
      this.initialPosFunc = initialPosFunc;
   }

   public static CustomMoveBaseShadowInstruction delta(Vec3 delta, int ticks, UnaryOperator<Float> interpolation) {
      return new CustomMoveBaseShadowInstruction(v -> f -> {
            float i = interpolation.apply(f);
            return delta.scale((double)i);
         }, ticks);
   }

   public static CustomMoveBaseShadowInstruction to(Vec3 target, int ticks, UnaryOperator<Float> interpolation) {
      return new CustomMoveBaseShadowInstruction(v -> f -> {
            float i = interpolation.apply(f);
            return target.subtract(v).scale((double)i).add(v.scale((double)(1.0F - i)));
         }, ticks);
   }

   public static CustomMoveBaseShadowInstruction to(Vec3 target) {
      return new CustomMoveBaseShadowInstruction(v -> f -> target.subtract(v), 1);
   }

   protected void firstTick(PonderScene scene) {
      super.firstTick(scene);
      this.previousPos = Vec3.ZERO;
      this.posFunc = this.initialPosFunc.apply(((PonderSceneExtension)scene).simulated$getShadowOffset(0.0F));
   }

   public void tick(PonderScene scene) {
      super.tick(scene);
      float f = 1.0F - (float)this.remainingTicks / (float)this.totalTicks;
      Vec3 pos = this.posFunc.apply(f);
      if (this.totalTicks <= 1) {
         ((PonderSceneExtension)scene).simulated$setShadowOffset(pos);
         ((PonderSceneExtension)scene).simulated$setOldShadowOffset(pos);
      } else {
         ((PonderSceneExtension)scene).simulated$moveShadowOffset(pos.subtract(this.previousPos));
      }

      this.previousPos = pos;
   }
}
