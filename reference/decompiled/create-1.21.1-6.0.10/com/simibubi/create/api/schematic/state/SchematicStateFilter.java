package com.simibubi.create.api.schematic.state;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface SchematicStateFilter {
   BlockState filterStates(@Nullable BlockEntity var1, BlockState var2);
}
