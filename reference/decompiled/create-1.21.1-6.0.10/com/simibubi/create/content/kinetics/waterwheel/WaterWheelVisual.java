package com.simibubi.create.content.kinetics.waterwheel;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import java.util.function.Consumer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;

public class WaterWheelVisual<T extends WaterWheelBlockEntity> extends KineticBlockEntityVisual<T> {
   private static final RendererReloadCache<WaterWheelVisual.ModelKey, Model> MODEL_CACHE = new RendererReloadCache(WaterWheelVisual::createModel);
   protected final boolean large;
   protected BlockState lastMaterial;
   protected RotatingInstance rotatingModel;

   public WaterWheelVisual(VisualizationContext context, T blockEntity, boolean large, float partialTick) {
      super(context, blockEntity, partialTick);
      this.large = large;
      this.setupInstance();
   }

   public static <T extends WaterWheelBlockEntity> WaterWheelVisual<T> standard(VisualizationContext context, T blockEntity, float partialTick) {
      return new WaterWheelVisual<>(context, blockEntity, false, partialTick);
   }

   public static <T extends WaterWheelBlockEntity> WaterWheelVisual<T> large(VisualizationContext context, T blockEntity, float partialTick) {
      return new WaterWheelVisual<>(context, blockEntity, true, partialTick);
   }

   private void setupInstance() {
      this.lastMaterial = ((WaterWheelBlockEntity)this.blockEntity).material;
      this.rotatingModel = (RotatingInstance)this.instancerProvider()
         .instancer(
            AllInstanceTypes.ROTATING,
            (Model)MODEL_CACHE.get(
               new WaterWheelVisual.ModelKey(WaterWheelRenderer.Variant.of(this.large, this.blockState), ((WaterWheelBlockEntity)this.blockEntity).material)
            )
         )
         .createInstance();
      this.rotatingModel.setup((KineticBlockEntity)this.blockEntity).setPosition(this.getVisualPosition()).rotateToFace(this.rotationAxis()).setChanged();
   }

   public void update(float pt) {
      if (this.lastMaterial != ((WaterWheelBlockEntity)this.blockEntity).material) {
         this.rotatingModel.delete();
         this.setupInstance();
      } else {
         this.rotatingModel.setup((KineticBlockEntity)this.blockEntity).setChanged();
      }
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

   private static Model createModel(WaterWheelVisual.ModelKey key) {
      BakedModel model = WaterWheelRenderer.generateModel(key.variant(), key.material());
      return new BakedModelBuilder(model).build();
   }

   public static record ModelKey(WaterWheelRenderer.Variant variant, BlockState material) {
   }
}
