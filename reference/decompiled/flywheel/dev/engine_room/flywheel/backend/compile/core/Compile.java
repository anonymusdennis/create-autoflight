package dev.engine_room.flywheel.backend.compile.core;

import dev.engine_room.flywheel.backend.compile.FlwPrograms;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.backend.gl.shader.GlShader;
import dev.engine_room.flywheel.backend.gl.shader.ShaderType;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.lib.util.StringUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

public class Compile<K> {
   public Compile.ShaderCompiler<K> shader(GlslVersion glslVersion, ShaderType shaderType) {
      return new Compile.ShaderCompiler<>(glslVersion, shaderType);
   }

   public Compile.ProgramStitcher<K> program() {
      return new Compile.ProgramStitcher<>();
   }

   public static class ProgramStitcher<K> implements CompilationHarness.KeyCompiler<K> {
      private final Map<ShaderType, Compile.ShaderCompiler<K>> compilers = new EnumMap<>(ShaderType.class);
      private BiConsumer<K, GlProgram> postLink = (k, p) -> {
      };
      private BiConsumer<K, GlProgram> preLink = (k, p) -> {
      };

      public CompilationHarness<K> harness(String marker, ShaderSources sources) {
         return new CompilationHarness<>(marker, sources, this);
      }

      public Compile.ProgramStitcher<K> link(Compile.ShaderCompiler<K> compilerBuilder) {
         if (this.compilers.containsKey(compilerBuilder.shaderType)) {
            throw new IllegalArgumentException("Duplicate shader type: " + compilerBuilder.shaderType);
         } else {
            this.compilers.put(compilerBuilder.shaderType, compilerBuilder);
            return this;
         }
      }

      public Compile.ProgramStitcher<K> postLink(BiConsumer<K, GlProgram> postLink) {
         this.postLink = postLink;
         return this;
      }

      public Compile.ProgramStitcher<K> preLink(BiConsumer<K, GlProgram> preLink) {
         this.preLink = preLink;
         return this;
      }

      @Override
      public GlProgram compile(K key, ShaderSources loader, ShaderCache shaderCache, ProgramLinker programLinker) {
         if (this.compilers.isEmpty()) {
            throw new IllegalStateException("No shader compilers were added!");
         } else {
            long start = System.nanoTime();
            List<GlShader> shaders = new ArrayList<>();

            for (Compile.ShaderCompiler<K> compiler : this.compilers.values()) {
               shaders.add(compiler.compile(key, shaderCache, loader));
            }

            GlProgram out = programLinker.link(shaders, p -> this.preLink.accept(key, p));
            this.postLink.accept(key, out);
            long end = System.nanoTime();
            FlwPrograms.LOGGER.debug("Linked {} in {}", key, StringUtil.formatTime(end - start));
            return out;
         }
      }
   }

   public static class ShaderCompiler<K> {
      private final GlslVersion glslVersion;
      private final ShaderType shaderType;
      private final List<BiFunction<K, ShaderSources, SourceComponent>> fetchers = new ArrayList<>();
      private BiConsumer<K, Compilation> compilationCallbacks = ($, $$) -> {
      };
      private Function<K, String> nameMapper = Object::toString;

      public ShaderCompiler(GlslVersion glslVersion, ShaderType shaderType) {
         this.glslVersion = glslVersion;
         this.shaderType = shaderType;
      }

      public Compile.ShaderCompiler<K> nameMapper(Function<K, String> nameMapper) {
         this.nameMapper = nameMapper;
         return this;
      }

      public Compile.ShaderCompiler<K> with(BiFunction<K, ShaderSources, SourceComponent> fetch) {
         this.fetchers.add(fetch);
         return this;
      }

      public Compile.ShaderCompiler<K> withComponents(Collection<SourceComponent> components) {
         components.forEach(this::withComponent);
         return this;
      }

      public Compile.ShaderCompiler<K> withComponent(SourceComponent component) {
         return this.withComponent($ -> component);
      }

      public Compile.ShaderCompiler<K> withComponent(Function<K, SourceComponent> sourceFetcher) {
         return this.with((key, $) -> sourceFetcher.apply(key));
      }

      public Compile.ShaderCompiler<K> withResource(Function<K, ResourceLocation> sourceFetcher) {
         return this.with((key, loader) -> loader.get(sourceFetcher.apply(key)));
      }

      public Compile.ShaderCompiler<K> withResource(ResourceLocation resourceLocation) {
         return this.withResource((Function<K, ResourceLocation>)($ -> resourceLocation));
      }

      public Compile.ShaderCompiler<K> onCompile(BiConsumer<K, Compilation> cb) {
         this.compilationCallbacks = this.compilationCallbacks.andThen(cb);
         return this;
      }

      public Compile.ShaderCompiler<K> define(String def, int value) {
         return this.onCompile(($, ctx) -> ctx.define(def, String.valueOf(value)));
      }

      public Compile.ShaderCompiler<K> enableExtension(String extension) {
         return this.onCompile(($, ctx) -> ctx.enableExtension(extension));
      }

      public Compile.ShaderCompiler<K> enableExtensions(String... extensions) {
         return this.onCompile(($, ctx) -> {
            for (String extension : extensions) {
               ctx.enableExtension(extension);
            }
         });
      }

      public Compile.ShaderCompiler<K> enableExtensions(Collection<String> extensions) {
         return this.onCompile(($, ctx) -> {
            for (String extension : extensions) {
               ctx.enableExtension(extension);
            }
         });
      }

      public Compile.ShaderCompiler<K> requireExtensions(Collection<String> extensions) {
         return this.onCompile(($, ctx) -> {
            for (String extension : extensions) {
               ctx.requireExtension(extension);
            }
         });
      }

      private GlShader compile(K key, ShaderCache compiler, ShaderSources loader) {
         long start = System.nanoTime();
         ArrayList<SourceComponent> components = new ArrayList<>();

         for (BiFunction<K, ShaderSources, SourceComponent> fetcher : this.fetchers) {
            components.add(fetcher.apply(key, loader));
         }

         Consumer<Compilation> cb = ctx -> this.compilationCallbacks.accept(key, ctx);
         String name = this.nameMapper.apply(key);
         GlShader out = compiler.compile(this.glslVersion, this.shaderType, name, cb, components);
         long end = System.nanoTime();
         FlwPrograms.LOGGER.debug("Compiled {} in {}", name, StringUtil.formatTime(end - start));
         return out;
      }
   }
}
