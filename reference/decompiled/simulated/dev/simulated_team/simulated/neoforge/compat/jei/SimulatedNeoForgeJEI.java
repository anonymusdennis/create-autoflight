package dev.simulated_team.simulated.neoforge.compat.jei;

import dev.simulated_team.simulated.Simulated;
import javax.annotation.ParametersAreNonnullByDefault;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

@ParametersAreNonnullByDefault
@JeiPlugin
public class SimulatedNeoForgeJEI implements IModPlugin {
   private static final ResourceLocation ID = Simulated.path("jei_plugin");

   public ResourceLocation getPluginUid() {
      return ID;
   }

   public void registerRecipes(IRecipeRegistration registration) {
      registration.addRecipes(RecipeTypes.CRAFTING, PortableEngineDyeingRecipeMaker.createRecipes().toList());
   }
}
