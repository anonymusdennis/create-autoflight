package com.simibubi.create.content.contraptions.bearing;

import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
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
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;

public class BearingVisual<B extends KineticBlockEntity & IBearingBlockEntity> extends OrientedRotatingVisual<B> implements SimpleDynamicVisual {
   final OrientedInstance topInstance;
   final Axis rotationAxis;
   final Quaternionf blockOrientation;

   public BearingVisual(VisualizationContext context, B blockEntity, float partialTick) {
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
      this.blockOrientation = getBlockStateOrientation(facing);
      PartialModel top = blockEntity.isWoodenTop() ? AllPartialModels.BEARING_TOP_WOODEN : AllPartialModels.BEARING_TOP;
      this.topInstance = (OrientedInstance)this.instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(top)).createInstance();
      this.topInstance.position(this.getVisualPosition()).rotation(this.blockOrientation).setChanged();
   }

   public void beginFrame(Context ctx) {
      float interpolatedAngle = ((IBearingBlockEntity)((KineticBlockEntity)this.blockEntity)).getInterpolatedAngle(ctx.partialTick() - 1.0F);
      Quaternionf rot = this.rotationAxis.rotationDegrees(interpolatedAngle);
      rot.mul(this.blockOrientation);
      this.topInstance.rotation(rot).setChanged();
   }

   @Override
   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(new FlatLit[]{this.topInstance});
   }

   @Override
   protected void _delete() {
      super._delete();
      this.topInstance.delete();
   }

   static Quaternionf getBlockStateOrientation(Direction facing) {
      Quaternionf orientation;
      if (facing.getAxis().isHorizontal()) {
         orientation = Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing.getOpposite()));
      } else {
         orientation = new Quaternionf();
      }

      orientation.mul(Axis.XP.rotationDegrees(-90.0F - AngleHelper.verticalAngle(facing)));
      return orientation;
   }

   @Override
   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.topInstance);
   }
}
