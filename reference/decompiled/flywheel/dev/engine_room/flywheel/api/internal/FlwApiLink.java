package dev.engine_room.flywheel.api.internal;

import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.api.registry.IdRegistry;
import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.EntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public interface FlwApiLink {
   FlwApiLink INSTANCE = DependencyInjection.load(FlwApiLink.class, "dev.engine_room.flywheel.impl.FlwApiLinkImpl");

   <T> IdRegistry<T> createIdRegistry();

   Backend getCurrentBackend();

   boolean isBackendOn();

   Backend getOffBackend();

   Backend getDefaultBackend();

   LayoutBuilder createLayoutBuilder();

   boolean supportsVisualization(@Nullable LevelAccessor var1);

   @Nullable
   VisualizationManager getVisualizationManager(@Nullable LevelAccessor var1);

   VisualizationManager getVisualizationManagerOrThrow(@Nullable LevelAccessor var1);

   @Nullable
   <T extends BlockEntity> BlockEntityVisualizer<? super T> getVisualizer(BlockEntityType<T> var1);

   @Nullable
   <T extends Entity> EntityVisualizer<? super T> getVisualizer(EntityType<T> var1);

   <T extends BlockEntity> void setVisualizer(BlockEntityType<T> var1, @Nullable BlockEntityVisualizer<? super T> var2);

   <T extends Entity> void setVisualizer(EntityType<T> var1, @Nullable EntityVisualizer<? super T> var2);
}
