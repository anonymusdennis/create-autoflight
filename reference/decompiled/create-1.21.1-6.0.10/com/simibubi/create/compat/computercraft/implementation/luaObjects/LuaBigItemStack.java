package com.simibubi.create.compat.computercraft.implementation.luaObjects;

import com.simibubi.create.content.logistics.BigItemStack;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import java.util.Map;

public class LuaBigItemStack implements LuaComparable {
   private final BigItemStack stack;

   public LuaBigItemStack(BigItemStack stack) {
      this.stack = stack;
   }

   @Override
   public Map<?, ?> getTableRepresentation() {
      Map<String, Object> details = VanillaDetailRegistries.ITEM_STACK.getDetails(this.stack.stack);
      details.put("count", this.stack.count);
      return details;
   }
}
