package com.simibubi.create.content.kinetics.crank;

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
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;

public class HandCrankVisual extends KineticBlockEntityVisual<HandCrankBlockEntity> implements SimpleDynamicVisual {
   private final RotatingInstance rotatingModel;
   private final TransformedInstance crank = (TransformedInstance)this.instancerProvider()
      .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.HAND_CRANK_HANDLE))
      .createInstance();

   public HandCrankVisual(VisualizationContext modelManager, HandCrankBlockEntity blockEntity, float partialTick) {
      super(modelManager, blockEntity, partialTick);
      this.rotateCrank(partialTick);
      this.rotatingModel = (RotatingInstance)this.instancerProvider()
         .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.HAND_CRANK_BASE))
         .createInstance();
      this.rotatingModel
         .setup((KineticBlockEntity)this.blockEntity)
         .setPosition(this.getVisualPosition())
         .rotateToFace((Direction)this.blockState.getValue(BlockStateProperties.FACING))
         .setChanged();
   }

   public void beginFrame(Context ctx) {
      this.rotateCrank(ctx.partialTick());
   }

   private void rotateCrank(float pt) {
      Direction facing = (Direction)this.blockState.getValue(BlockStateProperties.FACING);
      float angle = AngleHelper.rad((double)((HandCrankBlockEntity)this.blockEntity).getIndependentAngle(pt));
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.crank
                     .setIdentityTransform()
                     .translate(this.getVisualPosition()))
                  .center())
               .rotate(angle, Direction.get(AxisDirection.POSITIVE, facing.getAxis())))
            .rotate(new Quaternionf().rotateTo(0.0F, 0.0F, -1.0F, (float)facing.getStepX(), (float)facing.getStepY(), (float)facing.getStepZ()))
            .uncenter())
         .setChanged();
   }

   protected void _delete() {
      this.crank.delete();
      this.rotatingModel.delete();
   }

   public void update(float pt) {
      this.rotatingModel.setup((KineticBlockEntity)this.blockEntity).setChanged();
   }

   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.crank, this.rotatingModel});
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.crank);
      consumer.accept(this.rotatingModel);
   }
}
