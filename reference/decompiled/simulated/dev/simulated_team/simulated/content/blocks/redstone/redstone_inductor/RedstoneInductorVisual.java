package dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class RedstoneInductorVisual extends AbstractBlockEntityVisual<RedstoneInductorBlockEntity> implements SimpleDynamicVisual {
   private final OrientedInstance redstoneIndicator = (OrientedInstance)this.instancerProvider()
      .instancer(InstanceTypes.ORIENTED, Models.partial(SimPartialModels.REDSTONE_INDUCTOR_INDICATOR))
      .createInstance();

   public RedstoneInductorVisual(VisualizationContext ctx, RedstoneInductorBlockEntity blockEntity, float partialTick) {
      super(ctx, blockEntity, partialTick);
      this.redstoneIndicator
         .position(this.getVisualPosition())
         .translatePosition(0.5F, 0.0F, 0.5F)
         .translatePivot(-0.5F, 0.0F, -0.5F)
         .rotateYDegrees(AngleHelper.horizontalAngle((Direction)blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)));
      this.redstoneIndicator.colorArgb(SimColors.redstone(((RedstoneInductorBlockEntity)this.blockEntity).lerpedState.getValue(partialTick) / 15.0F));
   }

   public void beginFrame(Context context) {
      this.redstoneIndicator.colorArgb(SimColors.redstone(((RedstoneInductorBlockEntity)this.blockEntity).lerpedState.getValue(context.partialTick()) / 15.0F));
      this.redstoneIndicator.setChanged();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.redstoneIndicator);
   }

   public void updateLight(float v) {
      this.relight(new FlatLit[]{this.redstoneIndicator});
   }

   protected void _delete() {
      this.redstoneIndicator.delete();
   }
}
