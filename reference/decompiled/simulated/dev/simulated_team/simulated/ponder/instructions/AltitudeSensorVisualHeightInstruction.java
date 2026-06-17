package dev.simulated_team.simulated.ponder.instructions;

import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.core.BlockPos;

public abstract class AltitudeSensorVisualHeightInstruction extends TickingInstruction {
   protected final BlockPos location;
   protected final float startValue;
   protected final float endValue;
   protected final FloatUnaryOperator interpolation;

   public AltitudeSensorVisualHeightInstruction(BlockPos location, int ticks, float startValue, float endValue, FloatUnaryOperator interpolation) {
      super(false, ticks);
      this.location = location;
      this.startValue = startValue;
      this.endValue = endValue;
      this.interpolation = interpolation;
   }

   public float getLerpedValue() {
      return this.totalTicks != 0
         ? this.startValue + (this.endValue - this.startValue) * this.interpolation.apply(1.0F - (float)this.remainingTicks / (float)this.totalTicks)
         : this.endValue;
   }

   public static class Linear extends AltitudeSensorVisualHeightInstruction {
      public Linear(BlockPos location, int ticks, float startValue, float endValue, FloatUnaryOperator interpolation) {
         super(location, ticks, startValue, endValue, interpolation);
      }

      protected void firstTick(PonderScene scene) {
         super.firstTick(scene);
         PonderLevel world = scene.getWorld();
         if (world.getBlockEntity(this.location) instanceof AltitudeSensorBlockEntity be) {
            be.updateVisualHeight = true;
            be.previousVisualHeight = (float)this.location.getY();
            be.visualHeight = (float)this.location.getY();
         }
      }

      public void tick(PonderScene scene) {
         super.tick(scene);
         PonderLevel world = scene.getWorld();
         if (world.getBlockEntity(this.location) instanceof AltitudeSensorBlockEntity be) {
            float targetValue = this.getLerpedValue();
            be.lowSignal = be.toNormalHeight((float)this.location.getY() - targetValue);
            be.highSignal = be.toNormalHeight((float)this.location.getY() - targetValue + 1.0F);
         }
      }
   }

   public static class Radial extends AltitudeSensorVisualHeightInstruction {
      public Radial(BlockPos location, int ticks, float startValue, float endValue, FloatUnaryOperator interpolation) {
         super(location, ticks, startValue, endValue, interpolation);
      }

      protected void firstTick(PonderScene scene) {
         super.firstTick(scene);
         PonderLevel world = scene.getWorld();
         if (world.getBlockEntity(this.location) instanceof AltitudeSensorBlockEntity be) {
            be.updateVisualHeight = false;
            be.previousVisualHeight = this.startValue;
            be.visualHeight = this.endValue;
         }
      }

      public void tick(PonderScene scene) {
         super.tick(scene);
         PonderLevel world = scene.getWorld();
         if (world.getBlockEntity(this.location) instanceof AltitudeSensorBlockEntity be) {
            float targetValue = this.getLerpedValue();
            be.visualHeight = targetValue;
         }
      }
   }
}
