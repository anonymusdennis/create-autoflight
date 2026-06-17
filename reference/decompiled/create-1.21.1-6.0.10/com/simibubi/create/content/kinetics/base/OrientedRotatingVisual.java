package com.simibubi.create.content.kinetics.base;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer.Factory;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class OrientedRotatingVisual<T extends KineticBlockEntity> extends KineticBlockEntityVisual<T> {
   protected final RotatingInstance rotatingModel;

   public OrientedRotatingVisual(VisualizationContext context, T blockEntity, float partialTick, Direction from, Direction to, Model model) {
      super(context, blockEntity, partialTick);
      this.rotatingModel = ((RotatingInstance)this.instancerProvider().instancer(AllInstanceTypes.ROTATING, model).createInstance())
         .rotateToFace(from, to)
         .setup(blockEntity)
         .setPosition(this.getVisualPosition());
      this.rotatingModel.setChanged();
   }

   public static <T extends KineticBlockEntity> Factory<T> of(PartialModel partial) {
      return (context, blockEntity, partialTick) -> {
         Direction facing = (Direction)blockEntity.getBlockState().getValue(BlockStateProperties.FACING);
         return new OrientedRotatingVisual<KineticBlockEntity>(context, blockEntity, partialTick, Direction.SOUTH, facing, Models.partial(partial));
      };
   }

   public static <T extends KineticBlockEntity> Factory<T> backHorizontal(PartialModel partial) {
      return (context, blockEntity, partialTick) -> {
         Direction facing = ((Direction)blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)).getOpposite();
         return new OrientedRotatingVisual<KineticBlockEntity>(context, blockEntity, partialTick, Direction.SOUTH, facing, Models.partial(partial));
      };
   }

   public static BlockEntityVisual<? super GantryShaftBlockEntity> gantryShaft(
      VisualizationContext visualizationContext, GantryShaftBlockEntity gantryShaftBlockEntity, float partialTick
   ) {
      BlockState blockState = gantryShaftBlockEntity.getBlockState();
      GantryShaftBlock.Part part = (GantryShaftBlock.Part)blockState.getValue(GantryShaftBlock.PART);
      boolean isPowered = (Boolean)blockState.getValue(GantryShaftBlock.POWERED);
      boolean isFlipped = ((Direction)blockState.getValue(GantryShaftBlock.FACING)).getAxisDirection() == AxisDirection.NEGATIVE;
      Model model = Models.partial(AllPartialModels.GANTRY_SHAFTS.get(new AllPartialModels.GantryShaftKey(part, isPowered, isFlipped)));
      return new OrientedRotatingVisual(
         visualizationContext, gantryShaftBlockEntity, partialTick, Direction.UP, (Direction)blockState.getValue(GantryShaftBlock.FACING), model
      );
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
