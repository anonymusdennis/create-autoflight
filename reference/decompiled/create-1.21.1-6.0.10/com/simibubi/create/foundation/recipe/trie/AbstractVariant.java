package com.simibubi.create.foundation.recipe.trie;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

public sealed interface AbstractVariant permits AbstractVariant.AbstractItem, AbstractVariant.AbstractFluid {
   public static final class AbstractFluid implements AbstractVariant {
      @NotNull
      private final Fluid fluid;
      private final int hashCode;

      public AbstractFluid(@NotNull Fluid fluid) {
         this.fluid = fluid;
         this.hashCode = fluid.hashCode();
      }

      @Override
      public boolean equals(Object o) {
         return o instanceof AbstractVariant.AbstractFluid that ? this.fluid == that.fluid : false;
      }

      @Override
      public int hashCode() {
         return this.hashCode;
      }
   }

   public static final class AbstractItem implements AbstractVariant {
      @NotNull
      private final Item item;
      private final int hashCode;

      public AbstractItem(@NotNull Item item) {
         this.item = item;
         this.hashCode = item.hashCode();
      }

      @Override
      public boolean equals(Object o) {
         return o instanceof AbstractVariant.AbstractItem that ? this.item == that.item : false;
      }

      @Override
      public int hashCode() {
         return this.hashCode;
      }
   }
}
