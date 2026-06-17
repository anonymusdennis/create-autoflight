package com.simibubi.create.content.trains.observer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.TickableVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class TrackObserverVisual extends AbstractBlockEntityVisual<TrackObserverBlockEntity> implements SimpleTickableVisual {
   private final TransformedInstance overlay;
   private BlockPos oldTargetPos;

   public TrackObserverVisual(VisualizationContext ctx, TrackObserverBlockEntity blockEntity, float partialTick) {
      super(ctx, blockEntity, partialTick);
      this.overlay = (TransformedInstance)ctx.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.TRACK_OBSERVER_OVERLAY))
         .createInstance();
      this.setupVisual();
   }

   public void tick(Context context) {
      this.setupVisual();
   }

   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.overlay});
   }

   protected void _delete() {
      this.overlay.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.overlay);
   }

   private void setupVisual() {
      TrackTargetingBehaviour<TrackObserver> target = ((TrackObserverBlockEntity)this.blockEntity).edgePoint;
      BlockPos targetPosition = target.getGlobalPosition();
      Level level = ((TrackObserverBlockEntity)this.blockEntity).getLevel();
      BlockState trackState = level.getBlockState(targetPosition);
      if (trackState.getBlock() instanceof ITrackBlock trackBlock) {
         if (!targetPosition.equals(this.oldTargetPos)) {
            this.oldTargetPos = targetPosition;
            this.overlay.setIdentityTransform().translate(targetPosition.subtract(this.renderOrigin()));
            TrackTargetingBehaviour.RenderedTrackOverlayType type = TrackTargetingBehaviour.RenderedTrackOverlayType.OBSERVER;
            trackBlock.prepareTrackOverlay(this.overlay, level, targetPosition, trackState, target.getTargetBezier(), target.getTargetDirection(), type);
            this.overlay.setChanged();
         }
      } else {
         this.overlay.setZeroTransform().setChanged();
      }
   }
}
