package dev.engine_room.flywheel.impl.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.impl.extension.PoseStackExtension;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({PoseStack.class})
abstract class PoseStackMixin implements PoseStackExtension {
   @Unique
   private PoseTransformStack flywheel$wrapper;

   @Override
   public PoseTransformStack flywheel$transformStack() {
      if (this.flywheel$wrapper == null) {
         this.flywheel$wrapper = new PoseTransformStack((PoseStack)this);
      }

      return this.flywheel$wrapper;
   }
}
