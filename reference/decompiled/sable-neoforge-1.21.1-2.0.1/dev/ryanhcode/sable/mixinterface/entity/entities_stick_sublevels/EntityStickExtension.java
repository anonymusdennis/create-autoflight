package dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface EntityStickExtension {
   void sable$plotLerpTo(Vec3 var1, int var2);

   void sable$setPlotPosition(@Nullable Vec3 var1);

   @Nullable
   Vec3 sable$getPlotPosition();
}
