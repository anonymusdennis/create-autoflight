package dev.simulated_team.simulated.data.advancements;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.CriterionValidator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleSimulatedTrigger extends SimulatedCriterionTriggerBase<SimulatedCriterionTriggerBase.Instance> {
   public SimpleSimulatedTrigger(ResourceLocation id) {
      super(id);
   }

   public void trigger(ServerPlayer player) {
      super.trigger(player, null);
   }

   public SimpleSimulatedTrigger.Instance instance() {
      return new SimpleSimulatedTrigger.Instance(this.getId());
   }

   @NotNull
   public Codec<SimulatedCriterionTriggerBase.Instance> codec() {
      return ResourceLocation.CODEC.xmap(SimpleSimulatedTrigger.Instance::new, SimulatedCriterionTriggerBase.Instance::getId);
   }

   public static class Instance extends SimulatedCriterionTriggerBase.Instance {
      public Instance(ResourceLocation id) {
         super(id);
      }

      @Override
      protected boolean test(@Nullable List<Supplier<Object>> suppliers) {
         return true;
      }

      public void validate(@NotNull CriterionValidator criterionValidator) {
      }
   }
}
