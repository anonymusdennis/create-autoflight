package dev.simulated_team.simulated.neoforge.service;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.inventory.neoforge.InventoryLoaderWrapperImpl;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import dev.simulated_team.simulated.service.SimInventoryService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class NeoForgeSimInventoryService implements SimInventoryService {
   public static Set<NeoForgeSimInventoryService.InventoryGetterHolder<? extends BlockEntity>> inventoryGetters = new HashSet<>();
   public static Set<NeoForgeSimInventoryService.TankGetterHolder<? extends BlockEntity>> fluidTankGetters = new HashSet<>();
   public static Set<NeoForgeSimInventoryService.EnergyGetterHolder<? extends BlockEntity>> energyGetters = new HashSet<>();
   public static HashMap<BlockEntityType<BlockEntity>, Function<BlockEntity, SingleTank>> tankGetters = new HashMap<>();

   @Override
   public <T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerInventory(BiFunction<T, Direction, AbstractContainer> getter) {
      return type -> inventoryGetters.add(new NeoForgeSimInventoryService.InventoryGetterHolder<>(getter, type));
   }

   @Override
   public <T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerTank(BiFunction<T, Direction, SingleTank> getter) {
      return type -> fluidTankGetters.add(new NeoForgeSimInventoryService.TankGetterHolder<>(getter, type));
   }

   @Override
   public <T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerBattery(BiFunction<T, Direction, SingleBattery> getter) {
      return type -> energyGetters.add(new NeoForgeSimInventoryService.EnergyGetterHolder<>(getter, type));
   }

   @Override
   public <T extends InventoryLoaderWrapper> T getInventory(@Nullable BlockEntity be, @Nullable Direction dir) {
      if (be != null) {
         IItemHandler handler = (IItemHandler)be.getLevel().getCapability(ItemHandler.BLOCK, be.getBlockPos(), dir);
         if (handler != null) {
            return (T)(new InventoryLoaderWrapperImpl(handler));
         }
      }

      return null;
   }

   @Override
   public <T extends InventoryLoaderWrapper> T getWrappedAllItemsFromContraption(MountedStorageManager manager) {
      return (T)(new InventoryLoaderWrapperImpl(manager.getAllItems()));
   }

   @Override
   public <T extends InventoryLoaderWrapper> T getWrappedMountedItemsFromContraption(MountedStorageManager manager) {
      return (T)(new InventoryLoaderWrapperImpl(manager.getMountedItems()));
   }

   public static record EnergyGetterHolder<T extends BlockEntity>(BiFunction<T, Direction, SingleBattery> getter, BlockEntityType<T> type) {
      public SingleBattery castBlockEntityAndGetInv(BlockEntity be, Direction dir) {
         return this.getter.apply((T)be, dir);
      }
   }

   public static record InventoryGetterHolder<T extends BlockEntity>(BiFunction<T, Direction, AbstractContainer> getter, BlockEntityType<T> type) {
      public AbstractContainer castBlockEntityAndGetInv(BlockEntity be, Direction dir) {
         return this.getter.apply((T)be, dir);
      }
   }

   public static record TankGetterHolder<T extends BlockEntity>(BiFunction<T, Direction, SingleTank> getter, BlockEntityType<T> type) {
      public SingleTank castBlockEntityAndGetInv(BlockEntity be, Direction dir) {
         return this.getter.apply((T)be, dir);
      }
   }
}
