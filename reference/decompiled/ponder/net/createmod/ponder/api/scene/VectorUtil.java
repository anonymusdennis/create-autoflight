package net.createmod.ponder.api.scene;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public interface VectorUtil {
   Vec3 centerOf(int var1, int var2, int var3);

   Vec3 centerOf(BlockPos var1);

   Vec3 topOf(int var1, int var2, int var3);

   Vec3 topOf(BlockPos var1);

   Vec3 blockSurface(BlockPos var1, Direction var2);

   Vec3 blockSurface(BlockPos var1, Direction var2, float var3);

   Vec3 of(double var1, double var3, double var5);
}
