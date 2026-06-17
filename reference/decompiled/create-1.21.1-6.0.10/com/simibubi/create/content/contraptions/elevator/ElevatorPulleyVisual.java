package com.simibubi.create.content.contraptions.elevator;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import com.simibubi.create.content.contraptions.pulley.PulleyRenderer;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import com.simibubi.create.content.processing.burner.ScrollInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.simibubi.create.foundation.render.SpecialModels;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual.SectionCollector;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.util.InstanceRecycler;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public class ElevatorPulleyVisual extends ShaftVisual<ElevatorPulleyBlockEntity> implements SimpleDynamicVisual, ShaderLightVisual {
   private final InstanceRecycler<ScrollInstance> belt;
   private final ScrollInstance halfBelt;
   private final ScrollInstance coil;
   private final TransformedInstance magnet;
   private final Matrix4fc cachedMagnetTransform;
   private float lastOffset = Float.NaN;
   private final long topSection;
   private long lastBottomSection;

   public ElevatorPulleyVisual(VisualizationContext context, ElevatorPulleyBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      float blockStateAngle = AngleHelper.horizontalAngle((Direction)this.blockState.getValue(ElevatorPulleyBlock.HORIZONTAL_FACING));
      Quaternionfc rotation = new Quaternionf().rotationY((float) (Math.PI / 180.0) * blockStateAngle);
      this.topSection = SectionPos.of(this.pos).asLong();
      this.belt = new InstanceRecycler(
         () -> ((ScrollInstance)context.instancerProvider()
                  .instancer(AllInstanceTypes.SCROLLING, SpecialModels.flatLit(AllPartialModels.ELEVATOR_BELT))
                  .createInstance())
               .rotation(rotation)
               .setSpriteShift(AllSpriteShifts.ELEVATOR_BELT)
      );
      this.halfBelt = ((ScrollInstance)context.instancerProvider()
            .instancer(AllInstanceTypes.SCROLLING, SpecialModels.flatLit(AllPartialModels.ELEVATOR_BELT_HALF))
            .createInstance())
         .rotation(rotation)
         .setSpriteShift(AllSpriteShifts.ELEVATOR_BELT);
      this.coil = ((ScrollInstance)context.instancerProvider()
            .instancer(AllInstanceTypes.SCROLLING, Models.partial(AllPartialModels.ELEVATOR_COIL))
            .createInstance())
         .position(this.getVisualPosition())
         .rotation(rotation)
         .setSpriteShift(AllSpriteShifts.ELEVATOR_COIL);
      this.coil.setChanged();
      this.magnet = (TransformedInstance)context.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatLit(AllPartialModels.ELEVATOR_MAGNET))
         .createInstance();
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)this.magnet.setIdentityTransform().translate(this.getVisualPosition())).center())
            .rotateYDegrees(blockStateAngle))
         .uncenter();
      this.cachedMagnetTransform = new Matrix4f(this.magnet.pose);
      this.animate(PulleyRenderer.getBlockEntityOffset(partialTick, blockEntity));
   }

   @Override
   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(new FlatLit[]{this.coil});
   }

   public void setSectionCollector(SectionCollector sectionCollector) {
      super.setSectionCollector(sectionCollector);
      sectionCollector.sections(this.getLightSections(this.lastOffset));
   }

   public void beginFrame(Context ctx) {
      this.animate(PulleyRenderer.getBlockEntityOffset(ctx.partialTick(), (PulleyBlockEntity)this.blockEntity));
   }

   @Override
   protected void _delete() {
      super._delete();
      this.belt.delete();
      this.halfBelt.delete();
      this.coil.delete();
      this.magnet.delete();
   }

   private void animate(float offset) {
      if (offset != this.lastOffset) {
         this.lastOffset = offset;
         this.maybeUpdateSections(offset);
         this.animateCoil(offset);
         this.animateHalfBelt(offset);
         this.animateBelt(offset);
         this.animateMagnet(offset);
      }
   }

   private void maybeUpdateSections(float offset) {
      if (this.lightSections != null) {
         if (this.lastBottomSection != SectionPos.offset(this.topSection, 0, -offset2SectionCount(offset), 0)) {
            this.lightSections.sections(this.getLightSections(offset));
         }
      }
   }

   private void animateMagnet(float offset) {
      ((TransformedInstance)this.magnet.setTransform(this.cachedMagnetTransform).translateY(-offset)).setChanged();
   }

   private void animateBelt(float offset) {
      this.belt.resetCount();

      for (int i = 0; (float)i < offset - 0.25F; i++) {
         ScrollInstance segment = ((ScrollInstance)this.belt.get()).position(this.getVisualPosition()).shift(0.0F, -(offset - (float)i), 0.0F);
         segment.offsetV = offset;
         segment.setChanged();
      }

      this.belt.discardExtra();
   }

   private void animateHalfBelt(float offset) {
      float f = offset % 1.0F;
      if (!(f < 0.25F) && !(f > 0.75F)) {
         this.halfBelt.setVisible(false);
      } else {
         this.halfBelt.setVisible(true);
         this.halfBelt.position(this.getVisualPosition()).shift(0.0F, -(f > 0.75F ? f - 1.0F : f), 0.0F);
         this.halfBelt.offsetV = offset;
         this.halfBelt.setChanged();
      }
   }

   private void animateCoil(float offset) {
      this.coil.offsetV = -offset * 2.0F;
      this.coil.setChanged();
   }

   private LongSet getLightSections(float offset) {
      LongArraySet out = new LongArraySet();
      int sectionCount = offset2SectionCount(offset);

      for (int i = 0; i < sectionCount; i++) {
         out.add(SectionPos.offset(this.topSection, 0, -i, 0));
      }

      this.lastBottomSection = SectionPos.offset(this.topSection, 0, -sectionCount, 0);
      return out;
   }

   private static int offset2SectionCount(float offset) {
      return (int)Math.ceil((double)((offset + 1.0F) / 16.0F));
   }
}
