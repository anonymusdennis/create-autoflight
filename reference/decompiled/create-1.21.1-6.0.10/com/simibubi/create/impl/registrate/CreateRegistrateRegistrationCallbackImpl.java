package com.simibubi.create.impl.registrate;

import com.mojang.datafixers.util.Either;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class CreateRegistrateRegistrationCallbackImpl {
   private static final Map<String, Either<List<CreateRegistrateRegistrationCallbackImpl.CallbackImpl<?, ?>>, CreateRegistrate>> CALLBACKS = new HashMap<>();

   public static void provideRegistrate(CreateRegistrate registrate) {
      synchronized (CALLBACKS) {
         String modid = registrate.getModid();
         Either<List<CreateRegistrateRegistrationCallbackImpl.CallbackImpl<?, ?>>, CreateRegistrate> either = CALLBACKS.remove(modid);
         if (either != null) {
            Optional<List<CreateRegistrateRegistrationCallbackImpl.CallbackImpl<?, ?>>> optionalCallbacks = either.left();
            if (optionalCallbacks.isEmpty()) {
               throw new IllegalArgumentException("Tried to register a duplicate CreateRegistrate instance for mod ID: " + modid);
            }

            for (CreateRegistrateRegistrationCallbackImpl.CallbackImpl<?, ?> callback : optionalCallbacks.get()) {
               callback.addToRegistrate(registrate);
            }
         }

         CALLBACKS.put(modid, Either.right(registrate));
      }
   }

   public static <R, T extends R> void register(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, NonNullConsumer<? super T> callback) {
      CreateRegistrateRegistrationCallbackImpl.CallbackImpl<R, T> callbackImpl = new CreateRegistrateRegistrationCallbackImpl.CallbackImpl<>(
         registry, id, callback
      );
      Either<List<CreateRegistrateRegistrationCallbackImpl.CallbackImpl<?, ?>>, CreateRegistrate> either;
      synchronized (CALLBACKS) {
         either = CALLBACKS.computeIfAbsent(id.getNamespace(), k -> Either.left(new ArrayList()));
         either.ifLeft(callbacks -> callbacks.add(callbackImpl));
      }

      either.ifRight(callbackImpl::addToRegistrate);
   }

   private static record CallbackImpl<R, T extends R>(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, NonNullConsumer<? super T> callback) {
      public void addToRegistrate(CreateRegistrate registrate) {
         registrate.addRegisterCallback(this.id.getPath(), this.registry, this.callback);
      }
   }
}
