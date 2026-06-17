package com.simibubi.create.foundation.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SimpleCreateTrigger extends CriterionTriggerBase<SimpleCreateTrigger.Instance> {
   public SimpleCreateTrigger(String id) {
      super(id);
   }

   public void trigger(ServerPlayer player) {
      super.trigger(player, null);
   }

   public SimpleCreateTrigger.Instance instance() {
      return new SimpleCreateTrigger.Instance();
   }

   public Codec<SimpleCreateTrigger.Instance> codec() {
      return SimpleCreateTrigger.Instance.CODEC;
   }

   public static class Instance extends CriterionTriggerBase.Instance {
      private static final Codec<SimpleCreateTrigger.Instance> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(SimpleCreateTrigger.Instance::player))
               .apply(instance, SimpleCreateTrigger.Instance::new)
      );
      private final Optional<ContextAwarePredicate> player;

      public Instance() {
         this.player = Optional.empty();
      }

      public Instance(Optional<ContextAwarePredicate> player) {
         this.player = player;
      }

      @Override
      protected boolean test(@Nullable List<Supplier<Object>> suppliers) {
         return true;
      }

      public Optional<ContextAwarePredicate> player() {
         return this.player;
      }
   }
}
