package com.simibubi.create.content.kinetics.simpleRelays;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import java.util.function.Consumer;
import net.minecraft.core.Direction.Axis;

public class BracketedKineticBlockEntityVisual {
   public static BlockEntityVisual<BracketedKineticBlockEntity> create(VisualizationContext context, BracketedKineticBlockEntity blockEntity, float partialTick) {
      if (ICogWheel.isLargeCog(blockEntity.getBlockState())) {
         return new BracketedKineticBlockEntityVisual.LargeCogVisual(context, blockEntity, partialTick);
      } else {
         Model model;
         if (AllBlocks.COGWHEEL.is(blockEntity.getBlockState().getBlock())) {
            model = Models.partial(AllPartialModels.COGWHEEL);
         } else {
            model = Models.partial(AllPartialModels.SHAFT);
         }

         return new SingleAxisRotatingVisual<BracketedKineticBlockEntity>(context, blockEntity, partialTick, model);
      }
   }

   public static class LargeCogVisual extends SingleAxisRotatingVisual<BracketedKineticBlockEntity> {
      protected final RotatingInstance additionalShaft;

      private LargeCogVisual(VisualizationContext context, BracketedKineticBlockEntity blockEntity, float partialTick) {
         super(context, blockEntity, partialTick, Models.partial(AllPartialModels.SHAFTLESS_LARGE_COGWHEEL));
         Axis axis = KineticBlockEntityRenderer.getRotationAxisOf(blockEntity);
         this.additionalShaft = (RotatingInstance)this.instancerProvider()
            .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.COGWHEEL_SHAFT))
            .createInstance();
         this.additionalShaft
            .rotateToFace(axis)
            .setup(blockEntity)
            .setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(axis, this.pos))
            .setPosition(this.getVisualPosition())
            .setChanged();
      }

      @Override
      public void update(float pt) {
         super.update(pt);
         this.additionalShaft
            .setup((KineticBlockEntity)this.blockEntity)
            .setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(this.rotationAxis(), this.pos))
            .setChanged();
      }

      @Override
      public void updateLight(float partialTick) {
         super.updateLight(partialTick);
         this.relight(new FlatLit[]{this.additionalShaft});
      }

      @Override
      protected void _delete() {
         super._delete();
         this.additionalShaft.delete();
      }

      @Override
      public void collectCrumblingInstances(Consumer<Instance> consumer) {
         super.collectCrumblingInstances(consumer);
         consumer.accept(this.additionalShaft);
      }
   }
}
