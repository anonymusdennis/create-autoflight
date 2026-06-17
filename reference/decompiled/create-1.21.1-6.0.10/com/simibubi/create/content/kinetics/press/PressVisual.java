package com.simibubi.create.content.kinetics.press;

import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;

public class PressVisual extends ShaftVisual<MechanicalPressBlockEntity> implements SimpleDynamicVisual {
   private final OrientedInstance pressHead = (OrientedInstance)this.instancerProvider()
      .instancer(InstanceTypes.ORIENTED, Models.partial(AllPartialModels.MECHANICAL_PRESS_HEAD))
      .createInstance();

   public PressVisual(VisualizationContext context, MechanicalPressBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      Quaternionf q = Axis.YP.rotationDegrees(AngleHelper.horizontalAngle((Direction)this.blockState.getValue(MechanicalPressBlock.HORIZONTAL_FACING)));
      this.pressHead.rotation(q);
      this.transformModels(partialTick);
   }

   public void beginFrame(Context ctx) {
      this.transformModels(ctx.partialTick());
   }

   private void transformModels(float pt) {
      float renderedHeadOffset = this.getRenderedHeadOffset(pt);
      this.pressHead.position(this.getVisualPosition()).translatePosition(0.0F, -renderedHeadOffset, 0.0F).setChanged();
   }

   private float getRenderedHeadOffset(float pt) {
      PressingBehaviour pressingBehaviour = ((MechanicalPressBlockEntity)this.blockEntity).getPressingBehaviour();
      return pressingBehaviour.getRenderedHeadOffset(pt) * pressingBehaviour.mode.headOffset;
   }

   @Override
   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(new FlatLit[]{this.pressHead});
   }

   @Override
   protected void _delete() {
      super._delete();
      this.pressHead.delete();
   }

   @Override
   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.pressHead);
   }
}
