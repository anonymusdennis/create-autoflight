package dev.engine_room.flywheel.impl.compat;

import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.impl.FlwImpl;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.caffeinemc.mods.sodium.api.blockentity.BlockEntityRenderHandler;
import net.caffeinemc.mods.sodium.api.blockentity.BlockEntityRenderPredicate;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public final class SodiumCompat {
   public static final boolean ACTIVE = CompatMod.SODIUM.isLoaded;

   private SodiumCompat() {
   }

   @Nullable
   public static <T extends BlockEntity> Object onSetBlockEntityVisualizer(
      BlockEntityType<T> type,
      @Nullable BlockEntityVisualizer<? super T> oldVisualizer,
      @Nullable BlockEntityVisualizer<? super T> newVisualizer,
      @Nullable Object predicate
   ) {
      if (!ACTIVE) {
         return null;
      } else if (oldVisualizer == null && newVisualizer != null) {
         if (predicate != null) {
            throw new IllegalArgumentException("Sodium predicate must be null when old visualizer is null");
         } else {
            return SodiumCompat.Internals.addPredicate(type);
         }
      } else if (oldVisualizer == null || newVisualizer != null) {
         return predicate;
      } else if (predicate == null) {
         throw new IllegalArgumentException("Sodium predicate must not be null when old visualizer is not null");
      } else {
         SodiumCompat.Internals.removePredicate(type, predicate);
         return null;
      }
   }

   static {
      if (ACTIVE) {
         FlwImpl.LOGGER.debug("Detected Sodium");
      }
   }

   private static final class Internals {
      static <T extends BlockEntity> Object addPredicate(BlockEntityType<T> type) {
         BlockEntityRenderPredicate<T> predicate = (getter, pos, be) -> !VisualizationHelper.tryAddBlockEntity(be);
         BlockEntityRenderHandler.instance().addRenderPredicate(type, predicate);
         return predicate;
      }

      static <T extends BlockEntity> void removePredicate(BlockEntityType<T> type, Object predicate) {
         BlockEntityRenderHandler.instance().removeRenderPredicate(type, (BlockEntityRenderPredicate)predicate);
      }
   }
}
