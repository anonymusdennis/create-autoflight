package dev.simulated_team.simulated.service.compat;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public interface SimPeripheralService {
   <T extends BlockEntity> void addPeripheral(Supplier<BlockEntityType<T>> var1, SimPeripheralService.CapabilityGetter<T, IPeripheral> var2);

   default <T extends BlockEntity> void addPeripheral(
      Supplier<BlockEntityType<T>> typeSupplier, SimPeripheralService.SimpleCapabilityGetter<T, IPeripheral> getter
   ) {
      this.addPeripheral(typeSupplier, getter);
   }

   <T extends BlockEntity> void addWired(Supplier<BlockEntityType<T>> var1, SimPeripheralService.CapabilityGetter<T, WiredElement> var2);

   default <T extends BlockEntity> void addWired(Supplier<BlockEntityType<T>> typeSupplier, SimPeripheralService.SimpleCapabilityGetter<T, WiredElement> getter) {
      this.addWired(typeSupplier, getter);
   }

   @FunctionalInterface
   public interface CapabilityGetter<T extends BlockEntity, V> {
      @Nullable
      V get(T var1, Direction var2);
   }

   @FunctionalInterface
   public interface SimpleCapabilityGetter<T extends BlockEntity, V> extends SimPeripheralService.CapabilityGetter<T, V> {
      @Nullable
      V get(T var1);

      @Override
      default V get(T blockEntity, Direction direction) {
         return this.get(blockEntity);
      }
   }
}
