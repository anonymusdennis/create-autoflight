package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.compat.computercraft.events.ComputerEvent;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import com.simibubi.create.compat.computercraft.implementation.ComputerUtil;
import com.simibubi.create.compat.computercraft.implementation.luaObjects.PackageLuaObject;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PackagerPeripheral extends SyncedPeripheral<PackagerBlockEntity> {
   public PackagerPeripheral(PackagerBlockEntity blockEntity) {
      super(blockEntity);
   }

   @Override
   public void attach(@NotNull IComputerAccess computer) {
      super.attach(computer);
      this.blockEntity.hasCustomComputerAddress = false;
   }

   @Override
   public void detach(@NotNull IComputerAccess computer) {
      super.detach(computer);
      this.blockEntity.hasCustomComputerAddress = false;
   }

   @LuaFunction(
      mainThread = true
   )
   public final boolean makePackage() {
      if (!this.blockEntity.heldBox.isEmpty()) {
         return false;
      } else {
         this.blockEntity.activate();
         return !this.blockEntity.heldBox.isEmpty();
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public Map<Integer, Map<String, ?>> list() {
      return ComputerUtil.list(this.blockEntity.targetInventory.getInventory());
   }

   @LuaFunction(
      mainThread = true
   )
   public Map<String, ?> getItemDetail(int slot) throws LuaException {
      return ComputerUtil.getItemDetail(this.blockEntity.targetInventory.getInventory(), slot);
   }

   @LuaFunction(
      mainThread = true
   )
   public final String getAddress() {
      this.blockEntity.updateSignAddress();
      return this.blockEntity.signBasedAddress;
   }

   @LuaFunction(
      mainThread = true
   )
   public final void setAddress(Optional<String> argument) {
      if (argument.isPresent()) {
         this.blockEntity.customComputerAddress = argument.get();
         this.blockEntity.signBasedAddress = argument.get();
         this.blockEntity.hasCustomComputerAddress = true;
      } else {
         this.blockEntity.customComputerAddress = "";
         this.blockEntity.hasCustomComputerAddress = false;
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public final PackageLuaObject getPackage() {
      ItemStack box = this.blockEntity.heldBox;
      return box.isEmpty() ? null : new PackageLuaObject(this.blockEntity, box);
   }

   @Override
   public void prepareComputerEvent(@NotNull ComputerEvent event) {
      if (event instanceof PackageEvent pe) {
         this.queueEvent(pe.status, new Object[]{new PackageLuaObject(this.blockEntity, pe.box)});
      }
   }

   @NotNull
   public String getType() {
      return "Create_Packager";
   }
}
