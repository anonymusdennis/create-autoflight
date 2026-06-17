package dev.simulated_team.simulated.ponder.instructions;

import java.util.Objects;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ChaseAABBWithLinkInstruction extends TickingInstruction {
   protected final ElementLink<WorldSectionElement> elementLink;
   protected WorldSectionElement element;
   protected final AABB bb;
   protected final Object slot;
   protected final PonderPalette color;
   protected double timeShift;
   protected Vec3 previousOffset;

   public ChaseAABBWithLinkInstruction(ElementLink<WorldSectionElement> elementLink, PonderPalette color, Object slot, AABB bb, int ticks, double timeShift) {
      super(false, ticks);
      this.elementLink = elementLink;
      this.color = color;
      this.slot = slot;
      this.bb = bb;
      this.timeShift = timeShift;
   }

   protected final void firstTick(PonderScene scene) {
      super.firstTick(scene);
      this.element = Objects.requireNonNull((WorldSectionElement)scene.resolve(this.elementLink), "elementLink");
      this.previousOffset = this.element.getAnimatedOffset();
   }

   public void tick(PonderScene scene) {
      super.tick(scene);
      Vec3 offset = this.element.getAnimatedOffset();
      Vec3 shiftedOffset = this.previousOffset.lerp(offset, this.timeShift);
      this.previousOffset = offset;
      AABB offsetBB = this.bb.move(shiftedOffset);
      scene.getOutliner().chaseAABB(this.slot, offsetBB).lineWidth(0.0625F).colored(this.color.getColor());
   }
}
