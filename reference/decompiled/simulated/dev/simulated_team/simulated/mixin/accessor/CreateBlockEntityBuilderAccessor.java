package dev.simulated_team.simulated.mixin.accessor;

import com.simibubi.create.foundation.data.CreateBlockEntityBuilder;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer.Factory;
import java.util.function.Predicate;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({CreateBlockEntityBuilder.class})
public interface CreateBlockEntityBuilderAccessor<T extends BlockEntity, P> {
   @Accessor
   NonNullSupplier<Factory<T>> getVisualFactory();

   @Accessor
   Predicate<T> getRenderNormally();
}
