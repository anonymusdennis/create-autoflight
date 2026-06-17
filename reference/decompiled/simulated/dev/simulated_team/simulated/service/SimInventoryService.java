package dev.simulated_team.simulated.service;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import java.util.function.BiFunction;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public interface SimInventoryService {
   SimInventoryService INSTANCE = ServiceUtil.load(SimInventoryService.class);

   <T extends InventoryLoaderWrapper> T getInventory(@Nullable BlockEntity var1, @Nullable Direction var2);

   <T extends InventoryLoaderWrapper> T getWrappedAllItemsFromContraption(MountedStorageManager var1);

   <T extends InventoryLoaderWrapper> T getWrappedMountedItemsFromContraption(MountedStorageManager var1);

   <T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerInventory(BiFunction<T, Direction, AbstractContainer> var1);

   <T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerTank(BiFunction<T, Direction, SingleTank> var1);

   <T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerBattery(BiFunction<T, Direction, SingleBattery> var1);
}
