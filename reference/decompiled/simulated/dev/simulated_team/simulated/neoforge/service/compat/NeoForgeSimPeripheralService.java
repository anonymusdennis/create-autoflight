package dev.simulated_team.simulated.neoforge.service.compat;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredElementCapability;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class NeoForgeSimPeripheralService implements SimPeripheralService {
   private static final List<NeoForgeSimPeripheralService.Entry<BlockEntity, IPeripheral>> PERIPHERALS = new ArrayList<>();
   private static final List<NeoForgeSimPeripheralService.Entry<BlockEntity, WiredElement>> WIRED_ELEMENTS = new ArrayList<>();

   @Override
   public <T extends BlockEntity> void addPeripheral(Supplier<BlockEntityType<T>> typeSupplier, SimPeripheralService.CapabilityGetter<T, IPeripheral> getter) {
      PERIPHERALS.add(new NeoForgeSimPeripheralService.Entry<>(typeSupplier, getter));
   }

   @Override
   public <T extends BlockEntity> void addWired(Supplier<BlockEntityType<T>> typeSupplier, SimPeripheralService.CapabilityGetter<T, WiredElement> getter) {
      WIRED_ELEMENTS.add(new NeoForgeSimPeripheralService.Entry<>(typeSupplier, getter));
   }

   @SubscribeEvent
   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      for (NeoForgeSimPeripheralService.Entry<BlockEntity, IPeripheral> entry : PERIPHERALS) {
         event.registerBlockEntity(PeripheralCapability.get(), entry.typeSupplier.get(), (be, direction) -> entry.peripheralFunction().get(be, direction));
      }

      for (NeoForgeSimPeripheralService.Entry<BlockEntity, WiredElement> entry : WIRED_ELEMENTS) {
         event.registerBlockEntity(WiredElementCapability.get(), entry.typeSupplier.get(), (be, direction) -> entry.peripheralFunction().get(be, direction));
      }
   }

   private static record Entry<T extends BlockEntity, V>(
      Supplier<BlockEntityType<T>> typeSupplier, SimPeripheralService.CapabilityGetter<T, V> peripheralFunction
   ) {
   }
}
