package com.simibubi.create.compat.computercraft.implementation;

import com.simibubi.create.compat.computercraft.implementation.luaObjects.LuaComparable;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.createmod.catnip.data.Glob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class ComputerUtil {
   public static int bigItemStackToLuaTableFilter(BigItemStack entry, Map<?, ?> filter) throws LuaException {
      Map<String, Object> details = VanillaDetailRegistries.ITEM_STACK.getDetails(entry.stack);
      details.put("count", entry.count);
      if (filter.containsKey("name") && filter.get("name") instanceof String name && !name.contains(":")) {
         details.put("name", "minecraft:" + name);
      }

      return !deepEquals(new HashMap<>(filter), details) ? 0 : entry.count;
   }

   private static boolean deepEquals(Object fVal, Object iVal) throws LuaException {
      if (Objects.equals(iVal, fVal)) {
         return true;
      } else if (iVal instanceof LuaComparable iStack) {
         return deepEquals(fVal, iStack.getTableRepresentation());
      } else {
         if (fVal instanceof Number fn && iVal instanceof Number in) {
            return Double.compare(fn.doubleValue(), in.doubleValue()) == 0;
         }

         if (fVal instanceof Map<?, ?> fMap && fMap.get("_op") instanceof String op && fMap.get("value") != null) {
            Object fValue = fMap.get("value");
            switch (op) {
               case "not":
                  return !deepEquals(fValue, iVal);
               case "any":
               case "all":
                  String errorMsg = op + " operator requires a list of values";
                  if (fValue instanceof Map<?, ?> valueMap) {
                     List<?> values = toOrderedList(valueMap);
                     if (values == null) {
                        throw new LuaException(errorMsg);
                     }

                     boolean isAll = op.equals("all");

                     for (Object v : values) {
                        boolean match = deepEquals(v, iVal);
                        if (isAll) {
                           if (!match) {
                              return false;
                           }
                        } else if (match) {
                           return true;
                        }
                     }

                     return isAll;
                  }

                  throw new LuaException(errorMsg);
               case "type":
                  if (!(fValue instanceof String type)) {
                     throw new LuaException("Type operator requires a string value");
                  }

                  if (iVal == null) {
                     return type.equals("nil");
                  }
                  return switch (type) {
                     case "nil" -> iVal == null;
                     case "number" -> iVal instanceof Number;
                     case "string" -> iVal instanceof String;
                     case "boolean" -> iVal instanceof Boolean;
                     case "table" -> iVal instanceof Map || iVal instanceof List;
                     case "list" -> iVal instanceof List;
                     case "map" -> iVal instanceof Map;
                     case "object" -> iVal instanceof LuaComparable;
                     default -> throw new LuaException("Unknown type: " + type);
                  };
               default:
                  if (iVal instanceof Number in && fValue instanceof Number val) {
                     return switch (op) {
                        case ">" -> in.doubleValue() > val.doubleValue();
                        case ">=" -> in.doubleValue() >= val.doubleValue();
                        case "<" -> in.doubleValue() < val.doubleValue();
                        case "<=" -> in.doubleValue() <= val.doubleValue();
                        case "==" -> in.doubleValue() == val.doubleValue();
                        case "~=" -> in.doubleValue() != val.doubleValue();
                        default -> throw new LuaException("Unknown operator: " + op);
                     };
                  }

                  if (iVal instanceof String inStr && fValue instanceof String fStr) {
                     return switch (op) {
                        case "glob" -> inStr.matches(Glob.toRegexPattern(fStr, ""));
                        case "regex" -> inStr.matches(fStr);
                        default -> throw new LuaException("Unknown operator: " + op);
                     };
                  }

                  throw new LuaException("Operator " + op + " not supported for type " + (fValue == null ? "null" : fValue.getClass().getSimpleName()));
            }
         }

         ComputerUtil.Collection fColl = ComputerUtil.Collection.of(fVal);
         ComputerUtil.Collection iColl = ComputerUtil.Collection.of(iVal);
         if (fColl == null || iColl == null) {
            return false;
         } else if (iColl.isList() && fColl.isList()) {
            return matchList(fColl, iColl);
         } else {
            return iColl.isMap() && fColl.isMap() ? matchMap(fColl, iColl) : false;
         }
      }
   }

   private static boolean matchList(ComputerUtil.Collection f, ComputerUtil.Collection i) throws LuaException {
      switch (f.mode) {
         case EXACT:
            if (f.list.size() != i.list.size()) {
               return false;
            } else {
               for (int k = 0; k < f.list.size(); k++) {
                  if (!deepEquals(f.list.get(k), i.list.get(k))) {
                     return false;
                  }
               }

               return true;
            }
         case CONTAINS:
            label61:
            for (Object fVal : f.list) {
               Iterator<?> it = i.list.iterator();

               while (it.hasNext()) {
                  Object iVal = it.next();
                  if (deepEquals(fVal, iVal)) {
                     it.remove();
                     continue label61;
                  }
               }

               return false;
            }

            return true;
         case CONTAINED:
            label52:
            for (Object iVal : i.list) {
               Iterator<?> it = f.list.iterator();

               while (it.hasNext()) {
                  Object fVal = it.next();
                  if (deepEquals(fVal, iVal)) {
                     it.remove();
                     continue label52;
                  }
               }

               return false;
            }

            return true;
         default:
            return false;
      }
   }

   private static boolean matchMap(ComputerUtil.Collection f, ComputerUtil.Collection i) throws LuaException {
      switch (f.mode) {
         case EXACT:
            if (!f.map.keySet().equals(i.map.keySet())) {
               return false;
            } else {
               for (Entry<?, ?> ex : f.map.entrySet()) {
                  if (!deepEquals(ex.getValue(), i.map.get(ex.getKey()))) {
                     return false;
                  }
               }

               return true;
            }
         case CONTAINS:
            for (Entry<?, ?> e : f.map.entrySet()) {
               if (!i.map.containsKey(e.getKey()) || !deepEquals(e.getValue(), i.map.get(e.getKey()))) {
                  return false;
               }
            }

            return true;
         case CONTAINED:
            for (Entry<?, ?> exx : i.map.entrySet()) {
               if (!f.map.containsKey(exx.getKey()) || !deepEquals(f.map.get(exx.getKey()), exx.getValue())) {
                  return false;
               }
            }

            return true;
         default:
            return false;
      }
   }

   private static boolean isArrayLike(Map<?, ?> map) {
      int n = map.size();
      if (n == 0) {
         return true;
      } else {
         boolean[] seen = new boolean[n];

         for (Object keyObj : map.keySet()) {
            if (!(keyObj instanceof Number)) {
               return false;
            }

            int k = ((Number)keyObj).intValue() - 1;
            if ((double)k != (double)k) {
               return false;
            }

            if (k < 0 || k >= n || seen[k]) {
               return false;
            }

            seen[k] = true;
         }

         for (boolean ok : seen) {
            if (!ok) {
               return false;
            }
         }

         return true;
      }
   }

   private static List<Object> toOrderedList(Map<?, ?> m) {
      if (!isArrayLike(m)) {
         return null;
      } else {
         int n = m.size();
         List<Object> out = new ArrayList<>(Collections.nCopies(n, null));

         for (Entry<?, ?> e : m.entrySet()) {
            out.set(((Number)e.getKey()).intValue() - 1, e.getValue());
         }

         return out;
      }
   }

   public static Map<Integer, Map<String, ?>> list(IItemHandler inventory) {
      Map<Integer, Map<String, ?>> result = new HashMap<>();
      int size = inventory.getSlots();

      for (int i = 0; i < size; i++) {
         ItemStack stack = inventory.getStackInSlot(i);
         if (!stack.isEmpty()) {
            result.put(i + 1, VanillaDetailRegistries.ITEM_STACK.getBasicDetails(stack));
         }
      }

      return result;
   }

   public static Map<String, ?> getItemDetail(IItemHandler inventory, int slot) throws LuaException {
      int maxSlots = inventory.getSlots();
      if (slot >= 1 && slot <= maxSlots) {
         ItemStack stack = inventory.getStackInSlot(slot - 1);
         return stack.isEmpty() ? null : VanillaDetailRegistries.ITEM_STACK.getDetails(stack);
      } else {
         throw new LuaException(String.format("Slot " + slot + " out of range, available slots between 1 and " + maxSlots));
      }
   }

   public static Map<String, ?> getItemDetail(InventorySummary inventorySummary, int slot) throws LuaException {
      List<BigItemStack> stacks = inventorySummary.getStacks();
      int maxSlots = stacks.size();
      if (slot >= 1 && slot <= maxSlots) {
         BigItemStack entry = stacks.get(slot - 1);
         Map<String, Object> details = new HashMap<>(VanillaDetailRegistries.ITEM_STACK.getDetails(entry.stack));
         details.put("count", entry.count);
         return entry.stack.isEmpty() ? null : details;
      } else {
         throw new LuaException(String.format("Slot " + slot + " out of range, available slots between 1 and " + maxSlots));
      }
   }

   private static record Collection(ComputerUtil.MatchMode mode, List<?> list, Map<?, ?> map) {
      boolean isList() {
         return this.list != null;
      }

      boolean isMap() {
         return this.map != null;
      }

      static ComputerUtil.Collection of(Object o) throws LuaException {
         if (o instanceof Map<?, ?> m) {
            ComputerUtil.MatchMode mode = ComputerUtil.MatchMode.parse(m.get("_mode"));
            m.remove("_mode");
            List<Object> lst = ComputerUtil.toOrderedList(m);
            return new ComputerUtil.Collection(mode, lst, m);
         } else {
            return o instanceof List<?> raw ? new ComputerUtil.Collection(ComputerUtil.MatchMode.CONTAINS, raw, null) : null;
         }
      }
   }

   private static enum MatchMode {
      EXACT,
      CONTAINS,
      CONTAINED;

      static ComputerUtil.MatchMode parse(Object t) throws LuaException {
         if (t instanceof String s) {
            String var2 = s.toLowerCase();

            return switch (var2) {
               case "exact" -> EXACT;
               case "contains" -> CONTAINS;
               case "contained" -> CONTAINED;
               default -> throw new LuaException("Invalid match mode: " + s + ", expected 'exact', 'contained' or 'contains'");
            };
         } else {
            return CONTAINS;
         }
      }
   }
}
