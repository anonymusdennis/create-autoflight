package com.simibubi.create.compat.jei;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.fluids.potion.PotionFluid;
import java.util.List;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class PotionFluidSubtypeInterpreter implements ISubtypeInterpreter<FluidStack> {
   @Nullable
   public Object getSubtypeData(FluidStack ingredient, UidContext context) {
      if (ingredient.getComponentsPatch().isEmpty()) {
         return null;
      } else {
         PotionContents contents = (PotionContents)ingredient.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         String potionTypeString = ingredient.getDescriptionId();
         String bottleType = ((PotionFluid.BottleType)ingredient.getOrDefault(AllDataComponents.POTION_FLUID_BOTTLE_TYPE, PotionFluid.BottleType.REGULAR))
            .name();
         StringBuilder stringBuilder = new StringBuilder(potionTypeString);
         List<MobEffectInstance> effects = contents.customEffects();
         stringBuilder.append(";").append(bottleType);
         contents.potion().ifPresent(p -> {
            for (MobEffectInstance effectx : ((Potion)p.value()).getEffects()) {
               stringBuilder.append(";").append(effectx);
            }
         });

         for (MobEffectInstance effect : effects) {
            stringBuilder.append(";").append(effect);
         }

         return stringBuilder.toString();
      }
   }

   public String getLegacyStringSubtypeInfo(FluidStack ingredient, UidContext context) {
      return "";
   }
}
