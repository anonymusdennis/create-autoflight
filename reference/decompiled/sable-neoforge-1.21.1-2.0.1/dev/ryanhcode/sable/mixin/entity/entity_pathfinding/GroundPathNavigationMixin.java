package dev.ryanhcode.sable.mixin.entity.entity_pathfinding;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GroundPathNavigation.class})
public abstract class GroundPathNavigationMixin {
   @Shadow
   public abstract Path createPath(BlockPos var1, int var2);

   @Inject(
      method = {"createPath(Lnet/minecraft/world/entity/Entity;I)Lnet/minecraft/world/level/pathfinder/Path;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$createPath(Entity entity, int i, CallbackInfoReturnable<Path> cir) {
      SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(entity);
      if (trackingSubLevel != null) {
         BlockPos localPos = BlockPos.containing(trackingSubLevel.logicalPose().transformPositionInverse(entity.position()));
         cir.setReturnValue(this.createPath(localPos, i));
      }
   }
}
