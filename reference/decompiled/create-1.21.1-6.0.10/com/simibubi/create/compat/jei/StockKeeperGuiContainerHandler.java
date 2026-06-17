package com.simibubi.create.compat.jei;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import java.util.Optional;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

public record StockKeeperGuiContainerHandler(IIngredientManager ingredientManager) implements IGuiContainerHandler<StockKeeperRequestScreen> {
   public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(StockKeeperRequestScreen containerScreen, double mouseX, double mouseY) {
      return containerScreen.getHoveredIngredient((int)mouseX, (int)mouseY)
         .flatMap(pair -> this.ingredientManager.createClickableIngredient((ItemStack)pair.getFirst(), (Rect2i)pair.getSecond(), true));
   }
}
