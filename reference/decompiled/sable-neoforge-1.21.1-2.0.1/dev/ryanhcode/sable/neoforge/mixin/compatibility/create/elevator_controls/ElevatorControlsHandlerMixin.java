package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.elevator_controls;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorControlsHandler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ElevatorControlsHandler.class})
public class ElevatorControlsHandlerMixin {
   @Redirect(
      method = {"onScroll"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"
      )
   )
   private static AABB sable$projectAABB(AbstractContraptionEntity instance) {
      SubLevel subLevel = Sable.HELPER.getContaining(instance.level(), instance.getBoundingBox().getCenter());
      AABB projectedBB = instance.getBoundingBox();
      if (subLevel != null) {
         BoundingBox3d bb = new BoundingBox3d(projectedBB);
         return bb.transform(subLevel.logicalPose(), bb).toMojang();
      } else {
         return projectedBB;
      }
   }
}
