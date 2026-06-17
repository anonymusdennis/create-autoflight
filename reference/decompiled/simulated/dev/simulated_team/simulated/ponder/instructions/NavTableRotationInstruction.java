package dev.simulated_team.simulated.ponder.instructions;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.core.BlockPos;

public class NavTableRotationInstruction extends TickingInstruction {
   protected final BlockPos location;
   protected final int ticks;
   protected final int angle;
   protected float initialAngle;
   protected int progress;
   protected WorldSectionElement element;

   public NavTableRotationInstruction(BlockPos location, int angle, int ticks) {
      super(false, ticks);
      this.location = location;
      this.angle = angle;
      this.ticks = ticks;
   }

   protected void firstTick(PonderScene scene) {
      super.firstTick(scene);
      this.progress = 0;
      if (scene.getWorld().getBlockEntity(this.location) instanceof NavTableBlockEntity nbe) {
         this.initialAngle = nbe.lerpedAngleDegrees.getValue();
      }
   }

   public void tick(PonderScene scene) {
      super.tick(scene);
      this.progress++;
      float lerpedValue = (float)this.progress / (float)this.ticks;
      PonderLevel level = scene.getWorld();
      if (level.getBlockEntity(this.location) instanceof NavTableBlockEntity nbe) {
         nbe.forceCurrentAngle(this.initialAngle + (float)this.angle * lerpedValue);
      }
   }
}
