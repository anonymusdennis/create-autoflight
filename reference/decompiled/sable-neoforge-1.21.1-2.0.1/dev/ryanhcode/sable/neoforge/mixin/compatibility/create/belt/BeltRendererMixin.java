package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.belt;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({BeltRenderer.class})
public class BeltRendererMixin {
   @ModifyExpressionValue(
      method = {"renderItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;getPosition()Lnet/minecraft/world/phys/Vec3;"
      )}
   )
   private Vec3 sable$renderViewEntityPosition(Vec3 original, @Local(argsOnly = true) BeltBlockEntity be) {
      ClientSubLevel subLevel = Sable.HELPER.getContainingClient(be);
      return subLevel != null ? subLevel.renderPose().transformPositionInverse(original) : original;
   }
}
