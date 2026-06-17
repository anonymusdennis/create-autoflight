package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.vertical_gearbox;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.gearbox.VerticalGearboxItem;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({VerticalGearboxItem.class})
public class VerticalGearboxItemMixin {
   @Redirect(
      method = {"updateCustomBlockEntityTag"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getDirection()Lnet/minecraft/core/Direction;"
      )
   )
   private Direction sable$getDirection(Player player, @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) Level level) {
      SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
      if (subLevel != null) {
         SubLevelHelper.pushEntityLocal(subLevel, player);
         Direction dir = player.getDirection();
         SubLevelHelper.popEntityLocal(subLevel, player);
         return dir;
      } else {
         return player.getDirection();
      }
   }
}
