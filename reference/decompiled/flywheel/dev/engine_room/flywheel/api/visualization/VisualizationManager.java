package dev.engine_room.flywheel.api.visualization;

import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.internal.FlwApiLink;
import dev.engine_room.flywheel.api.visual.Effect;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface VisualizationManager {
   static boolean supportsVisualization(@Nullable LevelAccessor level) {
      return FlwApiLink.INSTANCE.supportsVisualization(level);
   }

   @Nullable
   static VisualizationManager get(@Nullable LevelAccessor level) {
      return FlwApiLink.INSTANCE.getVisualizationManager(level);
   }

   static VisualizationManager getOrThrow(@Nullable LevelAccessor level) {
      return FlwApiLink.INSTANCE.getVisualizationManagerOrThrow(level);
   }

   Vec3i renderOrigin();

   VisualManager<BlockEntity> blockEntities();

   VisualManager<Entity> entities();

   VisualManager<Effect> effects();

   VisualizationManager.RenderDispatcher renderDispatcher();

   @NonExtendable
   public interface RenderDispatcher {
      void onStartLevelRender(RenderContext var1);

      void afterEntities(RenderContext var1);

      void beforeCrumbling(RenderContext var1, Long2ObjectMap<SortedSet<BlockDestructionProgress>> var2);
   }
}
