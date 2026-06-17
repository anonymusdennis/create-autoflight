package dev.engine_room.flywheel.api.visualization;

import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface BlockEntityVisualizer<T extends BlockEntity> {
   BlockEntityVisual<? super T> createVisual(VisualizationContext var1, T var2, float var3);

   boolean skipVanillaRender(T var1);
}
