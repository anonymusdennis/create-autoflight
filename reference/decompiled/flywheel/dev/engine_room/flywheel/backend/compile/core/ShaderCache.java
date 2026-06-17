package dev.engine_room.flywheel.backend.compile.core;

import dev.engine_room.flywheel.backend.gl.GlObject;
import dev.engine_room.flywheel.backend.gl.shader.GlShader;
import dev.engine_room.flywheel.backend.gl.shader.ShaderType;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ShaderCache {
   private final Map<ShaderCache.ShaderKey, ShaderResult> inner = new HashMap<>();

   public GlShader compile(GlslVersion glslVersion, ShaderType shaderType, String name, Consumer<Compilation> callback, List<SourceComponent> sourceComponents) {
      ShaderCache.ShaderKey key = new ShaderCache.ShaderKey(glslVersion, shaderType, name);
      ShaderResult cached = this.inner.get(key);
      if (cached != null) {
         return cached.unwrap();
      } else {
         Compilation ctx = new Compilation();
         ctx.version(glslVersion);
         ctx.define(shaderType.define);
         callback.accept(ctx);
         expand(sourceComponents, ctx::appendComponent);
         ShaderResult out = ctx.compile(shaderType, name);
         this.inner.put(key, out);
         return out.unwrap();
      }
   }

   public void delete() {
      this.inner.values().stream().filter(r -> r instanceof ShaderResult.Success).map(ShaderResult::unwrap).forEach(GlObject::delete);
      this.inner.clear();
   }

   private static void expand(List<SourceComponent> rootSources, Consumer<SourceComponent> out) {
      LinkedHashSet<SourceComponent> included = new LinkedHashSet<>();

      for (SourceComponent component : rootSources) {
         recursiveDepthFirstInclude(included, component);
         included.add(component);
      }

      included.forEach(out);
   }

   private static void recursiveDepthFirstInclude(Set<SourceComponent> included, SourceComponent component) {
      for (SourceComponent include : component.included()) {
         recursiveDepthFirstInclude(included, include);
      }

      included.addAll(component.included());
   }

   private static record ShaderKey(GlslVersion glslVersion, ShaderType shaderType, String name) {
   }
}
