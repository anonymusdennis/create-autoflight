package dev.engine_room.flywheel.impl.visualization.storage;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.LightUpdatedVisual;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visual.Visual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.task.ConditionalPlan;
import dev.engine_room.flywheel.lib.task.PlanMap;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public abstract class Storage<T> {
   private final Map<T, Visual> visuals = new Reference2ObjectOpenHashMap();
   protected final PlanMap<DynamicVisual, DynamicVisual.Context> dynamicVisuals = new PlanMap<>();
   protected final PlanMap<TickableVisual, TickableVisual.Context> tickableVisuals = new PlanMap<>();
   protected final List<SimpleDynamicVisual> simpleDynamicVisuals = new ArrayList<>();
   protected final List<SimpleTickableVisual> simpleTickableVisuals = new ArrayList<>();
   protected final LightUpdatedVisualStorage lightUpdatedVisuals = new LightUpdatedVisualStorage();
   protected final ShaderLightVisualStorage shaderLightVisuals = new ShaderLightVisualStorage();

   public Collection<Visual> getAllVisuals() {
      return this.visuals.values();
   }

   public Plan<DynamicVisual.Context> framePlan() {
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.NullPointerException: Cannot read field "parameterTypes" because the return value of "org.jetbrains.java.decompiler.struct.StructMethod.getSignature()" is null
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.getInferredExprType(InvocationExprent.java:472)
      //   at org.jetbrains.java.decompiler.modules.decompiler.GenericsProcessor.qualifyChain(GenericsProcessor.java:78)
      //   at org.jetbrains.java.decompiler.modules.decompiler.GenericsProcessor.qualifyChain(GenericsProcessor.java:40)
      //   at org.jetbrains.java.decompiler.modules.decompiler.GenericsProcessor.qualifyChain(GenericsProcessor.java:36)
      //   at org.jetbrains.java.decompiler.modules.decompiler.GenericsProcessor.qualifyChains(GenericsProcessor.java:21)
      //   at org.jetbrains.java.decompiler.modules.decompiler.GenericsProcessor.qualifyChains(GenericsProcessor.java:26)
      //   at org.jetbrains.java.decompiler.main.rels.MethodProcessor.codeToJava(MethodProcessor.java:435)
      //
      // Bytecode:
      // 00: invokedynamic getAsBoolean ()Ldev/engine_room/flywheel/lib/task/functional/BooleanSupplierWithContext$Ignored; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ ()Z, dev/engine_room/flywheel/impl/visualization/storage/Storage.lambda$framePlan$0 ()Z, ()Z ]
      // 05: invokestatic dev/engine_room/flywheel/lib/task/ConditionalPlan.on (Ldev/engine_room/flywheel/lib/task/functional/BooleanSupplierWithContext$Ignored;)Ldev/engine_room/flywheel/lib/task/ConditionalPlan$Builder;
      // 08: bipush 2
      // 09: anewarray 126
      // 0c: dup
      // 0d: bipush 0
      // 0e: aload 0
      // 0f: getfield dev/engine_room/flywheel/impl/visualization/storage/Storage.dynamicVisuals Ldev/engine_room/flywheel/lib/task/PlanMap;
      // 12: aastore
      // 13: dup
      // 14: bipush 1
      // 15: aload 0
      // 16: invokedynamic get (Ldev/engine_room/flywheel/impl/visualization/storage/Storage;)Ldev/engine_room/flywheel/lib/task/functional/SupplierWithContext$Ignored; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ ()Ljava/lang/Object;, dev/engine_room/flywheel/impl/visualization/storage/Storage.lambda$framePlan$1 ()Ljava/util/List;, ()Ljava/util/List; ]
      // 1b: invokedynamic accept ()Ldev/engine_room/flywheel/lib/task/functional/ConsumerWithContext; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ (Ljava/lang/Object;Ljava/lang/Object;)V, dev/engine_room/flywheel/lib/visual/SimpleDynamicVisual.beginFrame (Ldev/engine_room/flywheel/api/visual/DynamicVisual$Context;)V, (Ldev/engine_room/flywheel/lib/visual/SimpleDynamicVisual;Ldev/engine_room/flywheel/api/visual/DynamicVisual$Context;)V ]
      // 20: invokestatic dev/engine_room/flywheel/lib/task/ForEachPlan.of (Ldev/engine_room/flywheel/lib/task/functional/SupplierWithContext$Ignored;Ldev/engine_room/flywheel/lib/task/functional/ConsumerWithContext;)Ldev/engine_room/flywheel/lib/task/ForEachPlan;
      // 23: aastore
      // 24: invokestatic dev/engine_room/flywheel/lib/task/NestedPlan.of ([Ldev/engine_room/flywheel/api/task/Plan;)Ldev/engine_room/flywheel/lib/task/NestedPlan;
      // 27: invokevirtual dev/engine_room/flywheel/lib/task/ConditionalPlan$Builder.then (Ldev/engine_room/flywheel/api/task/Plan;)Ldev/engine_room/flywheel/lib/task/ConditionalPlan;
      // 2a: astore 1
      // 2b: bipush 2
      // 2c: anewarray 126
      // 2f: dup
      // 30: bipush 0
      // 31: aload 0
      // 32: getfield dev/engine_room/flywheel/impl/visualization/storage/Storage.lightUpdatedVisuals Ldev/engine_room/flywheel/impl/visualization/storage/LightUpdatedVisualStorage;
      // 35: invokevirtual dev/engine_room/flywheel/impl/visualization/storage/LightUpdatedVisualStorage.plan ()Ldev/engine_room/flywheel/api/task/Plan;
      // 38: aastore
      // 39: dup
      // 3a: bipush 1
      // 3b: aload 1
      // 3c: aastore
      // 3d: invokestatic dev/engine_room/flywheel/lib/task/NestedPlan.of ([Ldev/engine_room/flywheel/api/task/Plan;)Ldev/engine_room/flywheel/lib/task/NestedPlan;
      // 40: areturn
   }

   public Plan<TickableVisual.Context> tickPlan() {
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.NullPointerException: Cannot read field "parameterTypes" because the return value of "org.jetbrains.java.decompiler.struct.StructMethod.getSignature()" is null
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.getInferredExprType(InvocationExprent.java:472)
      //   at org.jetbrains.java.decompiler.modules.decompiler.GenericsProcessor.qualifyChain(GenericsProcessor.java:78)
      //   at org.jetbrains.java.decompiler.modules.decompiler.GenericsProcessor.qualifyChain(GenericsProcessor.java:40)
      //   at org.jetbrains.java.decompiler.modules.decompiler.GenericsProcessor.qualifyChain(GenericsProcessor.java:36)
      //   at org.jetbrains.java.decompiler.modules.decompiler.GenericsProcessor.qualifyChains(GenericsProcessor.java:21)
      //   at org.jetbrains.java.decompiler.modules.decompiler.GenericsProcessor.qualifyChains(GenericsProcessor.java:26)
      //   at org.jetbrains.java.decompiler.main.rels.MethodProcessor.codeToJava(MethodProcessor.java:435)
      //
      // Bytecode:
      // 00: invokedynamic getAsBoolean ()Ldev/engine_room/flywheel/lib/task/functional/BooleanSupplierWithContext$Ignored; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ ()Z, dev/engine_room/flywheel/impl/visualization/storage/Storage.lambda$tickPlan$2 ()Z, ()Z ]
      // 05: invokestatic dev/engine_room/flywheel/lib/task/ConditionalPlan.on (Ldev/engine_room/flywheel/lib/task/functional/BooleanSupplierWithContext$Ignored;)Ldev/engine_room/flywheel/lib/task/ConditionalPlan$Builder;
      // 08: bipush 2
      // 09: anewarray 126
      // 0c: dup
      // 0d: bipush 0
      // 0e: aload 0
      // 0f: getfield dev/engine_room/flywheel/impl/visualization/storage/Storage.tickableVisuals Ldev/engine_room/flywheel/lib/task/PlanMap;
      // 12: aastore
      // 13: dup
      // 14: bipush 1
      // 15: aload 0
      // 16: invokedynamic get (Ldev/engine_room/flywheel/impl/visualization/storage/Storage;)Ldev/engine_room/flywheel/lib/task/functional/SupplierWithContext$Ignored; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ ()Ljava/lang/Object;, dev/engine_room/flywheel/impl/visualization/storage/Storage.lambda$tickPlan$3 ()Ljava/util/List;, ()Ljava/util/List; ]
      // 1b: invokedynamic accept ()Ldev/engine_room/flywheel/lib/task/functional/ConsumerWithContext; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ (Ljava/lang/Object;Ljava/lang/Object;)V, dev/engine_room/flywheel/lib/visual/SimpleTickableVisual.tick (Ldev/engine_room/flywheel/api/visual/TickableVisual$Context;)V, (Ldev/engine_room/flywheel/lib/visual/SimpleTickableVisual;Ldev/engine_room/flywheel/api/visual/TickableVisual$Context;)V ]
      // 20: invokestatic dev/engine_room/flywheel/lib/task/ForEachPlan.of (Ldev/engine_room/flywheel/lib/task/functional/SupplierWithContext$Ignored;Ldev/engine_room/flywheel/lib/task/functional/ConsumerWithContext;)Ldev/engine_room/flywheel/lib/task/ForEachPlan;
      // 23: aastore
      // 24: invokestatic dev/engine_room/flywheel/lib/task/NestedPlan.of ([Ldev/engine_room/flywheel/api/task/Plan;)Ldev/engine_room/flywheel/lib/task/NestedPlan;
      // 27: invokevirtual dev/engine_room/flywheel/lib/task/ConditionalPlan$Builder.then (Ldev/engine_room/flywheel/api/task/Plan;)Ldev/engine_room/flywheel/lib/task/ConditionalPlan;
      // 2a: areturn
   }

   public LightUpdatedVisualStorage lightUpdatedVisuals() {
      return this.lightUpdatedVisuals;
   }

   public ShaderLightVisualStorage shaderLightVisuals() {
      return this.shaderLightVisuals;
   }

   public abstract boolean willAccept(T var1);

   public void add(VisualizationContext visualizationContext, T obj, float partialTick) {
      Visual visual = this.visuals.get(obj);
      if (visual == null) {
         visual = this.createRaw(visualizationContext, obj, partialTick);
         if (visual != null) {
            this.setup(visual, partialTick);
            this.visuals.put(obj, visual);
         }
      }
   }

   public void remove(T obj) {
      Visual visual = this.visuals.remove(obj);
      if (visual != null) {
         if (visual instanceof DynamicVisual dynamic) {
            if (visual instanceof SimpleDynamicVisual simpleDynamic) {
               this.simpleDynamicVisuals.remove(simpleDynamic);
            } else {
               this.dynamicVisuals.remove(dynamic);
            }
         }

         if (visual instanceof TickableVisual tickable) {
            if (visual instanceof SimpleTickableVisual simpleTickable) {
               this.simpleTickableVisuals.remove(simpleTickable);
            } else {
               this.tickableVisuals.remove(tickable);
            }
         }

         if (visual instanceof LightUpdatedVisual lightUpdated) {
            this.lightUpdatedVisuals.remove(lightUpdated);
         }

         if (visual instanceof ShaderLightVisual shaderLight) {
            this.shaderLightVisuals.remove(shaderLight);
         }

         visual.delete();
      }
   }

   public void update(T obj, float partialTick) {
      Visual visual = this.visuals.get(obj);
      if (visual != null) {
         visual.update(partialTick);
      }
   }

   public void recreateAll(VisualizationContext visualizationContext, float partialTick) {
      this.dynamicVisuals.clear();
      this.tickableVisuals.clear();
      this.simpleDynamicVisuals.clear();
      this.simpleTickableVisuals.clear();
      this.lightUpdatedVisuals.clear();
      this.shaderLightVisuals.clear();
      this.visuals.replaceAll((obj, visual) -> {
         visual.delete();
         Visual out = this.createRaw(visualizationContext, (T)obj, partialTick);
         if (out != null) {
            this.setup(out, partialTick);
         }

         return out;
      });
   }

   @Nullable
   protected abstract Visual createRaw(VisualizationContext var1, T var2, float var3);

   private void setup(Visual visual, float partialTick) {
      if (visual instanceof DynamicVisual dynamic) {
         if (visual instanceof SimpleDynamicVisual simpleDynamic) {
            this.simpleDynamicVisuals.add(simpleDynamic);
         } else {
            this.dynamicVisuals.add(dynamic, dynamic.planFrame());
         }
      }

      if (visual instanceof TickableVisual tickable) {
         if (visual instanceof SimpleTickableVisual simpleTickable) {
            this.simpleTickableVisuals.add(simpleTickable);
         } else {
            this.tickableVisuals.add(tickable, tickable.planTick());
         }
      }

      if (visual instanceof SectionTrackedVisual tracked) {
         SectionTracker tracker = new SectionTracker();
         tracked.setSectionCollector(tracker);
         if (visual instanceof LightUpdatedVisual lightUpdated) {
            this.lightUpdatedVisuals.add(lightUpdated, tracker);
            lightUpdated.updateLight(partialTick);
         }

         if (visual instanceof ShaderLightVisual shaderLight) {
            this.shaderLightVisuals.add(shaderLight, tracker);
         }
      }
   }

   public void invalidate() {
      this.dynamicVisuals.clear();
      this.tickableVisuals.clear();
      this.simpleDynamicVisuals.clear();
      this.simpleTickableVisuals.clear();
      this.lightUpdatedVisuals.clear();
      this.shaderLightVisuals.clear();
      this.visuals.values().forEach(Visual::delete);
      this.visuals.clear();
   }
}
