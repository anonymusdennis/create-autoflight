package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.ColoredLitInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.simulated_team.simulated.index.SimPartialModels;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

public class SteeringWheelVisual extends KineticBlockEntityVisual<SteeringWheelBlockEntity> implements SimpleDynamicVisual {
   private static final RendererReloadCache<SteeringWheelRenderer.ModelKey, Model> MODEL_CACHE = new RendererReloadCache(SteeringWheelVisual::generateModel);
   private final RotatingInstance halfShaftInstance;
   private final Direction facing;
   private final boolean onFloor;
   private TransformedInstance wheelInstance;
   private BlockState lastMaterial;
   private final List<ColoredLitInstance> allInstances = new ArrayList<>();

   public SteeringWheelVisual(VisualizationContext ctx, SteeringWheelBlockEntity blockEntity, float partialTick) {
      super(ctx, blockEntity, partialTick);
      this.facing = (Direction)blockEntity.getBlockState().getValue(SteeringWheelBlock.FACING);
      this.onFloor = (Boolean)blockEntity.getBlockState().getValue(SteeringWheelBlock.ON_FLOOR);
      this.setupWheel(partialTick);
      this.halfShaftInstance = ((RotatingInstance)this.instancerProvider()
            .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
            .createInstance())
         .setup(blockEntity, Axis.X)
         .rotateToFace(this.onFloor ? Direction.SOUTH : Direction.NORTH)
         .setPosition(this.getVisualPosition());
      this.allInstances.add(this.halfShaftInstance);
   }

   public void update(float partialTick) {
      super.update(partialTick);
      this.halfShaftInstance.setup((KineticBlockEntity)this.blockEntity, Axis.Y).setChanged();
   }

   public void beginFrame(Context context) {
      if (((SteeringWheelBlockEntity)this.blockEntity).material != this.lastMaterial) {
         this.wheelInstance.delete();
         this.allInstances.remove(this.wheelInstance);
         this.setupWheel(context.partialTick());
      } else {
         this.transformWheel(context.partialTick());
      }
   }

   private void setupWheel(float partialTicks) {
      this.lastMaterial = ((SteeringWheelBlockEntity)this.blockEntity).material;
      this.wheelInstance = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, (Model)MODEL_CACHE.get(new SteeringWheelRenderer.ModelKey(this.lastMaterial)))
         .createInstance();
      this.transformWheel(partialTicks);
      this.allInstances.add(this.wheelInstance);
      this.relight(new FlatLit[]{this.wheelInstance});
      this.wheelInstance.setChanged();
   }

   private void transformWheel(float partialTicks) {
      this.wheelInstance.setIdentityTransform();
      ((TransformedInstance)this.wheelInstance.translate(this.getVisualPosition())).rotateCentered(this.facing.getRotation());
      if (this.onFloor) {
         this.wheelInstance.translate(0.0, 0.40625, -0.3125);
      } else {
         this.wheelInstance.translate(0.0, 0.40625, 0.3125);
      }

      this.wheelInstance.rotateCentered(((SteeringWheelBlockEntity)this.blockEntity).getRenderAngle(partialTicks), Direction.UP);
      this.wheelInstance.setChanged();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      for (ColoredLitInstance inst : this.allInstances) {
         consumer.accept(inst);
      }
   }

   public void updateLight(float v) {
      for (ColoredLitInstance inst : this.allInstances) {
         this.relight(new FlatLit[]{inst});
      }
   }

   protected void _delete() {
      for (ColoredLitInstance inst : this.allInstances) {
         inst.delete();
      }
   }

   private static Model generateModel(SteeringWheelRenderer.ModelKey modelKey) {
      BakedModel bakedModel = SteeringWheelRenderer.generateModel(SimPartialModels.STEERING_WHEEL.get(), modelKey.material());
      return new BakedModelBuilder(bakedModel).build();
   }
}
