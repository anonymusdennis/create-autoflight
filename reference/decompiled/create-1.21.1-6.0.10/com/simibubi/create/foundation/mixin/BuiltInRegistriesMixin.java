package com.simibubi.create.foundation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({BuiltInRegistries.class})
public class BuiltInRegistriesMixin {
   @WrapOperation(
      method = {"validate"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/Registry;forEach(Ljava/util/function/Consumer;)V"
      )}
   )
   private static <T extends Registry<?>> void create$ourRegistriesAreNotEmpty(Registry<T> instance, Consumer<T> consumer, Operation<Void> original) {
      Consumer<T> callback = t -> {
         if (!t.key().location().getNamespace().equals("create")) {
            consumer.accept(t);
         }
      };
      original.call(new Object[]{instance, callback});
   }

   static {
      CreateBuiltInRegistries.init();
   }
}
