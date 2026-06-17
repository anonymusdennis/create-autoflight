package net.createmod.ponder.api.scene;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

public interface SelectionUtil {
   Selection everywhere();

   Selection position(int var1, int var2, int var3);

   Selection position(BlockPos var1);

   Selection fromTo(int var1, int var2, int var3, int var4, int var5, int var6);

   Selection fromTo(BlockPos var1, BlockPos var2);

   Selection column(int var1, int var2);

   Selection layer(int var1);

   Selection layersFrom(int var1);

   Selection layers(int var1, int var2);

   Selection cuboid(BlockPos var1, Vec3i var2);
}
