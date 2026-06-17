package dev.ryanhcode.sable.mixin.player_standup;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.mixinhelpers.CanFallAtleastHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Player.class})
public class PlayerMixin {
   @WrapOperation(
      method = {"canPlayerFitWithinBlocksAndEntitiesWhen"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z"
      )}
   )
   private boolean sable$noCollisionWithSubLevels(Level instance, Entity entity, AABB aabb, Operation<Boolean> original) {
      return !original.call(new Object[]{instance, entity, aabb}) ? false : CanFallAtleastHelper.canFallAtleastWithSubLevels(instance, aabb) == null;
   }
}
