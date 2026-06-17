package com.simibubi.create.content.logistics.funnel;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.FlapStuffs;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

public class FunnelVisual extends AbstractBlockEntityVisual<FunnelBlockEntity> implements SimpleDynamicVisual {
   private final FlapStuffs.Visual flaps;

   public FunnelVisual(VisualizationContext context, FunnelBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      if (!blockEntity.hasFlap()) {
         this.flaps = null;
      } else {
         Direction funnelFacing = FunnelBlock.getFunnelFacing(this.blockState);
         PartialModel flapPartial = this.blockState.getBlock() instanceof FunnelBlock ? AllPartialModels.FUNNEL_FLAP : AllPartialModels.BELT_FUNNEL_FLAP;
         Matrix4f commonTransform = FlapStuffs.commonTransform(this.getVisualPosition(), funnelFacing, -blockEntity.getFlapOffset());
         this.flaps = new FlapStuffs.Visual(this.instancerProvider(), commonTransform, FlapStuffs.FUNNEL_PIVOT, Models.partial(flapPartial));
         this.flaps.update(blockEntity.flap.getValue(partialTick));
      }
   }

   public void beginFrame(Context ctx) {
      if (this.flaps != null) {
         this.flaps.update(((FunnelBlockEntity)this.blockEntity).flap.getValue(ctx.partialTick()));
      }
   }

   public void updateLight(float partialTick) {
      if (this.flaps != null) {
         this.flaps.updateLight(this.computePackedLight());
      }
   }

   protected void _delete() {
      if (this.flaps != null) {
         this.flaps.delete();
      }
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      if (this.flaps != null) {
         this.flaps.collectCrumblingInstances(consumer);
      }
   }
}
