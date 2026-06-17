package dev.simulated_team.simulated.ponder.instructions;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AirflowAABBInstruction extends TickingInstruction {
   protected AABB bb;
   protected PonderPalette color;
   protected int hash;
   protected Direction direction;
   protected float speed;
   protected float spacing;
   protected boolean easeIn;
   protected boolean easeOut;

   public AirflowAABBInstruction(PonderPalette color, AABB bb, int ticks, Direction direction, float speed, float spacing) {
      this(color, bb, ticks, direction, speed, spacing, true, false);
   }

   public AirflowAABBInstruction(PonderPalette color, AABB bb, int ticks, Direction direction, float speed, float spacing, boolean easeIn, boolean easeOut) {
      super(false, ticks);
      this.bb = bb;
      this.color = color;
      this.speed = speed / 20.0F;
      this.spacing = spacing;
      this.direction = direction;
      this.easeIn = easeIn;
      this.easeOut = easeOut;
   }

   protected final void firstTick(PonderScene scene) {
      super.firstTick(scene);
      this.hash = scene.getWorld().random.nextInt();
   }

   public void tick(PonderScene scene) {
      super.tick(scene);
      int age = this.totalTicks - this.remainingTicks;
      float offset = this.speed * (float)age;
      float totalOffset = (float)this.totalTicks * this.speed;
      double length = Vec3.atLowerCornerOf(this.direction.getNormal()).dot(new Vec3(this.bb.getXsize(), this.bb.getYsize(), this.bb.getZsize()));
      length = Math.abs(length);
      AABB commonBB = this.bb
         .contract((double)this.direction.getStepX() * length, (double)this.direction.getStepY() * length, (double)this.direction.getStepZ() * length);
      int startIndex = (int)Math.ceil((this.easeIn ? Math.max((double)offset - length, 0.0) : (double)offset - length) / (double)this.spacing);
      int endIndex = (int)Math.floor((this.easeOut ? Math.min((double)offset, (double)totalOffset - length) : (double)offset) / (double)this.spacing);

      for (int i = startIndex; i <= endIndex; i++) {
         double position = (double)(offset - (float)i * this.spacing);
         AABB currentBB = commonBB.move(Vec3.atLowerCornerOf(this.direction.getNormal()).scale(position));
         scene.getOutliner().chaseAABB(this.hash + i, currentBB).lineWidth(0.03125F).colored(this.color.getColor());
      }
   }
}
