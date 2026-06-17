package com.simibubi.create.content.fluids.tank.storage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.fluid.WrapperMountedFluidStorage;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

public class FluidTankMountedStorage extends WrapperMountedFluidStorage<FluidTankMountedStorage.Handler> implements SyncedMountedStorage {
   public static final MapCodec<FluidTankMountedStorage> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("capacity").forGetter(FluidTankMountedStorage::getCapacity),
               FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(FluidTankMountedStorage::getFluid)
            )
            .apply(i, FluidTankMountedStorage::new)
   );
   private boolean dirty;

   protected FluidTankMountedStorage(MountedFluidStorageType<?> type, int capacity, FluidStack stack) {
      super(type, new FluidTankMountedStorage.Handler(capacity, stack));
      this.wrapped.onChange = () -> this.dirty = true;
   }

   protected FluidTankMountedStorage(int capacity, FluidStack stack) {
      this((MountedFluidStorageType<?>)AllMountedStorageTypes.FLUID_TANK.get(), capacity, stack);
   }

   @Override
   public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
      if (be instanceof FluidTankBlockEntity tank && tank.isController()) {
         FluidTank inventory = tank.getTankInventory();
         inventory.setFluid(this.wrapped.getFluid());
      }
   }

   public FluidStack getFluid() {
      return this.wrapped.getFluid();
   }

   public int getCapacity() {
      return this.wrapped.getCapacity();
   }

   @Override
   public boolean isDirty() {
      return this.dirty;
   }

   @Override
   public void markClean() {
      this.dirty = false;
   }

   @Override
   public void afterSync(Contraption contraption, BlockPos localPos) {
      if (contraption.getBlockEntityClientSide(localPos) instanceof FluidTankBlockEntity tank) {
         FluidTank inv = tank.getTankInventory();
         inv.setFluid(this.getFluid());
         float fillLevel = (float)inv.getFluidAmount() / (float)inv.getCapacity();
         if (tank.getFluidLevel() == null) {
            tank.setFluidLevel(LerpedFloat.linear().startWithValue((double)fillLevel));
         }

         tank.getFluidLevel().chase((double)fillLevel, 0.5, Chaser.EXP);
      }
   }

   public static FluidTankMountedStorage fromTank(FluidTankBlockEntity tank) {
      FluidTank inventory = tank.getTankInventory();
      return new FluidTankMountedStorage(inventory.getCapacity(), inventory.getFluid().copy());
   }

   public static FluidTankMountedStorage fromLegacy(Provider registries, CompoundTag nbt) {
      int capacity = nbt.getInt("Capacity");
      FluidStack fluid = FluidStack.parseOptional(registries, nbt);
      return new FluidTankMountedStorage(capacity, fluid);
   }

   public static final class Handler extends FluidTank {
      private Runnable onChange = () -> {
      };

      public Handler(int capacity, FluidStack stack) {
         super(capacity);
         this.setFluid(stack);
      }

      protected void onContentsChanged() {
         this.onChange.run();
      }
   }
}
