package com.simibubi.create.foundation.render;

import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.LightShader;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.ModelUtil;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;

public class SpecialModels {
   private static final RendererReloadCache<SpecialModels.Key, Model> FLAT = new RendererReloadCache(
      it -> new BakedModelBuilder(it.partial.get())
            .materialFunc(
               (renderType, shaded, ao) -> {
                  Material material = ModelUtil.getMaterial(renderType, shaded, ao);
                  return material == null
                     ? null
                     : SimpleMaterial.builderOf(material)
                        .light(it.light)
                        .cardinalLightingMode(shaded ? it.cardinalLightingMode : CardinalLightingMode.OFF)
                        .build();
               }
            )
            .build()
   );

   public static Model flatLit(PartialModel partial) {
      return (Model)FLAT.get(new SpecialModels.Key(partial, LightShaders.FLAT, CardinalLightingMode.ENTITY));
   }

   public static Model smoothLit(PartialModel partial) {
      return (Model)FLAT.get(new SpecialModels.Key(partial, LightShaders.SMOOTH, CardinalLightingMode.ENTITY));
   }

   public static Model flatChunk(PartialModel partial) {
      return (Model)FLAT.get(new SpecialModels.Key(partial, LightShaders.FLAT, CardinalLightingMode.CHUNK));
   }

   public static Model chunkDiffuse(PartialModel partial) {
      return (Model)FLAT.get(new SpecialModels.Key(partial, LightShaders.SMOOTH_WHEN_EMBEDDED, CardinalLightingMode.CHUNK));
   }

   private static record Key(PartialModel partial, LightShader light, CardinalLightingMode cardinalLightingMode) {
   }
}
