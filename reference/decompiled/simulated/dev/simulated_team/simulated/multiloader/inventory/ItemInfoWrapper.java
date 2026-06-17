package dev.simulated_team.simulated.multiloader.inventory;

import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponentPatch.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record ItemInfoWrapper(Item type, DataComponentPatch patchMap) {
   public static ItemInfoWrapper generateFromStack(ItemStack stack) {
      return new ItemInfoWrapper(stack.getItem(), stack.getComponentsPatch());
   }

   @NotNull
   public static ItemStack generateFromInfo(ItemInfoWrapper info) {
      ItemStack newStack = info.type().getDefaultInstance();
      Builder builder = DataComponentPatch.builder();

      for (Entry<DataComponentType<?>, Optional<?>> set : info.patchMap().entrySet()) {
         setDataComponent(set.getKey(), set.getValue(), builder);
      }

      newStack.applyComponents(builder.build());
      return newStack;
   }

   private static <T> void setDataComponent(DataComponentType<?> type, Optional<?> set, Builder builder) {
      if (set.isEmpty()) {
         builder.remove(type);
      } else {
         builder.set(type, set.get());
      }
   }
}
