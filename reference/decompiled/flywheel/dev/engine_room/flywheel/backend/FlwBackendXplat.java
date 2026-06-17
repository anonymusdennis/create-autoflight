package dev.engine_room.flywheel.backend;

import dev.engine_room.flywheel.api.internal.DependencyInjection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public interface FlwBackendXplat {
   FlwBackendXplat INSTANCE = DependencyInjection.load(FlwBackendXplat.class, "dev.engine_room.flywheel.backend.FlwBackendXplatImpl");

   int getLightEmission(BlockState var1, BlockGetter var2, BlockPos var3);
}
