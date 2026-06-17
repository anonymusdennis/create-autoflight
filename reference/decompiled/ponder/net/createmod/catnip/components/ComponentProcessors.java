package net.createmod.catnip.components;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.ItemStack;

public class ComponentProcessors {
   public static ItemStack withUnsafeComponentsDiscarded(ItemStack stack) {
      if (stack.getComponentsPatch().isEmpty()) {
         return stack;
      } else {
         ItemStack copy = stack.copy();
         stack.getComponents().stream().filter(ComponentProcessors::isUnsafeItemComponent).map(TypedDataComponent::type).forEach(copy::remove);
         return copy;
      }
   }

   public static boolean isUnsafeItemComponent(TypedDataComponent<?> component) {
      return isUnsafeItemComponent(component.type());
   }

   public static boolean isUnsafeItemComponent(DataComponentType<?> component) {
      if (component.equals(DataComponents.ENCHANTMENTS)) {
         return false;
      } else if (component.equals(DataComponents.POTION_CONTENTS)) {
         return false;
      } else {
         return component.equals(DataComponents.DAMAGE) ? false : !component.equals(DataComponents.CUSTOM_NAME);
      }
   }
}
