package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.super_glue;

import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHandler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SuperGlueSelectionHandler.class})
public class SuperGlueSelectionHandlerMixin {
   @Redirect(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/contraptions/glue/SuperGlueEntity;getBoundingBox()Lnet/minecraft/world/phys/AABB;",
         ordinal = 0
      )
   )
   private AABB sable$projectBoundingBox(SuperGlueEntity instance) {
      SubLevel subLevel = Sable.HELPER.getContaining(instance);
      if (subLevel != null) {
         BoundingBox3d bb = new BoundingBox3d(instance.getBoundingBox());
         return bb.transform(subLevel.logicalPose(), bb).toMojang();
      } else {
         return instance.getBoundingBox();
      }
   }
}
