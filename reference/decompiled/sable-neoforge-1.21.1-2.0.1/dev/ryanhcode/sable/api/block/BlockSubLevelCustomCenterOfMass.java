package dev.ryanhcode.sable.api.block;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3dc;

public interface BlockSubLevelCustomCenterOfMass {
   Vector3dc getCenterOfMass(BlockGetter var1, BlockState var2);
}
