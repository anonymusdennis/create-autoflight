package com.simibubi.create.content.kinetics.saw;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SawVisual extends KineticBlockEntityVisual<SawBlockEntity> {
   protected final RotatingInstance rotatingModel;

   public SawVisual(VisualizationContext context, SawBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      this.rotatingModel = shaft(this.instancerProvider(), this.blockState).setup(blockEntity).setPosition(this.getVisualPosition());
      this.rotatingModel.setChanged();
   }

   public static RotatingInstance shaft(InstancerProvider instancerProvider, BlockState state) {
      Direction facing = (Direction)state.getValue(BlockStateProperties.FACING);
      Axis axis = facing.getAxis();
      if (axis.isHorizontal()) {
         Direction align = facing.getOpposite();
         return ((RotatingInstance)instancerProvider.instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF)).createInstance())
            .rotateTo(0.0F, 0.0F, 1.0F, (float)align.getStepX(), (float)align.getStepY(), (float)align.getStepZ());
      } else {
         return ((RotatingInstance)instancerProvider.instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT)).createInstance())
            .rotateToFace(state.getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE) ? Axis.X : Axis.Z);
      }
   }

   public void update(float pt) {
      this.rotatingModel.setup((KineticBlockEntity)this.blockEntity).setChanged();
   }

   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.rotatingModel});
   }

   protected void _delete() {
      this.rotatingModel.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.rotatingModel);
   }
}
