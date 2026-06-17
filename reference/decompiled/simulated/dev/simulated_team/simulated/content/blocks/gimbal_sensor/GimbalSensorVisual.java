package dev.simulated_team.simulated.content.blocks.gimbal_sensor;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.ColoredLitInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimDirectionUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;

public class GimbalSensorVisual extends AbstractBlockEntityVisual<GimbalSensorBlockEntity> implements SimpleDynamicVisual {
   private final List<OrientedInstance> indicators = new ArrayList<>();
   private final OrientedInstance gimbal;
   private final OrientedInstance compass;
   private final OrientedInstance needle;
   private final List<ColoredLitInstance> allInstances = new ArrayList<>();

   public GimbalSensorVisual(VisualizationContext ctx, GimbalSensorBlockEntity blockEntity, float partialTick) {
      super(ctx, blockEntity, partialTick);

      for (Direction dir : SimDirectionUtil.Y_AXIS_PLANE) {
         OrientedInstance inst = (OrientedInstance)this.instancerProvider()
            .instancer(InstanceTypes.ORIENTED, Models.partial(SimPartialModels.GIMBAL_SENSOR_INDICATOR))
            .createInstance();
         inst.position(this.getVisualPosition()).translatePivot(-0.5F, 0.0F, 0.0F).translatePosition(0.5F, 0.0F, 0.0F).rotateToFace(dir);
         this.indicators.add(inst);
      }

      this.gimbal = ((OrientedInstance)this.instancerProvider()
            .instancer(InstanceTypes.ORIENTED, Models.partial(SimPartialModels.GIMBAL_SENSOR_GIMBAL))
            .createInstance())
         .position(this.getVisualPosition())
         .translatePosition(0.5F, 0.5F, 0.5F)
         .translatePivot(-0.5F, -0.5F, -0.5F);
      this.compass = ((OrientedInstance)this.instancerProvider()
            .instancer(InstanceTypes.ORIENTED, Models.partial(SimPartialModels.GIMBAL_SENSOR_COMPASS))
            .createInstance())
         .position(this.getVisualPosition())
         .translatePosition(0.5F, 0.5F, 0.5F)
         .translatePivot(-0.5F, -0.5F, -0.5F);
      this.needle = ((OrientedInstance)this.instancerProvider()
            .instancer(InstanceTypes.ORIENTED, Models.partial(SimPartialModels.GIMBAL_SENSOR_NEEDLE))
            .createInstance())
         .position(this.getVisualPosition())
         .translatePosition(0.5F, 0.5F, 0.5F)
         .translatePivot(-0.5F, -0.5F, -0.5F);
      this.allInstances.addAll(this.indicators);
      this.allInstances.add(this.gimbal);
      this.allInstances.add(this.compass);
      this.allInstances.add(this.needle);
   }

   public void beginFrame(Context context) {
      for (int i = 0; i < SimDirectionUtil.Y_AXIS_PLANE.length; i++) {
         Direction dir = SimDirectionUtil.Y_AXIS_PLANE[i];
         OrientedInstance associatedInst = this.indicators.get(i);
         associatedInst.colorArgb(SimColors.redstone((float)Math.max(((GimbalSensorBlockEntity)this.blockEntity).getPower(dir), 0) / 15.0F)).setChanged();
      }

      this.handleRotations(context.partialTick());
   }

   private void handleRotations(float partialTicks) {
      this.gimbal.identityRotation();
      this.compass.identityRotation();
      this.needle.identityRotation();
      Quaternionf base = ((GimbalSensorBlockEntity)this.blockEntity).getBaseQuaternion();
      ((GimbalSensorBlockEntity)this.blockEntity).applyPrimaryQuaternion(base, partialTicks);
      this.gimbal.rotation(base);
      this.gimbal.setChanged();
      ((GimbalSensorBlockEntity)this.blockEntity).applySecondaryQuaternion(base, partialTicks);
      this.compass.rotation(base);
      this.compass.setChanged();
      ((GimbalSensorBlockEntity)this.blockEntity).applyCompassQuaternion(base, partialTicks);
      this.needle.rotation(base);
      this.needle.setChanged();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      for (AbstractInstance inst : this.allInstances) {
         consumer.accept(inst);
      }
   }

   public void updateLight(float v) {
      for (ColoredLitInstance inst : this.allInstances) {
         this.relight(new FlatLit[]{inst});
      }
   }

   protected void _delete() {
      for (ColoredLitInstance inst : this.allInstances) {
         inst.delete();
      }
   }
}
