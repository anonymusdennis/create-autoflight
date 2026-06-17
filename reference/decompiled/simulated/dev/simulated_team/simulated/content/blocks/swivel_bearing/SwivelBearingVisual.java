package dev.simulated_team.simulated.content.blocks.swivel_bearing;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import dev.simulated_team.simulated.index.SimPartialModels;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SwivelBearingVisual extends OrientedRotatingVisual<SwivelBearingBlockEntity> {
   private final RotatingInstance topShaft;
   private final RotatingInstance cogInstance;

   public SwivelBearingVisual(VisualizationContext context, SwivelBearingBlockEntity blockEntity, float partialTick) {
      super(
         context,
         blockEntity,
         partialTick,
         Direction.SOUTH,
         ((Direction)blockEntity.getBlockState().getValue(BlockStateProperties.FACING)).getOpposite(),
         Models.partial(SimPartialModels.SHAFT_SIXTEENTH)
      );
      this.topShaft = ((RotatingInstance)this.instancerProvider()
            .instancer(AllInstanceTypes.ROTATING, Models.partial(SimPartialModels.SHAFT_SIXTEENTH))
            .createInstance())
         .rotateToFace(Direction.SOUTH, (Direction)blockEntity.getBlockState().getValue(BlockStateProperties.FACING))
         .setup(blockEntity)
         .setPosition(this.getVisualPosition());
      this.cogInstance = ((RotatingInstance)this.instancerProvider()
            .instancer(AllInstanceTypes.ROTATING, Models.partial(SimPartialModels.SWIVEL_BEARING_COG))
            .createInstance())
         .rotateToFace(Direction.UP, ((Direction)blockEntity.getBlockState().getValue(BlockStateProperties.FACING)).getOpposite())
         .setup(blockEntity.getExtraKinetics())
         .setPosition(this.getVisualPosition());
      this.topShaft.setVisible(!((SwivelBearingBlockEntity)this.blockEntity).isAssembled());
      this.topShaft.setChanged();
      this.cogInstance.setChanged();
   }

   public void update(float pt) {
      super.update(pt);
      this.topShaft.setVisible(!((SwivelBearingBlockEntity)this.blockEntity).isAssembled());
      this.topShaft.setup((KineticBlockEntity)this.blockEntity).setChanged();
      this.cogInstance.setup(((SwivelBearingBlockEntity)this.blockEntity).getExtraKinetics()).setChanged();
   }

   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(new FlatLit[]{this.topShaft});
      this.relight(new FlatLit[]{this.cogInstance});
   }

   protected void _delete() {
      super._delete();
      this.topShaft.delete();
      this.cogInstance.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.topShaft);
      consumer.accept(this.cogInstance);
   }
}
