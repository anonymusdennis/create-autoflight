package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.compat.computercraft.events.ComputerEvent;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import com.simibubi.create.compat.computercraft.implementation.ComputerUtil;
import com.simibubi.create.compat.computercraft.implementation.luaObjects.PackageLuaObject;
import com.simibubi.create.content.logistics.packagePort.postbox.PostboxBlockEntity;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class PostboxPeripheral extends SyncedPeripheral<PostboxBlockEntity> {
   public PostboxPeripheral(PostboxBlockEntity blockEntity) {
      super(blockEntity);
   }

   @LuaFunction(
      mainThread = true
   )
   public final void setAddress(String address) throws LuaException {
      this.blockEntity.addressFilter = address;
      this.blockEntity.filterChanged();
      this.blockEntity.notifyUpdate();
   }

   @LuaFunction(
      mainThread = true
   )
   public final String getAddress() throws LuaException {
      return this.blockEntity.addressFilter;
   }

   @LuaFunction(
      mainThread = true
   )
   public Map<Integer, Map<String, ?>> list() {
      return ComputerUtil.list(this.blockEntity.inventory);
   }

   @LuaFunction(
      mainThread = true
   )
   public Map<String, ?> getItemDetail(int slot) throws LuaException {
      return ComputerUtil.getItemDetail(this.blockEntity.inventory, slot);
   }

   @LuaFunction(
      mainThread = true
   )
   public final String getConfiguration() throws LuaException {
      if (this.blockEntity.target == null) {
         return null;
      } else {
         return this.blockEntity.acceptsPackages ? "send_recieve" : "send";
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public final boolean setConfiguration(String config) throws LuaException {
      if (this.blockEntity.target == null) {
         return false;
      } else if (config.equals("send_recieve")) {
         this.blockEntity.acceptsPackages = true;
         this.blockEntity.filterChanged();
         this.blockEntity.notifyUpdate();
         return true;
      } else if (config.equals("send")) {
         this.blockEntity.acceptsPackages = false;
         this.blockEntity.filterChanged();
         this.blockEntity.notifyUpdate();
         return true;
      } else {
         throw new LuaException("Unknown configuration: \"" + config + "\" Possible configurations are: \"send_recieve\" and \"send\".");
      }
   }

   @Override
   public void prepareComputerEvent(@NotNull ComputerEvent event) {
      if (event instanceof PackageEvent pe) {
         this.queueEvent(pe.status, new Object[]{new PackageLuaObject(null, pe.box)});
      }
   }

   @NotNull
   public String getType() {
      return "Create_Postbox";
   }
}
