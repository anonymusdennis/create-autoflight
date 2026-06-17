package dev.engine_room.flywheel.api.backend;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@BackendImplemented
public interface Engine {
   VisualizationContext createVisualizationContext();

   Plan<RenderContext> createFramePlan();

   Vec3i renderOrigin();

   boolean updateRenderOrigin(Camera var1);

   void lightSections(LongSet var1);

   void onLightUpdate(SectionPos var1, LightLayer var2);

   void render(RenderContext var1);

   void renderCrumbling(RenderContext var1, List<Engine.CrumblingBlock> var2);

   void delete();

   @NonExtendable
   public interface CrumblingBlock {
      BlockPos pos();

      @Range(
         from = 0L,
         to = 9L
      )
      int progress();

      List<Instance> instances();
   }
}
