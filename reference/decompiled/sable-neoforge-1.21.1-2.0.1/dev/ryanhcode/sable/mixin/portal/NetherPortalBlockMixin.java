package dev.ryanhcode.sable.mixin.portal;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({NetherPortalBlock.class})
public class NetherPortalBlockMixin {
   @Redirect(
      method = {"getPortalDestination"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/border/WorldBorder;clampToBounds(DDD)Lnet/minecraft/core/BlockPos;"
      )
   )
   private BlockPos sable$getPortalDestination(
      WorldBorder instance, double x, double y, double z, @Local(argsOnly = true) Entity entity, @Local(ordinal = 0) double multiplier
   ) {
      Vec3 position = new Vec3(entity.getX(), entity.getY(), entity.getZ());
      Vec3 globalPos = Sable.HELPER.projectOutOfSubLevel(entity.level(), position);
      return instance.clampToBounds(globalPos.x * multiplier, globalPos.y, globalPos.z * multiplier);
   }
}
