package dev.engine_room.flywheel.backend.compile.core;

import dev.engine_room.flywheel.backend.gl.GlObject;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import java.util.HashMap;
import java.util.Map;

public class CompilationHarness<K> {
   private final ShaderSources sources;
   private final CompilationHarness.KeyCompiler<K> compiler;
   private final ShaderCache shaderCache;
   private final ProgramLinker programLinker;
   private final Map<K, GlProgram> programs = new HashMap<>();

   public CompilationHarness(String marker, ShaderSources sources, CompilationHarness.KeyCompiler<K> compiler) {
      this.sources = sources;
      this.compiler = compiler;
      this.shaderCache = new ShaderCache();
      this.programLinker = new ProgramLinker();
   }

   public GlProgram get(K key) {
      return this.programs.computeIfAbsent(key, this::compile);
   }

   private GlProgram compile(K key) {
      return this.compiler.compile(key, this.sources, this.shaderCache, this.programLinker);
   }

   public void delete() {
      this.shaderCache.delete();
      this.programs.values().forEach(GlObject::delete);
      this.programs.clear();
   }

   public interface KeyCompiler<K> {
      GlProgram compile(K var1, ShaderSources var2, ShaderCache var3, ProgramLinker var4);
   }
}
