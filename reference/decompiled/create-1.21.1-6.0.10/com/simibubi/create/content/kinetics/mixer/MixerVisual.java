package com.simibubi.create.content.kinetics.mixer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.core.Direction.Axis;

public class MixerVisual extends SingleAxisRotatingVisual<MechanicalMixerBlockEntity> implements SimpleDynamicVisual {
   private final RotatingInstance mixerHead;
   private final OrientedInstance mixerPole;
   private final MechanicalMixerBlockEntity mixer;

   public MixerVisual(VisualizationContext context, MechanicalMixerBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick, Models.partial(AllPartialModels.SHAFTLESS_COGWHEEL));
      this.mixer = blockEntity;
      this.mixerHead = (RotatingInstance)this.instancerProvider()
         .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.MECHANICAL_MIXER_HEAD))
         .createInstance();
      this.mixerHead.setRotationAxis(Axis.Y);
      this.mixerPole = (OrientedInstance)this.instancerProvider()
         .instancer(InstanceTypes.ORIENTED, Models.partial(AllPartialModels.MECHANICAL_MIXER_POLE))
         .createInstance();
      this.animate(partialTick);
   }

   public void beginFrame(Context ctx) {
      this.animate(ctx.partialTick());
   }

   private void animate(float pt) {
      float renderedHeadOffset = this.mixer.getRenderedHeadOffset(pt);
      this.transformPole(renderedHeadOffset);
      this.transformHead(renderedHeadOffset, pt);
   }

   private void transformHead(float renderedHeadOffset, float pt) {
      float speed = this.mixer.getRenderedHeadRotationSpeed(pt);
      this.mixerHead.setPosition(this.getVisualPosition()).nudge(0.0F, -renderedHeadOffset, 0.0F).setRotationalSpeed(speed * 2.0F * 6.0F).setChanged();
   }

   private void transformPole(float renderedHeadOffset) {
      this.mixerPole.position(this.getVisualPosition()).translatePosition(0.0F, -renderedHeadOffset, 0.0F).setChanged();
   }

   @Override
   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(this.pos.below(), new FlatLit[]{this.mixerHead});
      this.relight(new FlatLit[]{this.mixerPole});
   }

   @Override
   protected void _delete() {
      super._delete();
      this.mixerHead.delete();
      this.mixerPole.delete();
   }

   @Override
   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.mixerHead);
      consumer.accept(this.mixerPole);
   }
}
