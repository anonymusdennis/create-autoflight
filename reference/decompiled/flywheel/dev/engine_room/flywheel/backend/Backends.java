package dev.engine_room.flywheel.backend;

import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.backend.compile.IndirectPrograms;
import dev.engine_room.flywheel.backend.compile.InstancingPrograms;
import dev.engine_room.flywheel.backend.engine.EngineImpl;
import dev.engine_room.flywheel.backend.engine.indirect.IndirectDrawManager;
import dev.engine_room.flywheel.backend.engine.instancing.InstancedDrawManager;
import dev.engine_room.flywheel.backend.gl.Driver;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.lib.backend.SimpleBackend;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import dev.engine_room.flywheel.lib.util.ShadersModHelper;

public final class Backends {
   public static final Backend INSTANCING = SimpleBackend.builder()
      .engineFactory(level -> new EngineImpl(level, new InstancedDrawManager(InstancingPrograms.get()), 256))
      .priority(500)
      .supported(() -> GlCompat.SUPPORTS_INSTANCING && InstancingPrograms.allLoaded() && !ShadersModHelper.isShaderPackInUse())
      .register(ResourceUtil.rl("instancing"));
   public static final Backend INDIRECT = SimpleBackend.builder()
      .engineFactory(level -> new EngineImpl(level, new IndirectDrawManager(IndirectPrograms.get()), 256))
      .priority(() -> GlCompat.DRIVER == Driver.INTEL ? 1 : 1000)
      .supported(() -> GlCompat.SUPPORTS_INDIRECT && IndirectPrograms.allLoaded() && !ShadersModHelper.isShaderPackInUse())
      .register(ResourceUtil.rl("indirect"));

   private Backends() {
   }

   public static void init() {
   }
}
