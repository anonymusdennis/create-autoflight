package com.simibubi.create.content.kinetics.fan;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class FanVisual extends KineticBlockEntityVisual<EncasedFanBlockEntity> {
   protected final RotatingInstance shaft;
   protected final RotatingInstance fan;
   final Direction direction = (Direction)this.blockState.getValue(BlockStateProperties.FACING);
   private final Direction opposite = this.direction.getOpposite();

   public FanVisual(VisualizationContext context, EncasedFanBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      this.shaft = (RotatingInstance)this.instancerProvider()
         .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
         .createInstance();
      this.fan = (RotatingInstance)this.instancerProvider()
         .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.ENCASED_FAN_INNER))
         .createInstance();
      this.shaft.setup(blockEntity).setPosition(this.getVisualPosition()).rotateToFace(Direction.SOUTH, this.opposite).setChanged();
      this.fan.setup(blockEntity, this.getFanSpeed()).setPosition(this.getVisualPosition()).rotateToFace(Direction.SOUTH, this.opposite).setChanged();
   }

   private float getFanSpeed() {
      float speed = ((EncasedFanBlockEntity)this.blockEntity).getSpeed() * 5.0F;
      if (speed > 0.0F) {
         speed = Mth.clamp(speed, 80.0F, 1280.0F);
      }

      if (speed < 0.0F) {
         speed = Mth.clamp(speed, -1280.0F, -80.0F);
      }

      return speed;
   }

   public void update(float pt) {
      this.shaft.setup((KineticBlockEntity)this.blockEntity).setChanged();
      this.fan.setup((KineticBlockEntity)this.blockEntity, this.getFanSpeed()).setChanged();
   }

   public void updateLight(float partialTick) {
      BlockPos behind = this.pos.relative(this.opposite);
      this.relight(behind, new FlatLit[]{this.shaft});
      BlockPos inFront = this.pos.relative(this.direction);
      this.relight(inFront, new FlatLit[]{this.fan});
   }

   protected void _delete() {
      this.shaft.delete();
      this.fan.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.shaft);
      consumer.accept(this.fan);
   }
}
