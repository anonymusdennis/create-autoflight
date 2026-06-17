package dev.engine_room.flywheel.backend.engine;

import com.mojang.datafixers.util.Pair;
import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.backend.FlwBackend;
import dev.engine_room.flywheel.backend.engine.embed.Environment;
import dev.engine_room.flywheel.backend.engine.embed.EnvironmentStorage;
import dev.engine_room.flywheel.lib.task.ForEachPlan;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import net.minecraft.client.resources.model.ModelBakery;
import org.jetbrains.annotations.Nullable;

public abstract class DrawManager<N extends AbstractInstancer<?>> {
   private static final boolean MODEL_WARNINGS = Boolean.getBoolean("flywheel.modelWarnings");
   protected final Map<InstancerKey<?>, N> instancers = new ConcurrentHashMap<>();
   protected final Queue<DrawManager.UninitializedInstancer<N, ?>> initializationQueue = new ConcurrentLinkedQueue<>();
   protected final Function<InstancerKey<?>, N> createAndDeferInit = this::createAndDeferInit;

   public <I extends Instance> AbstractInstancer<I> getInstancer(Environment environment, InstanceType<I> type, Model model, int bias) {
      return this.getInstancer(new InstancerKey<>(environment, type, model, bias));
   }

   public <I extends Instance> AbstractInstancer<I> getInstancer(InstancerKey<I> key) {
      return this.instancers.computeIfAbsent(key, this.createAndDeferInit);
   }

   public Plan<RenderContext> createFramePlan() {
      return ForEachPlan.of(() -> new ArrayList<>(this.instancers.values()), AbstractInstancer::parallelUpdate);
   }

   public void render(LightStorage lightStorage, EnvironmentStorage environmentStorage) {
      for (DrawManager.UninitializedInstancer<N, ?> init : this.initializationQueue) {
         N instancer = init.instancer();
         if (instancer.instanceCount() > 0) {
            this.initialize(init.key(), instancer);
         } else {
            this.instancers.remove(init.key());
         }
      }

      this.initializationQueue.clear();
   }

   public void onRenderOriginChanged() {
      this.instancers.values().forEach(AbstractInstancer::clear);
   }

   public abstract void renderCrumbling(List<Engine.CrumblingBlock> var1);

   protected abstract <I extends Instance> N create(InstancerKey<I> var1);

   protected abstract <I extends Instance> void initialize(InstancerKey<I> var1, N var2);

   private N createAndDeferInit(InstancerKey<?> key) {
      N out = this.create(key);
      if (modelHasNoIssues(key.model())) {
         this.initializationQueue.add(new DrawManager.UninitializedInstancer<>(key, out));
      }

      return out;
   }

   private static boolean modelHasNoIssues(Model model) {
      if (model.meshes().isEmpty()) {
         if (MODEL_WARNINGS) {
            StringBuilder builder = new StringBuilder();
            builder.append("Creating an instancer for a model with no meshes! Stack trace:");
            StackWalker.getInstance().forEach(f -> builder.append("\n\t").append(f.toString()));
            FlwBackend.LOGGER.warn(builder.toString());
         }

         return false;
      } else {
         List<Model.ConfiguredMesh> meshes = model.meshes();

         for (int i = 0; i < meshes.size(); i++) {
            Model.ConfiguredMesh mesh = meshes.get(i);
            if (!MaterialRenderState.materialIsAllNonNull(mesh.material())) {
               if (MODEL_WARNINGS) {
                  StringBuilder builder = new StringBuilder();
                  builder.append("ConfiguredMesh at index ").append(i).append(" has null components in its material! Stack trace:");
                  StackWalker.getInstance().forEach(f -> builder.append("\n\t").append(f.toString()));
                  FlwBackend.LOGGER.warn(builder.toString());
               }

               return false;
            }
         }

         return true;
      }
   }

   protected static <I extends AbstractInstancer<?>> Map<GroupKey<?>, Int2ObjectMap<List<Pair<I, InstanceHandleImpl<?>>>>> doCrumblingSort(
      List<Engine.CrumblingBlock> crumblingBlocks, DrawManager.State2Instancer<I> cast
   ) {
      Map<GroupKey<?>, Int2ObjectMap<List<Pair<I, InstanceHandleImpl<?>>>>> byType = new HashMap<>();

      for (Engine.CrumblingBlock block : crumblingBlocks) {
         int progress = block.progress();
         if (progress >= 0 && progress < ModelBakery.DESTROY_TYPES.size()) {
            for (Instance instance : block.instances()) {
               InstanceHandle instancer = instance.handle();
               if (instancer instanceof InstanceHandleImpl) {
                  InstanceHandleImpl<?> impl = (InstanceHandleImpl<?>)instancer;
                  I instancerx = cast.apply(impl.state);
                  if (instancerx != null) {
                     ((List)byType.computeIfAbsent(new GroupKey(instancerx.type, instancerx.environment), $ -> new Int2ObjectArrayMap())
                           .computeIfAbsent(progress, $ -> new ArrayList()))
                        .add(Pair.of(instancerx, impl));
                  }
               }
            }
         }
      }

      return byType;
   }

   public void delete() {
      this.instancers.clear();
      this.initializationQueue.clear();
   }

   public abstract void triggerFallback();

   public abstract MeshPool meshPool();

   public Map<InstancerKey<?>, N> instancers() {
      return this.instancers;
   }

   @FunctionalInterface
   protected interface State2Instancer<I extends AbstractInstancer<?>> {
      @Nullable
      I apply(InstanceHandleImpl.State<?> var1);
   }

   protected static record UninitializedInstancer<N, I extends Instance>(InstancerKey<I> key, N instancer) {
   }
}
