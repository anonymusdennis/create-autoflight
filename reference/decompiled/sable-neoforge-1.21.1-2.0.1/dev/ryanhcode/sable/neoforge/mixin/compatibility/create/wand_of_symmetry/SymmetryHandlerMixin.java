package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.wand_of_symmetry;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryHandler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.util.SublevelRenderOffsetHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SymmetryHandler.class})
public class SymmetryHandlerMixin {
   @Redirect(
      method = {"onRenderWorld"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V",
         ordinal = 0
      )
   )
   private static void accountForSublevels(PoseStack instance, double f, double g, double h, @Local(name = {"pos"}) BlockPos pos) {
      SublevelRenderOffsetHelper.posePlotToProjected(Sable.HELPER.getContainingClient(pos), instance);
      Vec3 translation = SublevelRenderOffsetHelper.translation(Vec3.atLowerCornerOf(pos));
      instance.translate(f - translation.x, g - translation.y, h - translation.z);
   }
}
