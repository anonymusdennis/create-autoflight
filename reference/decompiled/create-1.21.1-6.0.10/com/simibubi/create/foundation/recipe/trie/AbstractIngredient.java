package com.simibubi.create.foundation.recipe.trie;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class AbstractIngredient {
   final Set<AbstractVariant> variants;
   final int hashCode;

   public AbstractIngredient(Set<AbstractVariant> variants) {
      this.variants = ImmutableSet.copyOf(variants);
      this.hashCode = variants.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof AbstractIngredient that) {
         return this == that ? true : this.hashCode == that.hashCode && this.variants.equals(that.variants);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.hashCode;
   }

   public static class Universal extends AbstractIngredient {
      public static final AbstractIngredient.Universal INSTANCE = new AbstractIngredient.Universal();
      private static final int hashCode = AbstractIngredient.Universal.class.hashCode();

      private Universal() {
         super(Set.of());
      }

      @Override
      public boolean equals(Object obj) {
         return obj instanceof AbstractIngredient.Universal;
      }

      @Override
      public int hashCode() {
         return hashCode;
      }
   }
}
