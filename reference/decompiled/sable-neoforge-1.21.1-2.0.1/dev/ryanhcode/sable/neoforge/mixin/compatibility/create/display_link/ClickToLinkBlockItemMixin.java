package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.display_link;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.redstone.displayLink.ClickToLinkBlockItem;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ClickToLinkBlockItem.class})
public class ClickToLinkBlockItemMixin {
   @WrapOperation(
      method = {"useOn"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"
      )}
   )
   public boolean sable$accountForSubLevels(BlockPos instance, Vec3i pos, double v, Operation<Boolean> original, @Local Level level) {
      return Sable.HELPER
            .distanceSquaredWithSubLevels(
               level,
               (double)instance.getX() + 0.5,
               (double)instance.getY() + 0.5,
               (double)instance.getZ() + 0.5,
               (double)pos.getX() + 0.5,
               (double)pos.getY() + 0.5,
               (double)pos.getZ() + 0.5
            )
         < v * v;
   }
}
