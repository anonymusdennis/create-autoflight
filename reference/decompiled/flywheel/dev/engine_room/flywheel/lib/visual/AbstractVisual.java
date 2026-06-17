package dev.engine_room.flywheel.lib.visual;

import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.visual.Visual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

public abstract class AbstractVisual implements Visual {
   protected final VisualizationContext visualizationContext;
   protected final Level level;
   protected boolean deleted = false;

   public AbstractVisual(VisualizationContext ctx, Level level, float partialTick) {
      this.visualizationContext = ctx;
      this.level = level;
   }

   @Override
   public void update(float partialTick) {
   }

   protected abstract void _delete();

   protected InstancerProvider instancerProvider() {
      return this.visualizationContext.instancerProvider();
   }

   protected Vec3i renderOrigin() {
      return this.visualizationContext.renderOrigin();
   }

   @Override
   public final void delete() {
      if (!this.deleted) {
         this._delete();
         this.deleted = true;
      }
   }
}
