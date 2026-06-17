package dev.engine_room.flywheel.api.visualization;

import dev.engine_room.flywheel.api.backend.BackendImplemented;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import net.minecraft.core.Vec3i;

@BackendImplemented
public interface VisualizationContext {
   InstancerProvider instancerProvider();

   Vec3i renderOrigin();

   VisualEmbedding createEmbedding(Vec3i var1);
}
