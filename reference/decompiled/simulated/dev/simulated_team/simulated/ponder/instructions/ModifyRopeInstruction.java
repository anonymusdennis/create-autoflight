package dev.simulated_team.simulated.ponder.instructions;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.ponder.SmoothMovementUtils;
import dev.simulated_team.simulated.ponder.elements.rope.PonderRopePose;
import dev.simulated_team.simulated.ponder.elements.rope.RopeStrandElement;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.world.phys.Vec3;

public class ModifyRopeInstruction extends TickingInstruction {
   private final RopeStrandElement rope;
   public PonderRopePose targetPose;
   public PonderRopePose currentPose = new PonderRopePose();
   public PonderRopePose startPose = new PonderRopePose();
   private FloatUnaryOperator interpolator = SmoothMovementUtils.linear();

   public ModifyRopeInstruction(int ticks, RopeStrandElement rope, Vec3 from, Vec3 to, double length, double sog, double floorDistance) {
      super(false, ticks);
      this.rope = rope;
      this.targetPose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, floorDistance);
   }

   public ModifyRopeInstruction(int duration, RopeStrandElement rope) {
      this(
         duration,
         rope,
         JOMLConversion.toMojang(rope.scenePose.start),
         JOMLConversion.toMojang(rope.scenePose.end),
         rope.scenePose.length,
         rope.scenePose.sog,
         rope.scenePose.floorHeight
      );
   }

   public ModifyRopeInstruction setStart(Vec3 start) {
      this.targetPose.start.set(JOMLConversion.toJOML(start));
      this.rope.scenePose.start.set(JOMLConversion.toJOML(start));
      return this;
   }

   public ModifyRopeInstruction setEnd(Vec3 end) {
      this.targetPose.end.set(JOMLConversion.toJOML(end));
      this.rope.scenePose.end.set(JOMLConversion.toJOML(end));
      return this;
   }

   public ModifyRopeInstruction setLength(double length) {
      this.targetPose.length = length;
      this.rope.scenePose.length = length;
      return this;
   }

   public ModifyRopeInstruction setSog(double sog) {
      this.targetPose.sog = sog;
      this.rope.scenePose.sog = sog;
      return this;
   }

   public ModifyRopeInstruction setInterpolator(FloatUnaryOperator interpolator) {
      this.interpolator = interpolator;
      return this;
   }

   public ModifyRopeInstruction start(CreateSceneBuilder scene) {
      scene.addInstruction(this);
      return this;
   }

   public void reset(PonderScene scene) {
      super.reset(scene);
   }

   protected void firstTick(PonderScene scene) {
      super.firstTick(scene);
      this.startPose.set(this.rope.pose);
      this.currentPose.set(this.rope.pose);
   }

   public void tick(PonderScene scene) {
      super.tick(scene);
      int ticks = this.totalTicks - this.remainingTicks;
      double t = (double)ticks / (double)this.totalTicks;
      t = (double)this.interpolator.apply((float)t);
      this.startPose.lerp(this.startPose, this.targetPose, this.currentPose, t);
      this.rope.set(this.currentPose);
      if (this.remainingTicks == 0) {
         this.rope.set(this.currentPose);
      }
   }
}
