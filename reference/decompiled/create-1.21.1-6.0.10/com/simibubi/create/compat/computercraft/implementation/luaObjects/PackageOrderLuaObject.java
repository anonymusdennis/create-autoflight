package com.simibubi.create.compat.computercraft.implementation.luaObjects;

import com.simibubi.create.compat.computercraft.implementation.CreateLuaTable;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

public class PackageOrderLuaObject implements LuaComparable {
   private final PackageLuaObject parent;
   private final PackageOrderWithCrafts context;

   public PackageOrderLuaObject(PackageLuaObject packageLuaObject) {
      this.parent = packageLuaObject;
      this.context = PackageItem.getOrderContext(this.parent.box);
   }

   @LuaFunction(
      mainThread = true
   )
   public final int getOrderID() throws LuaException {
      return PackageItem.getOrderId(this.parent.box);
   }

   @LuaFunction(
      mainThread = true
   )
   public final int getIndex() throws LuaException {
      return PackageItem.getIndex(this.parent.box) + 1;
   }

   @LuaFunction(
      mainThread = true
   )
   public final boolean isFinal() throws LuaException {
      return PackageItem.isFinal(this.parent.box);
   }

   @LuaFunction(
      mainThread = true
   )
   public final int getLinkIndex() throws LuaException {
      return PackageItem.getLinkIndex(this.parent.box) + 1;
   }

   @LuaFunction(
      mainThread = true
   )
   public final boolean isFinalLink() throws LuaException {
      return PackageItem.isFinalLink(this.parent.box);
   }

   @LuaFunction(
      mainThread = true
   )
   public final CreateLuaTable list() throws LuaException {
      if (this.context == null) {
         return null;
      } else {
         CreateLuaTable stacks = new CreateLuaTable();
         int i = 0;

         for (BigItemStack bis : this.context.stacks()) {
            i++;
            Map<String, Object> details = new HashMap<>(VanillaDetailRegistries.ITEM_STACK.getBasicDetails(bis.stack));
            details.put("count", bis.count);
            stacks.put(i, details);
         }

         return stacks;
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public final CreateLuaTable getItemDetail(int slot) throws LuaException {
      if (this.context == null) {
         return null;
      } else if (slot < 1) {
         throw new LuaException("Slot out of range (1 or greater)");
      } else {
         List<BigItemStack> stacks = this.context.stacks();
         if (slot > stacks.size()) {
            return null;
         } else {
            BigItemStack bis = stacks.get(slot - 1);
            Map<String, Object> details = new HashMap<>(VanillaDetailRegistries.ITEM_STACK.getDetails(bis.stack));
            details.put("count", bis.count);
            return new CreateLuaTable(details);
         }
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public final CreateLuaTable getCrafts() throws LuaException {
      if (this.context == null) {
         return null;
      } else {
         CreateLuaTable crafts = new CreateLuaTable();
         int i = 0;

         for (PackageOrderWithCrafts.CraftingEntry entry : this.context.orderedCrafts()) {
            CreateLuaTable craft = new CreateLuaTable();
            craft.put("count", entry.count());
            CreateLuaTable recipe = new CreateLuaTable();
            int j = 0;

            for (BigItemStack bis : entry.pattern().stacks()) {
               j++;
               String name = VanillaDetailRegistries.ITEM_STACK.getBasicDetails(bis.stack).get("name").toString();
               recipe.put(j, name.equals("minecraft:air") ? null : name);
            }

            i++;
            craft.put("recipe", recipe);
            crafts.put(i, craft);
         }

         return crafts;
      }
   }

   public final List<LuaBigItemStack> getLuaItemStacks() {
      List<LuaBigItemStack> result = new ArrayList<>();

      for (BigItemStack bis : this.context.stacks()) {
         ItemStack stack = bis.stack;
         if (!stack.isEmpty()) {
            result.add(new LuaBigItemStack(bis));
         }
      }

      return result;
   }

   @Override
   public Map<?, ?> getTableRepresentation() {
      try {
         Map<String, Object> result = new HashMap<>();
         result.put("orderID", this.getOrderID());
         result.put("index", this.getIndex());
         result.put("isFinal", this.isFinal());
         result.put("linkIndex", this.getLinkIndex());
         result.put("isFinalLink", this.isFinalLink());
         if (this.context != null) {
            result.put("stacks", this.getLuaItemStacks());
            result.put("crafts", this.getCrafts());
         }

         return result;
      } catch (LuaException var2) {
         return null;
      }
   }
}
