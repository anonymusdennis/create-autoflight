package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.redstoneRequester.AutoRequestData;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class TableClothShopPeripheral extends SyncedPeripheral<TableClothBlockEntity> {
   public TableClothShopPeripheral(TableClothBlockEntity blockEntity) {
      super(blockEntity);
   }

   private void assertShop() throws LuaException {
      if (!this.blockEntity.isShop()) {
         throw new LuaException("TableCloth is not a shop!");
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public final boolean isShop() {
      return this.blockEntity.isShop();
   }

   @LuaFunction(
      mainThread = true
   )
   public final String getAddress() throws LuaException {
      this.assertShop();
      return this.blockEntity.requestData.encodedTargetAddress();
   }

   @LuaFunction(
      mainThread = true
   )
   public final void setAddress(String address) throws LuaException {
      this.assertShop();
      AutoRequestData.Mutable mutable = new AutoRequestData.Mutable(this.blockEntity.requestData);
      mutable.encodedTargetAddress = address;
      this.blockEntity.requestData = mutable.toImmutable();
   }

   @LuaFunction(
      mainThread = true
   )
   public final Map<String, ?> getPriceTagItem() throws LuaException {
      this.assertShop();
      return VanillaDetailRegistries.ITEM_STACK.getDetails(this.blockEntity.priceTag.getFilter());
   }

   @LuaFunction(
      mainThread = true
   )
   public final void setPriceTagItem(Optional<String> itemName) throws LuaException {
      this.assertShop();
      ResourceLocation resourceLocation = ResourceLocation.tryParse("minecraft:air");
      if (itemName.isPresent()) {
         resourceLocation = ResourceLocation.tryParse(itemName.get());
      }

      ItemLike item = (ItemLike)BuiltInRegistries.ITEM.get(resourceLocation);
      this.blockEntity.priceTag.setFilter(new ItemStack(item));
   }

   @LuaFunction(
      mainThread = true
   )
   public final int getPriceTagCount() throws LuaException {
      this.assertShop();
      return this.blockEntity.priceTag.count;
   }

   @LuaFunction(
      mainThread = true
   )
   public final void setPriceTagCount(Optional<Double> argument) throws LuaException {
      this.assertShop();
      if (argument.isPresent()) {
         this.blockEntity.priceTag.count = Math.max(1, Math.min(100, argument.get().intValue()));
      } else {
         this.blockEntity.priceTag.count = 1;
      }

      this.blockEntity.notifyUpdate();
   }

   @LuaFunction(
      mainThread = true
   )
   public final Map<Integer, Map<String, ?>> getWares() throws LuaException {
      this.assertShop();
      List<BigItemStack> wares = this.blockEntity.requestData.encodedRequest().stacks();
      Map<Integer, Map<String, ?>> result = new HashMap<>();

      for (int i = 0; i < wares.size(); i++) {
         ItemStack stack = wares.get(i).stack;
         Map<String, Object> details = new HashMap<>(VanillaDetailRegistries.ITEM_STACK.getDetails(stack));
         details.put("count", wares.get(i).count);
         result.put(i + 1, details);
      }

      return result;
   }

   @LuaFunction(
      mainThread = true
   )
   public final void setWares(IArguments arguments) throws LuaException {
      if (!this.blockEntity.manuallyAddedItems.isEmpty()) {
         throw new LuaException("Tablecloth isn't empty.");
      } else {
         ArrayList<BigItemStack> list = new ArrayList<>();

         for (int i = 0; i <= 8; i++) {
            if (arguments.get(i) != null) {
               Map<?, ?> itemData = arguments.getTable(i);
               if (!(itemData instanceof Map)) {
                  throw new LuaException("Table or nil expected for each item entry");
               }

               String itemName = "minecraft:air";
               if (itemData.get("name") instanceof String) {
                  itemName = (String)itemData.get("name");
               }

               int count = 1;
               if (itemData.get("count") instanceof Number) {
                  Object countObj = itemData.get("count");
                  count = countObj instanceof Number ? ((Number)countObj).intValue() : 1;
                  if (count > 256) {
                     throw new LuaException("Count for item " + itemName + " exceeds 256");
                  }
               }

               ResourceLocation resourceLocation = ResourceLocation.tryParse(itemName);
               ItemLike item = (ItemLike)BuiltInRegistries.ITEM.get(resourceLocation);
               ItemStack itemStack = new ItemStack(item);
               if (itemStack.isEmpty()) {
                  throw new LuaException("Invalid item at index: " + (i + 1));
               }

               list.add(new BigItemStack(itemStack, count));
            }
         }

         AutoRequestData.Mutable mutable = new AutoRequestData.Mutable(this.blockEntity.requestData);
         mutable.encodedRequest = PackageOrderWithCrafts.simple(list);
         this.blockEntity.requestData = mutable.toImmutable();
         this.blockEntity.notifyUpdate();
         this.blockEntity.notifyShopUpdate();
      }
   }

   public String getType() {
      return "Create_TableClothShop";
   }
}
