package com.simibubi.create.impl.registry;

import com.simibubi.create.api.equipment.potatoCannon.PotatoCannonProjectileType;
import com.simibubi.create.api.registry.CreateRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent.NewRegistry;

@EventBusSubscriber
public class CreateRegistriesImpl {
   @SubscribeEvent
   public static void registerDatapackRegistries(NewRegistry event) {
      event.dataPackRegistry(CreateRegistries.POTATO_PROJECTILE_TYPE, PotatoCannonProjectileType.CODEC, PotatoCannonProjectileType.CODEC);
   }
}
