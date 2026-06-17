package com.simibubi.create.foundation.utility;

import java.util.Collection;
import java.util.List;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public class RecipeGenericsUtil {
   public static <P extends Recipe<?>, C extends P> List<RecipeHolder<P>> cast(List<RecipeHolder<C>> list) {
      return list;
   }

   public static Collection<RecipeHolder<? extends Recipe<?>>> specify(Collection<RecipeHolder<?>> list) {
      return list;
   }
}
