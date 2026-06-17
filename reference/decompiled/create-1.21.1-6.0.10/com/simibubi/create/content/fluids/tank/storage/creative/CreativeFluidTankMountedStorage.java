package com.simibubi.create.content.fluids.tank.storage.creative;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.fluid.WrapperMountedFluidStorage;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

public class CreativeFluidTankMountedStorage extends WrapperMountedFluidStorage<CreativeFluidTankBlockEntity.CreativeSmartFluidTank> {
   public static final MapCodec<CreativeFluidTankMountedStorage> CODEC = CreativeFluidTankBlockEntity.CreativeSmartFluidTank.CODEC
      .xmap(CreativeFluidTankMountedStorage::new, storage -> storage.wrapped)
      .fieldOf("value");

   protected CreativeFluidTankMountedStorage(MountedFluidStorageType<?> type, CreativeFluidTankBlockEntity.CreativeSmartFluidTank tank) {
      super(type, tank);
   }

   protected CreativeFluidTankMountedStorage(CreativeFluidTankBlockEntity.CreativeSmartFluidTank tank) {
      this((MountedFluidStorageType<?>)AllMountedStorageTypes.CREATIVE_FLUID_TANK.get(), tank);
   }

   @Override
   public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
   }

   public static CreativeFluidTankMountedStorage fromTank(CreativeFluidTankBlockEntity tank) {
      FluidTank inv = tank.getTankInventory();
      CreativeFluidTankBlockEntity.CreativeSmartFluidTank copy = new CreativeFluidTankBlockEntity.CreativeSmartFluidTank(inv.getCapacity(), $ -> {
      });
      copy.setContainedFluid(inv.getFluid());
      return new CreativeFluidTankMountedStorage(copy);
   }

   public static CreativeFluidTankMountedStorage fromLegacy(Provider registries, CompoundTag nbt) {
      int capacity = nbt.getInt("Capacity");
      FluidStack fluid = FluidStack.parseOptional(registries, nbt.getCompound("ProvidedStack"));
      CreativeFluidTankBlockEntity.CreativeSmartFluidTank tank = new CreativeFluidTankBlockEntity.CreativeSmartFluidTank(capacity, $ -> {
      });
      tank.setContainedFluid(fluid);
      return new CreativeFluidTankMountedStorage(tank);
   }
}
