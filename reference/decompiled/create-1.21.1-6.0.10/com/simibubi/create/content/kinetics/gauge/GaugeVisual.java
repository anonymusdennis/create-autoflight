package com.simibubi.create.content.kinetics.gauge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public abstract class GaugeVisual extends ShaftVisual<GaugeBlockEntity> implements SimpleDynamicVisual {
   protected final ArrayList<GaugeVisual.DialFace> faces = new ArrayList<>(2);
   protected final PoseStack ms = new PoseStack();

   protected GaugeVisual(VisualizationContext context, GaugeBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      GaugeBlock gaugeBlock = (GaugeBlock)this.blockState.getBlock();
      Instancer<TransformedInstance> dialModel = this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.GAUGE_DIAL));
      Instancer<TransformedInstance> headModel = this.getHeadModel();
      PoseTransformStack msr = TransformStack.of(this.ms);
      msr.translate(this.getVisualPosition());
      float progress = Mth.lerp(AnimationTickHolder.getPartialTicks(), blockEntity.prevDialState, blockEntity.dialState);

      for (Direction facing : Iterate.directions) {
         if (gaugeBlock.shouldRenderHeadOnFace(this.level, this.pos, this.blockState, facing)) {
            GaugeVisual.DialFace face = this.makeFace(facing, dialModel, headModel);
            this.faces.add(face);
            face.setupTransform(msr, progress);
         }
      }
   }

   private GaugeVisual.DialFace makeFace(Direction face, Instancer<TransformedInstance> dialModel, Instancer<TransformedInstance> headModel) {
      return new GaugeVisual.DialFace(face, (TransformedInstance)dialModel.createInstance(), (TransformedInstance)headModel.createInstance());
   }

   public void beginFrame(Context ctx) {
      if (!Mth.equal(((GaugeBlockEntity)this.blockEntity).prevDialState, ((GaugeBlockEntity)this.blockEntity).dialState)) {
         float progress = Mth.lerp(ctx.partialTick(), ((GaugeBlockEntity)this.blockEntity).prevDialState, ((GaugeBlockEntity)this.blockEntity).dialState);
         PoseTransformStack msr = TransformStack.of(this.ms);

         for (GaugeVisual.DialFace faceEntry : this.faces) {
            faceEntry.updateTransform(msr, progress);
         }
      }
   }

   @Override
   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(this.faces.stream().flatMap(Couple::stream).toArray(FlatLit[]::new));
   }

   @Override
   protected void _delete() {
      super._delete();
      this.faces.forEach(GaugeVisual.DialFace::delete);
   }

   @Override
   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);

      for (GaugeVisual.DialFace face : this.faces) {
         face.forEach(consumer);
      }
   }

   protected abstract Instancer<TransformedInstance> getHeadModel();

   protected class DialFace extends Couple<TransformedInstance> {
      Direction face;

      public DialFace(Direction face, TransformedInstance first, TransformedInstance second) {
         super(first, second);
         this.face = face;
      }

      private void setupTransform(TransformStack<?> msr, float progress) {
         float dialPivot = 0.359375F;
         msr.pushPose();
         this.rotateToFace(msr);
         ((TransformedInstance)this.getSecond()).setTransform(GaugeVisual.this.ms).setChanged();
         ((TransformStack)((TransformStack)msr.translate(0.0F, dialPivot, dialPivot)).rotate((float)((Math.PI / 2) * (double)(-progress)), Direction.EAST))
            .translate(0.0F, -dialPivot, -dialPivot);
         ((TransformedInstance)this.getFirst()).setTransform(GaugeVisual.this.ms).setChanged();
         msr.popPose();
      }

      private void updateTransform(TransformStack<?> msr, float progress) {
         float dialPivot = 0.359375F;
         msr.pushPose();
         ((TransformStack)((TransformStack)this.rotateToFace(msr).translate(0.0F, dialPivot, dialPivot))
               .rotate((float)((Math.PI / 2) * (double)(-progress)), Direction.EAST))
            .translate(0.0F, -dialPivot, -dialPivot);
         ((TransformedInstance)this.getFirst()).setTransform(GaugeVisual.this.ms).setChanged();
         msr.popPose();
      }

      protected TransformStack<?> rotateToFace(TransformStack<?> msr) {
         return (TransformStack<?>)((TransformStack)((TransformStack)msr.center())
               .rotate((float)((double)((-this.face.toYRot() - 90.0F) / 180.0F) * Math.PI), Direction.UP))
            .uncenter();
      }

      private void delete() {
         ((TransformedInstance)this.getFirst()).delete();
         ((TransformedInstance)this.getSecond()).delete();
      }
   }

   public static class Speed extends GaugeVisual {
      public Speed(VisualizationContext context, GaugeBlockEntity blockEntity, float partialTick) {
         super(context, blockEntity, partialTick);
      }

      @Override
      protected Instancer<TransformedInstance> getHeadModel() {
         return this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.GAUGE_HEAD_SPEED));
      }
   }

   public static class Stress extends GaugeVisual {
      public Stress(VisualizationContext context, GaugeBlockEntity blockEntity, float partialTick) {
         super(context, blockEntity, partialTick);
      }

      @Override
      protected Instancer<TransformedInstance> getHeadModel() {
         return this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.GAUGE_HEAD_STRESS));
      }
   }
}
