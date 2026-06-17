package dev.ryanhcode.sable.api.block;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface BlockSubLevelCollisionShape {
   VoxelShape getSubLevelCollisionShape(BlockGetter var1, BlockState var2);
}
