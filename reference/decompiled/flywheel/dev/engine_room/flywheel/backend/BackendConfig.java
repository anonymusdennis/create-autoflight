package dev.engine_room.flywheel.backend;

import dev.engine_room.flywheel.backend.compile.LightSmoothness;

public interface BackendConfig {
   BackendConfig INSTANCE = FlwBackend.config();

   LightSmoothness lightSmoothness();
}
