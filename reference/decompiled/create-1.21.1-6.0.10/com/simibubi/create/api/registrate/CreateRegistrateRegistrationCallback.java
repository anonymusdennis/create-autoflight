package com.simibubi.create.api.registrate;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.impl.registrate.CreateRegistrateRegistrationCallbackImpl;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class CreateRegistrateRegistrationCallback {
   public static <R, T extends R> void register(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, NonNullConsumer<? super T> callback) {
      CreateRegistrateRegistrationCallbackImpl.register(registry, id, callback);
   }

   public static void provideRegistrate(CreateRegistrate registrate) {
      CreateRegistrateRegistrationCallbackImpl.provideRegistrate(registrate);
   }

   private CreateRegistrateRegistrationCallback() {
      throw new AssertionError("This class should not be instantiated");
   }
}
