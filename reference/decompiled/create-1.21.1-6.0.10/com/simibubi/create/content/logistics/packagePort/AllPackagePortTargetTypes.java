package com.simibubi.create.content.logistics.packagePort;

import com.simibubi.create.api.registry.CreateRegistries;
import net.minecraft.core.Holder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus.Internal;

public class AllPackagePortTargetTypes {
   private static final DeferredRegister<PackagePortTargetType> REGISTER = DeferredRegister.create(CreateRegistries.PACKAGE_PORT_TARGET_TYPE, "create");
   public static final Holder<PackagePortTargetType> CHAIN_CONVEYOR = REGISTER.register(
      "chain_conveyor", PackagePortTarget.ChainConveyorFrogportTarget.Type::new
   );
   public static final Holder<PackagePortTargetType> TRAIN_STATION = REGISTER.register("train_station", PackagePortTarget.TrainStationFrogportTarget.Type::new);

   @Internal
   public static void register(IEventBus eventBus) {
      REGISTER.register(eventBus);
   }
}
