package dev.ryanhcode.sable.mixin.enchanting_table;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({EnchantingTableBlockEntity.class})
public class EnchantingTableBlockEntityMixin {
   @Redirect(
      method = {"bookAnimationTick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getX()D"
      )
   )
   private static double sable$getPlayerX(Player instance, @Local(argsOnly = true) BlockPos blockPos) {
      SubLevel subLevel = Sable.HELPER.getContaining(instance.level(), blockPos);
      return subLevel != null ? subLevel.logicalPose().transformPositionInverse(instance.getEyePosition()).x() : instance.getX();
   }

   @Redirect(
      method = {"bookAnimationTick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getZ()D"
      )
   )
   private static double sable$getPlayerZ(Player instance, @Local(argsOnly = true) BlockPos blockPos) {
      SubLevel subLevel = Sable.HELPER.getContaining(instance.level(), blockPos);
      return subLevel != null ? subLevel.logicalPose().transformPositionInverse(instance.getEyePosition()).z() : instance.getZ();
   }
}
