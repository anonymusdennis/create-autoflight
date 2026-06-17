package dev.eriksonn.aeronautics.data;

import dev.simulated_team.simulated.data.advancements.SimpleSimulatedTrigger;
import dev.simulated_team.simulated.data.advancements.SimulatedCriterionTriggerBase;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class AeroAdvancementTriggers {
   private static final List<SimulatedCriterionTriggerBase<?>> TRIGGERS = new LinkedList<>();

   public static SimpleSimulatedTrigger addSimple(String modid, String id) {
      return add(new SimpleSimulatedTrigger(ResourceLocation.fromNamespaceAndPath(modid, id)));
   }

   private static <T extends SimulatedCriterionTriggerBase<?>> T add(T instance) {
      TRIGGERS.add(instance);
      return instance;
   }

   public static void register() {
      TRIGGERS.forEach(trigger -> Registry.register(BuiltInRegistries.TRIGGER_TYPES, trigger.getId(), trigger));
   }
}
