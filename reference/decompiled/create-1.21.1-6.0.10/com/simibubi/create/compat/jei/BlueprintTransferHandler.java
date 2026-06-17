package com.simibubi.create.compat.jei;

import com.simibubi.create.content.equipment.blueprint.BlueprintAssignCompleteRecipePacket;
import com.simibubi.create.content.equipment.blueprint.BlueprintMenu;
import java.util.Optional;
import javax.annotation.ParametersAreNonnullByDefault;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlueprintTransferHandler implements IRecipeTransferHandler<BlueprintMenu, RecipeHolder<CraftingRecipe>> {
   public Class<? extends BlueprintMenu> getContainerClass() {
      return BlueprintMenu.class;
   }

   public Optional<MenuType<BlueprintMenu>> getMenuType() {
      return Optional.empty();
   }

   public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
      return RecipeTypes.CRAFTING;
   }

   @Nullable
   public IRecipeTransferError transferRecipe(
      BlueprintMenu menu, RecipeHolder<CraftingRecipe> craftingRecipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer
   ) {
      if (!doTransfer) {
         return null;
      } else {
         CatnipServices.NETWORK.sendToServer(new BlueprintAssignCompleteRecipePacket(craftingRecipe.id()));
         return null;
      }
   }
}
