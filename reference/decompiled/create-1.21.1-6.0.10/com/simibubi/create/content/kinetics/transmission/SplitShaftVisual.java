package com.simibubi.create.content.kinetics.transmission;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;

public class SplitShaftVisual extends KineticBlockEntityVisual<SplitShaftBlockEntity> {
   protected final ArrayList<RotatingInstance> keys = new ArrayList<>(2);

   public SplitShaftVisual(VisualizationContext modelManager, SplitShaftBlockEntity blockEntity, float partialTick) {
      super(modelManager, blockEntity, partialTick);
      float speed = blockEntity.getSpeed();

      for (Direction dir : Iterate.directionsInAxis(this.rotationAxis())) {
         float splitSpeed = speed * blockEntity.getRotationSpeedModifier(dir);
         RotatingInstance instance = (RotatingInstance)this.instancerProvider()
            .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
            .createInstance();
         instance.setup(blockEntity, splitSpeed).setPosition(this.getVisualPosition()).rotateToFace(Direction.SOUTH, dir).setChanged();
         this.keys.add(instance);
      }
   }

   public void update(float pt) {
      Block block = this.blockState.getBlock();
      Axis boxAxis = ((IRotate)block).getRotationAxis(this.blockState);
      Direction[] directions = Iterate.directionsInAxis(boxAxis);

      for (int i : Iterate.zeroAndOne) {
         this.keys
            .get(i)
            .setup(
               (KineticBlockEntity)this.blockEntity,
               ((SplitShaftBlockEntity)this.blockEntity).getSpeed() * ((SplitShaftBlockEntity)this.blockEntity).getRotationSpeedModifier(directions[i])
            )
            .setChanged();
      }
   }

   public void updateLight(float partialTick) {
      this.relight(this.keys.toArray(FlatLit[]::new));
   }

   protected void _delete() {
      this.keys.forEach(AbstractInstance::delete);
      this.keys.clear();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      this.keys.forEach(consumer);
   }
}
