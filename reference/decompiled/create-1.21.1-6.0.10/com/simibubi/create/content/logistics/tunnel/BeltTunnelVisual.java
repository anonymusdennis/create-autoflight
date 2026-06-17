package com.simibubi.create.content.logistics.tunnel;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.FlapStuffs;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

public class BeltTunnelVisual extends AbstractBlockEntityVisual<BeltTunnelBlockEntity> implements SimpleDynamicVisual {
   private final Map<Direction, FlapStuffs.Visual> tunnelFlaps = new EnumMap<>(Direction.class);
   private int light;

   public BeltTunnelVisual(VisualizationContext context, BeltTunnelBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      this.createFlaps();
      this.updateFlaps(partialTick);
   }

   private void createFlaps() {
      ((BeltTunnelBlockEntity)this.blockEntity)
         .flaps
         .forEach(
            (direction, flapValue) -> {
               Matrix4f commonTransform = FlapStuffs.commonTransform(this.visualPos, direction, 0.0F);
               FlapStuffs.Visual flapSide = new FlapStuffs.Visual(
                  this.instancerProvider(), commonTransform, FlapStuffs.TUNNEL_PIVOT, Models.partial(AllPartialModels.BELT_TUNNEL_FLAP)
               );
               flapSide.updateLight(this.light);
               this.tunnelFlaps.put(direction, flapSide);
            }
         );
   }

   public void update(float partialTick) {
      super.update(partialTick);
      this._delete();
      this.createFlaps();
      this.updateFlaps(partialTick);
   }

   public void beginFrame(Context ctx) {
      this.updateFlaps(ctx.partialTick());
   }

   private void updateFlaps(float partialTicks) {
      this.tunnelFlaps.forEach((direction, keys) -> {
         LerpedFloat lerpedFloat = ((BeltTunnelBlockEntity)this.blockEntity).flaps.get(direction);
         if (lerpedFloat != null) {
            keys.update(lerpedFloat.getValue(partialTicks));
         }
      });
   }

   public void updateLight(float partialTick) {
      this.light = this.computePackedLight();

      for (FlapStuffs.Visual value : this.tunnelFlaps.values()) {
         value.updateLight(this.light);
      }
   }

   protected void _delete() {
      this.tunnelFlaps.values().forEach(FlapStuffs.Visual::delete);
      this.tunnelFlaps.clear();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      for (FlapStuffs.Visual value : this.tunnelFlaps.values()) {
         value.collectCrumblingInstances(consumer);
      }
   }
}
