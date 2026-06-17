package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing;

import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.simulated_team.simulated.util.SimMathUtils;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;

public class PropellerBearingVisual extends OrientedRotatingVisual<PropellerBearingBlockEntity> implements SimpleDynamicVisual {
   private final OrientedInstance topInstance;
   private final Axis rotationAxis;
   private final Quaternionf blockOrientation;

   public PropellerBearingVisual(VisualizationContext context, PropellerBearingBlockEntity blockEntity, float partialTick) {
      super(
         context,
         blockEntity,
         partialTick,
         Direction.SOUTH,
         ((Direction)blockEntity.getBlockState().getValue(BlockStateProperties.FACING)).getOpposite(),
         Models.partial(AllPartialModels.SHAFT_HALF)
      );
      Direction facing = (Direction)this.blockState.getValue(BlockStateProperties.FACING);
      this.rotationAxis = Axis.of(Direction.get(AxisDirection.POSITIVE, this.rotationAxis()).step());
      this.blockOrientation = SimMathUtils.getBlockStateOrientation(facing);
      PartialModel top = AeroPartialModels.BEARING_PLATE;
      this.topInstance = (OrientedInstance)this.instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(top)).createInstance();
      this.topInstance.position(this.getVisualPosition()).rotation(this.blockOrientation).setChanged();
   }

   public void beginFrame(Context ctx) {
      float interpolatedAngle = ((PropellerBearingBlockEntity)this.blockEntity).getInterpolatedAngle(ctx.partialTick() - 1.0F);
      Quaternionf rot = this.rotationAxis.rotationDegrees(interpolatedAngle);
      rot.mul(this.blockOrientation);
      this.topInstance.rotation(rot).setChanged();
   }

   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(new FlatLit[]{this.topInstance});
   }

   protected void _delete() {
      super._delete();
      this.topInstance.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.topInstance);
   }
}
