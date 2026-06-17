package dev.engine_room.flywheel.api.backend;

import dev.engine_room.flywheel.api.internal.FlwApiLink;
import dev.engine_room.flywheel.api.registry.IdRegistry;
import net.minecraft.world.level.LevelAccessor;

@BackendImplemented
public interface Backend {
   IdRegistry<Backend> REGISTRY = FlwApiLink.INSTANCE.createIdRegistry();

   Engine createEngine(LevelAccessor var1);

   int priority();

   boolean isSupported();
}
