package dev.engine_room.flywheel.backend.compile.component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.SourceFile;
import dev.engine_room.flywheel.backend.glsl.generate.FnSignature;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBlock;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;
import dev.engine_room.flywheel.backend.glsl.generate.GlslExpr;
import dev.engine_room.flywheel.backend.glsl.generate.GlslSwitch;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class UberShaderComponent implements SourceComponent {
   private final ResourceLocation name;
   private final GlslExpr switchArg;
   private final List<UberShaderComponent.AdaptedFn> functionsToAdapt;
   private final List<StringSubstitutionComponent> adaptedComponents;

   private UberShaderComponent(
      ResourceLocation name, GlslExpr switchArg, List<UberShaderComponent.AdaptedFn> functionsToAdapt, List<StringSubstitutionComponent> adaptedComponents
   ) {
      this.name = name;
      this.switchArg = switchArg;
      this.functionsToAdapt = functionsToAdapt;
      this.adaptedComponents = adaptedComponents;
   }

   public static UberShaderComponent.Builder builder(ResourceLocation name) {
      return new UberShaderComponent.Builder(name);
   }

   @Override
   public String name() {
      return ResourceUtil.rl("uber_shader").toString() + " / " + this.name;
   }

   @Override
   public Collection<? extends SourceComponent> included() {
      return this.adaptedComponents;
   }

   @Override
   public String source() {
      GlslBuilder builder = new GlslBuilder();

      for (UberShaderComponent.AdaptedFn adaptedFunction : this.functionsToAdapt) {
         builder.function().signature(adaptedFunction.signature()).body(body -> this.generateAdapter(body, adaptedFunction));
         builder.blankLine();
      }

      return builder.build();
   }

   private void generateAdapter(GlslBlock body, UberShaderComponent.AdaptedFn adaptedFunction) {
      GlslSwitch sw = GlslSwitch.on(this.switchArg);
      FnSignature fnSignature = adaptedFunction.signature();
      String fnName = fnSignature.name();
      boolean isVoid = fnSignature.isVoid();
      Collection<? extends GlslExpr> fnArgs = fnSignature.createArgExpressions();

      for (int i = 0; i < this.adaptedComponents.size(); i++) {
         StringSubstitutionComponent component = this.adaptedComponents.get(i);
         if (component.replaces(fnName)) {
            GlslExpr.FunctionCall adaptedCall = GlslExpr.call(component.remapFnName(fnName), fnArgs);
            GlslBlock block = GlslBlock.create();
            if (isVoid) {
               block.eval(adaptedCall).breakStmt();
            } else {
               block.ret(adaptedCall);
            }

            sw.uintCase(i, block);
         }
      }

      if (!isVoid) {
         GlslExpr defaultReturn = adaptedFunction.defaultReturn;
         if (defaultReturn == null) {
            throw new IllegalStateException("Function " + fnName + " is not void, but no default return value was provided");
         }

         sw.defaultCase(GlslBlock.create().ret(defaultReturn));
      }

      body.add(sw);
   }

   private static record AdaptedFn(FnSignature signature, @Nullable GlslExpr defaultReturn) {
   }

   public static class Builder {
      private final ResourceLocation name;
      private final List<ResourceLocation> materialSources = new ArrayList<>();
      private final List<UberShaderComponent.AdaptedFn> adaptedFunctions = new ArrayList<>();
      @Nullable
      private GlslExpr switchArg;

      public Builder(ResourceLocation name) {
         this.name = name;
      }

      public UberShaderComponent.Builder materialSources(List<ResourceLocation> sources) {
         this.materialSources.addAll(sources);
         return this;
      }

      public UberShaderComponent.Builder adapt(FnSignature function) {
         this.adaptedFunctions.add(new UberShaderComponent.AdaptedFn(function, null));
         return this;
      }

      public UberShaderComponent.Builder adapt(FnSignature function, GlslExpr defaultReturn) {
         this.adaptedFunctions.add(new UberShaderComponent.AdaptedFn(function, defaultReturn));
         return this;
      }

      public UberShaderComponent.Builder switchOn(GlslExpr expr) {
         this.switchArg = expr;
         return this;
      }

      public UberShaderComponent build(ShaderSources sources) {
         if (this.switchArg == null) {
            throw new NullPointerException("Switch argument must be set");
         } else {
            com.google.common.collect.ImmutableList.Builder<StringSubstitutionComponent> transformed = ImmutableList.builder();
            int index = 0;

            for (ResourceLocation rl : this.materialSources) {
               SourceFile sourceFile = sources.get(rl);
               int finalIndex = index;
               ImmutableMap<String, String> adapterMap = createAdapterMap(this.adaptedFunctions, fnName -> "_" + fnName + "_" + finalIndex);
               transformed.add(new StringSubstitutionComponent(sourceFile, adapterMap));
               index++;
            }

            return new UberShaderComponent(this.name, this.switchArg, this.adaptedFunctions, transformed.build());
         }
      }

      private static ImmutableMap<String, String> createAdapterMap(List<UberShaderComponent.AdaptedFn> adaptedFunctions, UnaryOperator<String> nameAdapter) {
         com.google.common.collect.ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

         for (UberShaderComponent.AdaptedFn adapted : adaptedFunctions) {
            String fnName = adapted.signature().name();
            builder.put(fnName, nameAdapter.apply(fnName));
         }

         return builder.build();
      }
   }
}
