package com.simibubi.create.content.kinetics.crank;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;

public class ValveHandleVisual extends KineticBlockEntityVisual<HandCrankBlockEntity> implements SimpleDynamicVisual {
   private final TransformedInstance crank;

   public ValveHandleVisual(VisualizationContext modelManager, HandCrankBlockEntity blockEntity, float partialTick) {
      super(modelManager, blockEntity, partialTick);
      BlockState state = blockEntity.getBlockState();
      DyeColor color = null;
      if (state != null && state.getBlock() instanceof ValveHandleBlock vhb) {
         color = vhb.color;
      }

      this.crank = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(color == null ? AllPartialModels.VALVE_HANDLE : AllPartialModels.DYED_VALVE_HANDLES.get(color)))
         .createInstance();
      this.rotateCrank(partialTick);
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
            .rotate(new Quaternionf().rotateTo(0.0F, 1.0F, 0.0F, (float)facing.getStepX(), (float)facing.getStepY(), (float)facing.getStepZ()))
            .uncenter())
         .setChanged();
   }

   protected void _delete() {
      this.crank.delete();
   }

   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.crank});
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.crank);
   }
}
