package com.simibubi.create.content.kinetics.gearbox;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import java.util.EnumMap;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class GearboxVisual extends KineticBlockEntityVisual<GearboxBlockEntity> {
   protected final EnumMap<Direction, RotatingInstance> keys = new EnumMap<>(Direction.class);
   protected Direction sourceFacing;

   public GearboxVisual(VisualizationContext context, GearboxBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      Axis boxAxis = (Axis)this.blockState.getValue(BlockStateProperties.AXIS);
      this.updateSourceFacing();
      Instancer<RotatingInstance> instancer = this.instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF));

      for (Direction direction : Iterate.directions) {
         Axis axis = direction.getAxis();
         if (boxAxis != axis) {
            RotatingInstance instance = (RotatingInstance)instancer.createInstance();
            instance.setup(blockEntity, axis, this.getSpeed(direction))
               .setPosition(this.getVisualPosition())
               .rotateToFace(Direction.SOUTH, direction)
               .setChanged();
            this.keys.put((Enum)direction, instance);
         }
      }
   }

   private float getSpeed(Direction direction) {
      float speed = ((GearboxBlockEntity)this.blockEntity).getSpeed();
      if (speed != 0.0F && this.sourceFacing != null) {
         if (this.sourceFacing.getAxis() == direction.getAxis()) {
            speed *= this.sourceFacing == direction ? 1.0F : -1.0F;
         } else if (this.sourceFacing.getAxisDirection() == direction.getAxisDirection()) {
            speed *= -1.0F;
         }
      }

      return speed;
   }

   protected void updateSourceFacing() {
      if (((GearboxBlockEntity)this.blockEntity).hasSource()) {
         BlockPos source = ((GearboxBlockEntity)this.blockEntity).source.subtract(this.pos);
         this.sourceFacing = Direction.getNearest((float)source.getX(), (float)source.getY(), (float)source.getZ());
      } else {
         this.sourceFacing = null;
      }
   }

   public void update(float pt) {
      this.updateSourceFacing();

      for (Entry<Direction, RotatingInstance> key : this.keys.entrySet()) {
         Direction direction = key.getKey();
         Axis axis = direction.getAxis();
         key.getValue().setup((KineticBlockEntity)this.blockEntity, axis, this.getSpeed(direction)).setChanged();
      }
   }

   public void updateLight(float partialTick) {
      this.relight(this.keys.values().toArray(FlatLit[]::new));
   }

   protected void _delete() {
      this.keys.values().forEach(AbstractInstance::delete);
      this.keys.clear();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      this.keys.values().forEach(consumer);
   }
}
