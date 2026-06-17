package dev.simulated_team.simulated.client.sections;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class SimulatedSectionManager {
   private static final Map<ResourceLocation, SimulatedSection> SECTIONS = new HashMap<>();
   private static final Map<SimulatedSection, ResourceLocation> BY_SECTION = new HashMap<>();
   private static List<SimulatedSection> sortedSections = new ArrayList<>();

   public static SimulatedSection getSection(ResourceLocation id) {
      return SECTIONS.get(id);
   }

   public static ResourceLocation getId(SimulatedSection section) {
      return BY_SECTION.get(section);
   }

   public static List<SimulatedSection> getSections() {
      return sortedSections;
   }

   public static class ReloadListener extends SimpleJsonResourceReloadListener {
      private static final Gson GSON = new Gson();

      public ReloadListener() {
         super(GSON, "simulated_sections");
      }

      protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
         SimulatedSectionManager.SECTIONS.clear();
         SimulatedSectionManager.BY_SECTION.clear();

         for (Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            DataResult<SimulatedSection> result = SimulatedSection.CODEC.parse(JsonOps.INSTANCE, entry.getValue());
            if (result.isSuccess()) {
               SimulatedSection tab = (SimulatedSection)result.getOrThrow();
               SimulatedSectionManager.SECTIONS.put(entry.getKey(), tab);
               SimulatedSectionManager.BY_SECTION.put(tab, entry.getKey());
            }
         }

         SimulatedSectionManager.sortedSections = SimulatedSectionManager.SECTIONS.values().stream().sorted().toList();
      }
   }
}
