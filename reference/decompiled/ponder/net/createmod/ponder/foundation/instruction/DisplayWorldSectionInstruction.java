package net.createmod.ponder.foundation.instruction;

import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class DisplayWorldSectionInstruction extends FadeIntoSceneInstruction<WorldSectionElement> {
   private final Selection initialSelection;
   @Nullable
   private final Supplier<WorldSectionElement> mergeOnto;
   private final BlockPos glue;

   public DisplayWorldSectionInstruction(int fadeInTicks, Direction fadeInFrom, Selection selection, @Nullable Supplier<WorldSectionElement> mergeOnto) {
      this(fadeInTicks, fadeInFrom, selection, mergeOnto, null);
   }

   public DisplayWorldSectionInstruction(
      int fadeInTicks, Direction fadeInFrom, Selection selection, @Nullable Supplier<WorldSectionElement> mergeOnto, @Nullable BlockPos glue
   ) {
      super(fadeInTicks, fadeInFrom, new WorldSectionElementImpl(selection));
      this.initialSelection = selection;
      this.mergeOnto = mergeOnto;
      this.glue = glue;
   }

   @Override
   protected void firstTick(PonderScene scene) {
      super.firstTick(scene);
      Optional.ofNullable(this.mergeOnto).ifPresent(wse -> this.element.setAnimatedOffset(wse.get().getAnimatedOffset(), true));
      this.element.set(this.initialSelection);
      this.element.setVisible(true);
   }

   @Override
   public void tick(PonderScene scene) {
      super.tick(scene);
      if (this.remainingTicks <= 0) {
         Optional.ofNullable(this.mergeOnto).ifPresent(c -> this.element.mergeOnto(c.get()));
      }
   }

   @Override
   protected Class<WorldSectionElement> getElementClass() {
      return WorldSectionElement.class;
   }
}
