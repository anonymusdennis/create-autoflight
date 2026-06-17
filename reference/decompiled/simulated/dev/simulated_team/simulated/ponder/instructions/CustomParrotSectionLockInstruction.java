package dev.simulated_team.simulated.ponder.instructions;

import dev.simulated_team.simulated.mixin.accessor.WorldSectionElementAccessor;
import dev.simulated_team.simulated.util.SimMathUtils;
import java.util.Objects;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class CustomParrotSectionLockInstruction extends TickingInstruction {
   protected ElementLink<WorldSectionElement> link;
   protected ElementLink<ParrotElement> parrotLink;
   protected final Vec3 position;
   protected WorldSectionElement element;
   protected ParrotElement parrot;

   public CustomParrotSectionLockInstruction(ElementLink<WorldSectionElement> link, ElementLink<ParrotElement> parrotLink, Vec3 position, int ticks) {
      super(false, ticks);
      this.link = link;
      this.parrotLink = parrotLink;
      this.position = position;
   }

   protected void firstTick(PonderScene scene) {
      super.firstTick(scene);
      this.element = Objects.requireNonNull((WorldSectionElement)scene.resolve(this.link), "element");
      this.parrot = Objects.requireNonNull((ParrotElement)scene.resolve(this.parrotLink), "parrot");
   }

   public void tick(PonderScene scene) {
      super.tick(scene);
      Vec3 totalOffset = this.position;
      Quaternionf elementRot = new Quaternionf();
      if (this.link != null) {
         Vec3 elementOffset = this.element.getAnimatedOffset();
         Vec3 rotation = new Vec3(
            Math.toRadians(this.element.getAnimatedRotation().x),
            Math.toRadians(this.element.getAnimatedRotation().y),
            Math.toRadians(this.element.getAnimatedRotation().z)
         );
         elementRot.mul(new Quaternionf((float)Math.sin(rotation.x / 2.0), 0.0F, 0.0F, (float)Math.cos(rotation.x / 2.0)));
         elementRot.mul(new Quaternionf(0.0F, 0.0F, (float)Math.sin(rotation.z / 2.0), (float)Math.cos(rotation.z / 2.0)));
         elementRot.mul(new Quaternionf(0.0F, (float)Math.sin(rotation.y / 2.0), 0.0F, (float)Math.cos(rotation.y / 2.0)));
         totalOffset = totalOffset.subtract(((WorldSectionElementAccessor)this.element).getCenterOfRotation());
         totalOffset = SimMathUtils.rotateQuatReverse(totalOffset, elementRot);
         totalOffset = totalOffset.add(((WorldSectionElementAccessor)this.element).getCenterOfRotation());
         totalOffset = totalOffset.add(elementOffset);
      }

      this.parrot.setPositionOffset(totalOffset.subtract(this.position), this.remainingTicks >= this.totalTicks - 2);
   }
}
