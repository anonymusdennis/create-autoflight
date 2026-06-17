package com.simibubi.create.content.trains.signal;

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
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SignalVisual extends AbstractBlockEntityVisual<SignalBlockEntity> implements SimpleTickableVisual {
   private final TransformedInstance signalLight;
   private final TransformedInstance signalOverlay;
   private boolean previousIsRedLight;
   private SignalBlockEntity.OverlayState previousOverlayState;

   public SignalVisual(VisualizationContext ctx, SignalBlockEntity blockEntity, float partialTick) {
      super(ctx, blockEntity, partialTick);
      this.signalLight = (TransformedInstance)ctx.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.SIGNAL_OFF))
         .createInstance();
      this.signalOverlay = (TransformedInstance)ctx.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.TRACK_SIGNAL_OVERLAY))
         .createInstance();
      this.setupVisual();
   }

   public void tick(Context context) {
      this.setupVisual();
   }

   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.signalLight, this.signalOverlay});
   }

   protected void _delete() {
      this.signalLight.delete();
      this.signalOverlay.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.signalLight);
   }

   private void setupVisual() {
      SignalBlockEntity.SignalState signalState = ((SignalBlockEntity)this.blockEntity).getState();
      float renderTime = AnimationTickHolder.getRenderTime(((SignalBlockEntity)this.blockEntity).getLevel());
      boolean isRedLight = signalState.isRedLight(renderTime);
      if (isRedLight != this.previousIsRedLight) {
         PartialModel partial = isRedLight ? AllPartialModels.SIGNAL_ON : AllPartialModels.SIGNAL_OFF;
         this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(partial)).stealInstance(this.signalLight);
      }

      this.signalLight.setIdentityTransform().translate(this.getVisualPosition());
      if (isRedLight) {
         this.signalLight.light(240);
      }

      this.signalLight.setChanged();
      this.previousIsRedLight = isRedLight;
      SignalBlockEntity.OverlayState overlayState = ((SignalBlockEntity)this.blockEntity).getOverlay();
      TrackTargetingBehaviour<SignalBoundary> target = ((SignalBlockEntity)this.blockEntity).edgePoint;
      BlockPos targetPosition = target.getGlobalPosition();
      Level level = ((SignalBlockEntity)this.blockEntity).getLevel();
      BlockState trackState = level.getBlockState(targetPosition);
      if (trackState.getBlock() instanceof ITrackBlock trackBlock && overlayState != SignalBlockEntity.OverlayState.SKIP) {
         if (overlayState != this.previousOverlayState) {
            this.previousOverlayState = overlayState;
            PartialModel partial;
            TrackTargetingBehaviour.RenderedTrackOverlayType type;
            if (overlayState == SignalBlockEntity.OverlayState.DUAL) {
               type = TrackTargetingBehaviour.RenderedTrackOverlayType.DUAL_SIGNAL;
               partial = AllPartialModels.TRACK_SIGNAL_DUAL_OVERLAY;
            } else {
               type = TrackTargetingBehaviour.RenderedTrackOverlayType.SIGNAL;
               partial = AllPartialModels.TRACK_SIGNAL_OVERLAY;
            }

            this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(partial)).stealInstance(this.signalOverlay);
            this.signalOverlay.setIdentityTransform().translate(targetPosition.subtract(this.renderOrigin()));
            trackBlock.prepareTrackOverlay(this.signalOverlay, level, targetPosition, trackState, target.getTargetBezier(), target.getTargetDirection(), type);
            this.signalOverlay.setChanged();
         }

         return;
      }

      this.previousOverlayState = null;
      this.signalOverlay.setZeroTransform().setChanged();
   }
}
