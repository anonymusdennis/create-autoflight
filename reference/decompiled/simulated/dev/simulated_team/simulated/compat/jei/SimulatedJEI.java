package dev.simulated_team.simulated.compat.jei;

import com.simibubi.create.compat.jei.GhostIngredientHandler;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.client.SearchAlias;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterScreen;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IModInfoRegistration;
import mezz.jei.library.ingredients.itemStacks.TypedItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@ParametersAreNonnullByDefault
@JeiPlugin
public class SimulatedJEI implements IModPlugin {
   private static final ResourceLocation ID = Simulated.path("jei_plugin");

   public ResourceLocation getPluginUid() {
      return ID;
   }

   public void registerGuiHandlers(IGuiHandlerRegistration registration) {
      super.registerGuiHandlers(registration);
      registration.addGhostIngredientHandler(LinkedTypewriterScreen.class, new GhostIngredientHandler());
   }

   public void registerModInfo(IModInfoRegistration modAliasRegistration) {
      for (String mod : SimulatedRegistrate.MODS) {
         for (String otherMod : SimulatedRegistrate.MODS.stream().filter(v -> !v.equals(mod)).toList()) {
            modAliasRegistration.addModAliases(mod, new String[]{otherMod});
         }
      }
   }

   public void registerIngredientAliases(IIngredientAliasRegistration registration) {
      for (SearchAlias searchAlias : SimResourceManagers.SEARCH_ALIAS.entries()) {
         List<ITypedIngredient<ItemStack>> ingredients = searchAlias.getItems().stream().<ITypedIngredient<ItemStack>>map(TypedItemStack::create).toList();
         registration.addAliases(ingredients, searchAlias.terms());
      }
   }
}
