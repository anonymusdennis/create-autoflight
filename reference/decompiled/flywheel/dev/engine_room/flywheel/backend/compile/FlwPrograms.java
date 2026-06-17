package dev.engine_room.flywheel.backend.compile;

import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.SourceFile;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlwPrograms {
   public static final Logger LOGGER = LoggerFactory.getLogger("flywheel/backend/shaders");
   private static final ResourceLocation COMPONENTS_HEADER_FRAG = ResourceUtil.rl("internal/components_header.frag");
   public static ShaderSources SOURCES;

   private FlwPrograms() {
   }

   static void reload(ResourceManager resourceManager) {
      InstancingPrograms.setInstance(null);
      IndirectPrograms.setInstance(null);
      ShaderSources sources = new ShaderSources(resourceManager);
      SOURCES = sources;
      SourceFile fragmentComponentsHeader = sources.get(COMPONENTS_HEADER_FRAG);
      List<SourceComponent> vertexComponents = List.of();
      List<SourceComponent> fragmentComponents = List.of(fragmentComponentsHeader);
      InstancingPrograms.reload(sources, vertexComponents, fragmentComponents);
      IndirectPrograms.reload(sources, vertexComponents, fragmentComponents);
   }
}
