package com.simibubi.create.content.kinetics.base;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.equipment.armor.BacktankRenderer;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.TickableVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer.Factory;
import java.util.function.Consumer;
import net.minecraft.core.Direction;

public class SingleAxisRotatingVisual<T extends KineticBlockEntity> extends KineticBlockEntityVisual<T> implements SimpleTickableVisual {
   public static boolean rainbowMode = false;
   protected final RotatingInstance rotatingModel;

   public SingleAxisRotatingVisual(VisualizationContext context, T blockEntity, float partialTick, Model model) {
      this(context, blockEntity, partialTick, Direction.UP, model);
   }

   public SingleAxisRotatingVisual(VisualizationContext context, T blockEntity, float partialTick, Direction from, Model model) {
      super(context, blockEntity, partialTick);
      this.rotatingModel = ((RotatingInstance)this.instancerProvider().instancer(AllInstanceTypes.ROTATING, model).createInstance())
         .rotateToFace(from, this.rotationAxis())
         .setup(blockEntity)
         .setPosition(this.getVisualPosition());
      this.rotatingModel.setChanged();
   }

   public static <T extends KineticBlockEntity> Factory<T> of(PartialModel partial) {
      return (context, blockEntity, partialTick) -> new SingleAxisRotatingVisual<KineticBlockEntity>(context, blockEntity, partialTick, Models.partial(partial));
   }

   public static <T extends KineticBlockEntity> Factory<T> ofZ(PartialModel partial) {
      return (context, blockEntity, partialTick) -> new SingleAxisRotatingVisual<KineticBlockEntity>(
            context, blockEntity, partialTick, Direction.SOUTH, Models.partial(partial)
         );
   }

   public static <T extends KineticBlockEntity> SingleAxisRotatingVisual<T> shaft(VisualizationContext context, T blockEntity, float partialTick) {
      return new SingleAxisRotatingVisual<>(context, blockEntity, partialTick, Models.partial(AllPartialModels.SHAFT));
   }

   public static <T extends KineticBlockEntity> SingleAxisRotatingVisual<T> backtank(VisualizationContext context, T blockEntity, float partialTick) {
      Model model = Models.partial(BacktankRenderer.getShaftModel(blockEntity.getBlockState()));
      return new SingleAxisRotatingVisual<>(context, blockEntity, partialTick, model);
   }

   public void update(float pt) {
      this.rotatingModel.setup((KineticBlockEntity)this.blockEntity).setChanged();
   }

   public void tick(Context context) {
      applyOverstressEffect((KineticBlockEntity)this.blockEntity, new RotatingInstance[]{this.rotatingModel});
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
