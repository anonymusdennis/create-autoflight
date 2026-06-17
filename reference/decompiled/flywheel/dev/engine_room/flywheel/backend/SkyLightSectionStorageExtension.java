package dev.engine_room.flywheel.backend;

import net.minecraft.world.level.chunk.DataLayer;
import org.jetbrains.annotations.Nullable;

public interface SkyLightSectionStorageExtension {
   @Nullable
   DataLayer flywheel$skyDataLayer(long var1);
}
