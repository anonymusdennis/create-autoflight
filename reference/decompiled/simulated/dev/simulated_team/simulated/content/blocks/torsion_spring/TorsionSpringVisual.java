package dev.simulated_team.simulated.content.blocks.torsion_spring;

import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimMathUtils;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;

public class TorsionSpringVisual extends OrientedRotatingVisual<TorsionSpringBlockEntity> implements SimpleDynamicVisual {
   private final Axis rotationAxis;
   private final Quaternionf blockOrientation;
   private final RotatingInstance topInstance;
   private final OrientedInstance springInstance;
   private boolean wasSpringStatic;

   public TorsionSpringVisual(VisualizationContext context, TorsionSpringBlockEntity blockEntity, float partialTick) {
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
      this.topInstance = ((RotatingInstance)this.instancerProvider()
            .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
            .createInstance())
         .rotateToFace(Direction.SOUTH, (Direction)blockEntity.getBlockState().getValue(BlockStateProperties.FACING))
         .setup(blockEntity.getExtraKinetics())
         .setPosition(this.getVisualPosition());
      this.springInstance = ((OrientedInstance)this.instancerProvider()
            .instancer(InstanceTypes.ORIENTED, Models.partial(SimPartialModels.TORSION_SPRING))
            .createInstance())
         .rotation(this.blockOrientation)
         .position(this.getVisualPosition());
      this.topInstance.setChanged();
      this.springInstance.setChanged();
      this.wasSpringStatic = false;
   }

   public void update(float pt) {
      super.update(pt);
      this.topInstance.setup(((TorsionSpringBlockEntity)this.blockEntity).getExtraKinetics()).setChanged();
   }

   public void beginFrame(Context ctx) {
      if (!this.wasSpringStatic || !((TorsionSpringBlockEntity)this.blockEntity).isSpringStatic()) {
         this.wasSpringStatic = ((TorsionSpringBlockEntity)this.blockEntity).isSpringStatic();
         float interpolatedAngle = ((TorsionSpringBlockEntity)this.blockEntity).interpolatedSpring(ctx.partialTick());
         Quaternionf rot = this.rotationAxis.rotationDegrees(interpolatedAngle);
         rot.mul(this.blockOrientation);
         this.springInstance.rotation(rot).setChanged();
      }
   }

   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(new FlatLit[]{this.topInstance});
      this.relight(new FlatLit[]{this.springInstance});
   }

   protected void _delete() {
      super._delete();
      this.topInstance.delete();
      this.springInstance.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.topInstance);
      consumer.accept(this.springInstance);
   }
}
