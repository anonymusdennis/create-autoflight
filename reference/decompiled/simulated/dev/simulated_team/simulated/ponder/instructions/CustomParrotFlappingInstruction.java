package dev.simulated_team.simulated.ponder.instructions;

import dev.simulated_team.simulated.mixin.accessor.ParrotElementAccessor;
import java.util.Objects;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Parrot;

public class CustomParrotFlappingInstruction extends TickingInstruction {
   protected final ElementLink<ParrotElement> parrotLink;
   protected final float speed;
   protected final boolean shouldGround;
   protected ParrotElement parrot;

   public CustomParrotFlappingInstruction(ElementLink<ParrotElement> parrotLink, float speed, int ticks) {
      super(false, ticks);
      this.parrotLink = parrotLink;
      this.speed = speed;
      this.shouldGround = false;
   }

   public CustomParrotFlappingInstruction(ElementLink<ParrotElement> parrotLink) {
      super(false, 0);
      this.parrotLink = parrotLink;
      this.speed = 0.0F;
      this.shouldGround = true;
   }

   protected void firstTick(PonderScene scene) {
      super.firstTick(scene);
      this.parrot = Objects.requireNonNull((ParrotElement)scene.resolve(this.parrotLink), "parrot");
   }

   public void tick(PonderScene scene) {
      super.tick(scene);
      Parrot entity = ((ParrotElementAccessor)this.parrot).getEntity();
      if (!this.shouldGround) {
         entity.setOnGround(false);
         entity.flapSpeed = Mth.sin((float)(PonderUI.ponderTicks % 100) * this.speed) + 1.0F;
      } else {
         entity.setOnGround(true);
         entity.flapSpeed = 0.0F;
      }
   }
}
