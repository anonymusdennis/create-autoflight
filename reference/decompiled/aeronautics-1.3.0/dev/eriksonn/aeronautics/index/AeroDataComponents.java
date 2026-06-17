package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.content.components.Converter;
import dev.eriksonn.aeronautics.content.components.Levitating;
import foundry.veil.platform.registry.RegistrationProvider;
import java.util.function.UnaryOperator;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponentType.Builder;
import net.minecraft.core.registries.Registries;

public class AeroDataComponents {
   private static final RegistrationProvider<DataComponentType<?>> REGISTRY = RegistrationProvider.get(Registries.DATA_COMPONENT_TYPE, "aeronautics");
   public static final DataComponentType<Levitating> LEVITATING = create("levitating", builder -> builder.persistent(Levitating.CODEC));
   public static final DataComponentType<Converter> CONVERTER = create("converter", builder -> builder.persistent(Converter.CODEC));

   private static <T> DataComponentType<T> create(String name, UnaryOperator<Builder<T>> builder) {
      DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
      REGISTRY.register(name, () -> type);
      return type;
   }

   public static void init() {
   }
}
