package dev.engine_room.flywheel.api.visual;

import dev.engine_room.flywheel.api.instance.Instance;
import java.util.function.Consumer;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface BlockEntityVisual<T extends BlockEntity> extends Visual {
   void collectCrumblingInstances(Consumer<Instance> var1);
}
