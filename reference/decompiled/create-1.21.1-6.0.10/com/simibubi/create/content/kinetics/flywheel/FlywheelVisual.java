package com.simibubi.create.content.kinetics.flywheel;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class FlywheelVisual extends KineticBlockEntityVisual<FlywheelBlockEntity> implements SimpleDynamicVisual {
   protected final RotatingInstance shaft;
   protected final TransformedInstance wheel;
   protected float lastAngle = Float.NaN;
   protected final Matrix4f baseTransform = new Matrix4f();

   public FlywheelVisual(VisualizationContext context, FlywheelBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      Axis axis = this.rotationAxis();
      this.shaft = (RotatingInstance)this.instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT)).createInstance();
      this.shaft.setup((KineticBlockEntity)this.blockEntity).setPosition(this.getVisualPosition()).rotateToFace(axis).setChanged();
      this.wheel = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.FLYWHEEL))
         .createInstance();
      Direction align = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
      ((TransformedInstance)((TransformedInstance)this.wheel.translate(this.getVisualPosition())).center())
         .rotate(new Quaternionf().rotateTo(0.0F, 1.0F, 0.0F, (float)align.getStepX(), (float)align.getStepY(), (float)align.getStepZ()));
      this.baseTransform.set(this.wheel.pose);
      this.animate(blockEntity.angle);
   }

   public void beginFrame(Context ctx) {
      float partialTicks = ctx.partialTick();
      float speed = ((FlywheelBlockEntity)this.blockEntity).visualSpeed.getValue(partialTicks) * 3.0F / 10.0F;
      float angle = ((FlywheelBlockEntity)this.blockEntity).angle + speed * partialTicks;
      if (!((double)Math.abs(angle - this.lastAngle) < 0.001)) {
         this.animate(angle);
         this.lastAngle = angle;
      }
   }

   private void animate(float angle) {
      ((TransformedInstance)this.wheel.setTransform(this.baseTransform).rotateY(AngleHelper.rad((double)angle)).uncenter()).setChanged();
   }

   public void update(float pt) {
      this.shaft.setup((KineticBlockEntity)this.blockEntity).setChanged();
   }

   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.shaft, this.wheel});
   }

   protected void _delete() {
      this.shaft.delete();
      this.wheel.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.shaft);
      consumer.accept(this.wheel);
   }
}
