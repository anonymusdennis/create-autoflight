package dev.ryanhcode.sable.neoforge.mixin.compatibility.sodiumextras;

import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import toni.sodiumextras.EmbyTools;

@Mixin({EmbyTools.class})
public class EmbyToolsMixin {
   @Overwrite
   public static boolean isEntityWithinDistance(BlockPos bePos, Vec3 camVec, int maxHeight, int maxDistanceSquare) {
      return Sable.HELPER
            .distanceSquaredWithSubLevels(
               Minecraft.getInstance().level, (double)bePos.getX() + 0.5, (double)bePos.getY() + 0.5, (double)bePos.getZ() + 0.5, camVec.x, camVec.y, camVec.z
            )
         < (double)maxDistanceSquare;
   }
}
