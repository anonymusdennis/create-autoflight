package dev.ryanhcode.sable.mixin.entity.entity_pathfinding;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.entity.pathfinding.PathExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Path.class})
public class PathMixin implements PathExtension {
   @Unique
   private Level sable$level;
   @Unique
   private boolean sable$project;

   @Inject(
      method = {"getNextEntityPos"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void sable$getNextEntityPos(Entity entity, CallbackInfoReturnable<Vec3> cir) {
      if (this.sable$project) {
         cir.setReturnValue(Sable.HELPER.projectOutOfSubLevel(entity.level(), (Vec3)cir.getReturnValue()));
      }
   }

   @Inject(
      method = {"getNextNodePos"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void sable$getNextNodePos(CallbackInfoReturnable<BlockPos> cir) {
      if (this.sable$project) {
         BlockPos blockPos = (BlockPos)cir.getReturnValue();
         SubLevel subLevel = Sable.HELPER.getContaining(this.sable$level, blockPos);
         if (subLevel != null) {
            BlockPos global = BlockPos.containing(subLevel.logicalPose().transformPosition(blockPos.getCenter()));
            cir.setReturnValue(global);
         }
      }
   }

   @Inject(
      method = {"getNodePos"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void sable$getNodePos(int i, CallbackInfoReturnable<BlockPos> cir) {
      if (this.sable$project) {
         BlockPos blockPos = (BlockPos)cir.getReturnValue();
         SubLevel subLevel = Sable.HELPER.getContaining(this.sable$level, blockPos);
         if (subLevel != null) {
            BlockPos global = BlockPos.containing(subLevel.logicalPose().transformPosition(blockPos.getCenter()));
            cir.setReturnValue(global);
         }
      }
   }

   @Override
   public void sable$setLocalPath(Level level, boolean project) {
      this.sable$level = level;
      this.sable$project = project;
   }
}
