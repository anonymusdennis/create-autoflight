package dev.ryanhcode.sable.physics.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.physics.floating_block.FloatingBlockMaterial;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class FloatingBlockMaterialDataHandler {
   public static HashMap<ResourceLocation, FloatingBlockMaterial> allMaterials = new HashMap<>();

   public static void addMaterial(ResourceLocation id, FloatingBlockMaterial material) {
      allMaterials.put(id, material);
   }

   public static void clearMaterials() {
      allMaterials.clear();
   }

   public static class ReloadListener extends SimpleJsonResourceReloadListener {
      public static final String NAME = "floating_block_material";
      public static final ResourceLocation ID = Sable.sablePath("floating_block_material");
      private static final Gson GSON = new Gson();
      public static final FloatingBlockMaterialDataHandler.ReloadListener INSTANCE = new FloatingBlockMaterialDataHandler.ReloadListener();

      protected ReloadListener() {
         super(GSON, "floating_materials");
      }

      protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
         FloatingBlockMaterialDataHandler.allMaterials.clear();

         for (Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            JsonElement element = entry.getValue();

            try {
               DataResult<FloatingBlockMaterial> dataResult = FloatingBlockMaterial.CODEC.parse(JsonOps.INSTANCE, element);
               if (dataResult.error().isPresent()) {
                  Sable.LOGGER.error(String.valueOf(dataResult.error().get()));
               } else {
                  ResourceLocation loc = entry.getKey();
                  FloatingBlockMaterial floatingBlockMaterial = (FloatingBlockMaterial)dataResult.result().orElseThrow();
                  FloatingBlockMaterialDataHandler.addMaterial(loc, floatingBlockMaterial);
               }
            } catch (Exception var10) {
            }
         }
      }
   }
}
