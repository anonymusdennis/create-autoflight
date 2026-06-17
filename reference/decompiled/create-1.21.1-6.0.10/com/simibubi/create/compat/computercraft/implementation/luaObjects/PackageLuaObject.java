package com.simibubi.create.compat.computercraft.implementation.luaObjects;

import com.simibubi.create.compat.computercraft.implementation.ComputerUtil;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class PackageLuaObject implements LuaComparable {
   public final PackagerBlockEntity blockEntity;
   public final ItemStack box;
   public String address;

   public PackageLuaObject(PackagerBlockEntity blockEntity, ItemStack box) {
      this.blockEntity = blockEntity;
      this.box = box;
      this.address = PackageItem.getAddress(box);
   }

   @LuaFunction(
      mainThread = true
   )
   public final boolean isEditable() {
      return this.blockEntity != null && !this.blockEntity.heldBox.isEmpty() && this.blockEntity.heldBox == this.box;
   }

   @LuaFunction(
      mainThread = true
   )
   public final String getAddress() throws LuaException {
      if (this.isEditable()) {
         this.address = PackageItem.getAddress(this.box);
      }

      return this.address;
   }

   @LuaFunction(
      mainThread = true
   )
   public final void setAddress(String argument) throws LuaException {
      if (!this.isEditable()) {
         throw new LuaException("Package is not editable");
      } else {
         PackageItem.addAddress(this.box, argument);
         this.address = argument;
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public Map<Integer, Map<String, ?>> list() {
      return ComputerUtil.list(PackageItem.getContents(this.box));
   }

   @LuaFunction(
      mainThread = true
   )
   public Map<String, ?> getItemDetail(int slot) throws LuaException {
      return ComputerUtil.getItemDetail(PackageItem.getContents(this.box), slot);
   }

   public boolean hasOrderData() {
      return PackageItem.hasOrderData(this.box);
   }

   @LuaFunction(
      mainThread = true
   )
   public final PackageOrderLuaObject getOrderData() throws LuaException {
      return !this.hasOrderData() ? null : new PackageOrderLuaObject(this);
   }

   public final List<LuaItemStack> getLuaItemStacks() {
      ItemStackHandler results = PackageItem.getContents(this.box);
      List<LuaItemStack> result = new ArrayList<>();

      for (int i = 0; i < results.getSlots(); i++) {
         ItemStack stack = results.getStackInSlot(i);
         if (!stack.isEmpty()) {
            result.add(new LuaItemStack(stack));
         }
      }

      return result;
   }

   @Override
   public Map<?, ?> getTableRepresentation() {
      try {
         Map<String, Object> map = new HashMap<>();
         map.put("address", this.getAddress());
         map.put("contents", this.getLuaItemStacks());
         if (this.hasOrderData()) {
            map.put("orderData", this.getOrderData());
         }

         return map;
      } catch (LuaException var2) {
         return null;
      }
   }
}
