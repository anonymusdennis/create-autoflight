package dev.eriksonn.aeronautics.mixin.ponder;

import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.createmod.ponder.foundation.instruction.DisplayWorldSectionInstruction;
import net.createmod.ponder.foundation.instruction.FadeIntoSceneInstruction;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DisplayWorldSectionInstruction.class})
public abstract class DisplayWorldSectionInstructionMixin extends FadeIntoSceneInstruction<WorldSectionElement> {
   @Shadow
   @Final
   @Nullable
   private Supplier<WorldSectionElement> mergeOnto;

   public DisplayWorldSectionInstructionMixin(int fadeInTicks, Direction fadeInFrom, WorldSectionElement element) {
      super(fadeInTicks, fadeInFrom, element);
   }

   @Inject(
      method = {"firstTick"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/Optional;ofNullable(Ljava/lang/Object;)Ljava/util/Optional;"
      )}
   )
   public void firstTick(PonderScene scene, CallbackInfo ci) {
      Optional.ofNullable(this.mergeOnto).ifPresent(wse -> {
         WorldSectionElement e = wse.get();
         ((WorldSectionElement)this.element).setAnimatedRotation(e.getAnimatedRotation(), true);
         if (e instanceof WorldSectionElementImpl impl) {
            ((WorldSectionElement)this.element).setCenterOfRotation(((WorldSectionElementImplAccessor)impl).getCenterOfRotation());
         }
      });
   }
}
