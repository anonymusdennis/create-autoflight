package com.simibubi.create.content.kinetics.simpleRelays.encased;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import java.util.function.Consumer;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import org.jetbrains.annotations.Nullable;

public class EncasedCogVisual extends KineticBlockEntityVisual<KineticBlockEntity> {
   private final boolean large;
   protected final RotatingInstance rotatingModel;
   @Nullable
   protected final RotatingInstance rotatingTopShaft;
   @Nullable
   protected final RotatingInstance rotatingBottomShaft;

   public static EncasedCogVisual small(VisualizationContext modelManager, KineticBlockEntity blockEntity, float partialTick) {
      return new EncasedCogVisual(modelManager, blockEntity, false, partialTick, Models.partial(AllPartialModels.SHAFTLESS_COGWHEEL));
   }

   public static EncasedCogVisual large(VisualizationContext modelManager, KineticBlockEntity blockEntity, float partialTick) {
      return new EncasedCogVisual(modelManager, blockEntity, true, partialTick, Models.partial(AllPartialModels.SHAFTLESS_LARGE_COGWHEEL));
   }

   public EncasedCogVisual(VisualizationContext modelManager, KineticBlockEntity blockEntity, boolean large, float partialTick, Model model) {
      super(modelManager, blockEntity, partialTick);
      this.large = large;
      this.rotatingModel = (RotatingInstance)this.instancerProvider().instancer(AllInstanceTypes.ROTATING, model).createInstance();
      this.rotatingModel.setup(blockEntity).setPosition(this.getVisualPosition()).rotateToFace(this.rotationAxis()).setChanged();
      RotatingInstance rotatingTopShaft = null;
      RotatingInstance rotatingBottomShaft = null;
      if (this.blockState.getBlock() instanceof IRotate def) {
         for (Direction d : Iterate.directionsInAxis(this.rotationAxis())) {
            if (def.hasShaftTowards(blockEntity.getLevel(), blockEntity.getBlockPos(), this.blockState, d)) {
               RotatingInstance instance = (RotatingInstance)this.instancerProvider()
                  .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
                  .createInstance();
               instance.setup(blockEntity).setPosition(this.getVisualPosition()).rotateToFace(Direction.SOUTH, d).setChanged();
               if (large) {
                  instance.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(this.rotationAxis(), this.pos));
               }

               if (d.getAxisDirection() == AxisDirection.POSITIVE) {
                  rotatingTopShaft = instance;
               } else {
                  rotatingBottomShaft = instance;
               }
            }
         }
      }

      this.rotatingTopShaft = rotatingTopShaft;
      this.rotatingBottomShaft = rotatingBottomShaft;
   }

   public void update(float pt) {
      this.rotatingModel.setup((KineticBlockEntity)this.blockEntity).setChanged();
      if (this.rotatingTopShaft != null) {
         this.rotatingTopShaft.setup((KineticBlockEntity)this.blockEntity).setChanged();
      }

      if (this.rotatingBottomShaft != null) {
         this.rotatingBottomShaft.setup((KineticBlockEntity)this.blockEntity).setChanged();
      }
   }

   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.rotatingModel, this.rotatingTopShaft, this.rotatingBottomShaft});
   }

   protected void _delete() {
      this.rotatingModel.delete();
      if (this.rotatingTopShaft != null) {
         this.rotatingTopShaft.delete();
      }

      if (this.rotatingBottomShaft != null) {
         this.rotatingBottomShaft.delete();
      }
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.rotatingModel);
      consumer.accept(this.rotatingTopShaft);
      consumer.accept(this.rotatingBottomShaft);
   }
}
