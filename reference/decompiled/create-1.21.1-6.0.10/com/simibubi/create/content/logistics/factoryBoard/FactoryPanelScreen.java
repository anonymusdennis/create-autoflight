package com.simibubi.create.content.logistics.factoryBoard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

public class FactoryPanelScreen extends AbstractSimiScreen {
   private AddressEditBox addressBox;
   private IconButton confirmButton;
   private IconButton deleteButton;
   private IconButton newInputButton;
   private IconButton relocateButton;
   private IconButton activateCraftingButton;
   private ScrollInput promiseExpiration;
   private FactoryPanelBehaviour behaviour;
   private boolean restocker;
   private boolean sendReset;
   private boolean sendRedstoneReset;
   private BigItemStack outputConfig;
   private List<BigItemStack> inputConfig;
   private List<FactoryPanelConnection> connections;
   private CraftingRecipe availableCraftingRecipe;
   private boolean craftingActive;
   private List<BigItemStack> craftingIngredients;

   public FactoryPanelScreen(FactoryPanelBehaviour behaviour) {
      this.behaviour = behaviour;
      this.minecraft = Minecraft.getInstance();
      this.restocker = behaviour.panelBE().restocker;
      this.availableCraftingRecipe = null;
      this.craftingActive = !behaviour.activeCraftingArrangement.isEmpty();
      this.updateConfigs();
   }

   private void updateConfigs() {
      this.connections = new ArrayList<>(this.behaviour.targetedBy.values());
      this.outputConfig = new BigItemStack(this.behaviour.getFilter(), this.behaviour.recipeOutput);
      this.inputConfig = this.connections.stream().map(c -> {
         FactoryPanelBehaviour b = FactoryPanelBehaviour.at(this.minecraft.level, c.from);
         return b == null ? new BigItemStack(ItemStack.EMPTY, 0) : new BigItemStack(b.getFilter(), c.amount);
      }).toList();
      this.searchForCraftingRecipe();
      if (this.availableCraftingRecipe == null) {
         this.craftingActive = false;
      } else {
         this.craftingIngredients = convertRecipeToPackageOrderContext(this.availableCraftingRecipe, this.inputConfig, false);
      }
   }

   public static List<BigItemStack> convertRecipeToPackageOrderContext(
      CraftingRecipe availableCraftingRecipe, List<BigItemStack> inputs, boolean respectAmounts
   ) {
      List<BigItemStack> craftingIngredients = new ArrayList<>();
      BigItemStack emptyIngredient = new BigItemStack(ItemStack.EMPTY, 1);
      NonNullList<Ingredient> ingredients = availableCraftingRecipe.getIngredients();
      List<BigItemStack> mutableInputs = BigItemStack.duplicateWrappers(inputs);
      int width = Math.min(3, ingredients.size());
      int height = Math.min(3, ingredients.size() / 3 + 1);
      if (availableCraftingRecipe instanceof ShapedRecipe shaped) {
         width = shaped.getWidth();
         height = shaped.getHeight();
      }

      if (height == 1) {
         for (int i = 0; i < 3; i++) {
            craftingIngredients.add(emptyIngredient);
         }
      }

      if (width == 1) {
         craftingIngredients.add(emptyIngredient);
      }

      for (int i = 0; i < ingredients.size(); i++) {
         Ingredient ingredient = (Ingredient)ingredients.get(i);
         BigItemStack craftingIngredient = emptyIngredient;
         if (!ingredient.isEmpty()) {
            for (BigItemStack bigItemStack : mutableInputs) {
               if (bigItemStack.count > 0 && ingredient.test(bigItemStack.stack)) {
                  craftingIngredient = new BigItemStack(bigItemStack.stack, 1);
                  if (respectAmounts) {
                     bigItemStack.count--;
                  }
                  break;
               }
            }
         }

         craftingIngredients.add(craftingIngredient);
         if (width < 3 && (i + 1) % width == 0) {
            for (int j = 0; j < 3 - width; j++) {
               if (craftingIngredients.size() < 9) {
                  craftingIngredients.add(emptyIngredient);
               }
            }
         }
      }

      while (craftingIngredients.size() < 9) {
         craftingIngredients.add(emptyIngredient);
      }

      return craftingIngredients;
   }

   protected void init() {
      int sizeX = AllGuiTextures.FACTORY_GAUGE_BOTTOM.getWidth();
      int sizeY = (this.restocker ? AllGuiTextures.FACTORY_GAUGE_RESTOCK : AllGuiTextures.FACTORY_GAUGE_RECIPE).getHeight()
         + AllGuiTextures.FACTORY_GAUGE_BOTTOM.getHeight();
      this.setWindowSize(sizeX, sizeY);
      super.init();
      this.clearWidgets();
      int x = this.guiLeft;
      int y = this.guiTop;
      if (this.addressBox == null) {
         String frogAddress = this.behaviour.getFrogAddress();
         this.addressBox = new AddressEditBox(this, new NoShadowFontWrapper(this.font), x + 36, y + this.windowHeight - 51, 108, 10, false, frogAddress);
         this.addressBox.setValue(this.behaviour.recipeAddress);
         this.addressBox.setTextColor(5592405);
      }

      this.addressBox.setX(x + 36);
      this.addressBox.setY(y + this.windowHeight - 51);
      this.addRenderableWidget(this.addressBox);
      this.confirmButton = new IconButton(x + sizeX - 33, y + sizeY - 25, AllIcons.I_CONFIRM);
      this.confirmButton.withCallback(() -> this.minecraft.setScreen(null));
      this.confirmButton.setToolTip(CreateLang.translate("gui.factory_panel.save_and_close").component());
      this.addRenderableWidget(this.confirmButton);
      this.deleteButton = new IconButton(x + sizeX - 55, y + sizeY - 25, AllIcons.I_TRASH);
      this.deleteButton.withCallback(() -> {
         this.sendReset = true;
         this.minecraft.setScreen(null);
      });
      this.deleteButton.setToolTip(CreateLang.translate("gui.factory_panel.reset").component());
      this.addRenderableWidget(this.deleteButton);
      this.promiseExpiration = new ScrollInput(x + 97, y + this.windowHeight - 24, 28, 16)
         .withRange(-1, 31)
         .titled(CreateLang.translate("gui.factory_panel.promises_expire_title").component());
      this.promiseExpiration.setState(this.behaviour.promiseClearingInterval);
      this.addRenderableWidget(this.promiseExpiration);
      this.newInputButton = new IconButton(x + 31, y + 47, AllIcons.I_ADD);
      this.newInputButton.withCallback(() -> {
         FactoryPanelConnectionHandler.startConnection(this.behaviour);
         this.minecraft.setScreen(null);
      });
      this.newInputButton.setToolTip(CreateLang.translate("gui.factory_panel.connect_input").component());
      this.relocateButton = new IconButton(x + 31, y + 67, AllIcons.I_MOVE_GAUGE);
      this.relocateButton.withCallback(() -> {
         FactoryPanelConnectionHandler.startRelocating(this.behaviour);
         this.minecraft.setScreen(null);
      });
      this.relocateButton.setToolTip(CreateLang.translate("gui.factory_panel.relocate").component());
      if (!this.restocker) {
         this.addRenderableWidget(this.newInputButton);
         this.addRenderableWidget(this.relocateButton);
      }

      this.activateCraftingButton = null;
      if (this.availableCraftingRecipe != null) {
         this.activateCraftingButton = new IconButton(x + 31, y + 27, AllIcons.I_3x3);
         this.activateCraftingButton.withCallback(() -> {
            this.craftingActive = !this.craftingActive;
            this.init();
            if (this.craftingActive) {
               this.outputConfig.count = this.availableCraftingRecipe.getResultItem(this.minecraft.level.registryAccess()).getCount();
            }
         });
         this.activateCraftingButton.setToolTip(CreateLang.translate("gui.factory_panel.activate_crafting").component());
         this.addRenderableWidget(this.activateCraftingButton);
      }
   }

   public void tick() {
      super.tick();
      if (this.inputConfig.size() != this.behaviour.targetedBy.size()) {
         this.updateConfigs();
         this.init();
      }

      if (this.activateCraftingButton != null) {
         this.activateCraftingButton.green = this.craftingActive;
      }

      this.addressBox.tick();
      this.promiseExpiration
         .titled(
            CreateLang.translate(
                  this.promiseExpiration.getState() == -1 ? "gui.factory_panel.promises_do_not_expire" : "gui.factory_panel.promises_expire_title"
               )
               .component()
         );
   }

   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      int x = this.guiLeft;
      int y = this.guiTop;
      AllGuiTextures bg = this.restocker ? AllGuiTextures.FACTORY_GAUGE_RESTOCK : AllGuiTextures.FACTORY_GAUGE_RECIPE;
      if (this.restocker) {
         AllGuiTextures.FACTORY_GAUGE_RECIPE.render(graphics, x, y - 16);
      }

      bg.render(graphics, x, y);
      AllGuiTextures.FACTORY_GAUGE_BOTTOM.render(graphics, x, y + bg.getHeight());
      y = this.guiTop;
      int slot = 0;
      if (this.craftingActive) {
         for (BigItemStack itemStack : this.craftingIngredients) {
            this.renderInputItem(graphics, slot++, itemStack, mouseX, mouseY);
         }
      } else {
         for (BigItemStack itemStack : this.inputConfig) {
            this.renderInputItem(graphics, slot++, itemStack, mouseX, mouseY);
         }

         if (this.inputConfig.isEmpty()) {
            int inputX = this.guiLeft + (this.restocker ? 88 : 68 + slot % 3 * 20);
            int inputY = this.guiTop + (this.restocker ? 12 : 28) + slot / 3 * 20;
            if (!this.restocker && mouseY > inputY && mouseY < inputY + 60 && mouseX > inputX && mouseX < inputX + 60) {
               graphics.renderComponentTooltip(
                  this.font,
                  List.of(
                     CreateLang.translate("gui.factory_panel.unconfigured_input").color(ScrollInput.HEADER_RGB).component(),
                     CreateLang.translate("gui.factory_panel.unconfigured_input_tip").style(ChatFormatting.GRAY).component(),
                     CreateLang.translate("gui.factory_panel.unconfigured_input_tip_1").style(ChatFormatting.GRAY).component()
                  ),
                  mouseX,
                  mouseY
               );
            }
         }
      }

      if (this.restocker) {
         this.renderInputItem(graphics, slot, new BigItemStack(this.behaviour.getFilter(), 1), mouseX, mouseY);
      }

      if (!this.restocker) {
         int outputX = x + 160;
         int outputY = y + 48;
         graphics.renderItem(this.outputConfig.stack, outputX, outputY);
         graphics.renderItemDecorations(this.font, this.behaviour.getFilter(), outputX, outputY, this.outputConfig.count + "");
         if (mouseX >= outputX - 1 && mouseX < outputX - 1 + 18 && mouseY >= outputY - 1 && mouseY < outputY - 1 + 18) {
            MutableComponent c1 = CreateLang.translate(
                  "gui.factory_panel.expected_output",
                  CreateLang.itemName(this.outputConfig.stack).add(CreateLang.text(" x" + this.outputConfig.count)).string()
               )
               .color(ScrollInput.HEADER_RGB)
               .component();
            MutableComponent c2 = CreateLang.translate("gui.factory_panel.expected_output_tip").style(ChatFormatting.GRAY).component();
            MutableComponent c3 = CreateLang.translate("gui.factory_panel.expected_output_tip_1").style(ChatFormatting.GRAY).component();
            MutableComponent c4 = CreateLang.translate("gui.factory_panel.expected_output_tip_2")
               .style(ChatFormatting.DARK_GRAY)
               .style(ChatFormatting.ITALIC)
               .component();
            graphics.renderComponentTooltip(this.font, this.craftingActive ? List.of(c1, c2, c3) : List.of(c1, c2, c3, c4), mouseX, mouseY);
         }
      }

      PoseStack ms = graphics.pose();
      ms.pushPose();
      ms.translate(0.0F, 0.0F, 10.0F);
      if (this.addressBox.isHovered() && !this.addressBox.isFocused()) {
         this.showAddressBoxTooltip(graphics, mouseX, mouseY);
      }

      Component title = CreateLang.translate(this.restocker ? "gui.factory_panel.title_as_restocker" : "gui.factory_panel.title_as_recipe").component();
      graphics.drawString(this.font, title, x + 97 - this.font.width(title) / 2, y + (this.restocker ? -12 : 4), 4013128, false);
      int previewY = this.restocker ? 0 : 60;
      ms.pushPose();
      ms.translate(0.0F, (float)previewY, 0.0F);
      GuiGameElement.of(AllBlocks.FACTORY_GAUGE.asStack()).scale(4.0).at(0.0F, 0.0F, -200.0F).render(graphics, x + 195, y + 55);
      if (!this.behaviour.getFilter().isEmpty()) {
         GuiGameElement.of(this.behaviour.getFilter()).scale(1.625).at(0.0F, 0.0F, 100.0F).render(graphics, x + 214, y + 68);
      }

      ms.popPose();
      if (!this.behaviour.targetedByLinks.isEmpty()) {
         ItemStack asStack = AllBlocks.REDSTONE_LINK.asStack();
         int itemX = x + 9;
         int itemY = y + this.windowHeight - 24;
         AllGuiTextures.FROGPORT_SLOT.render(graphics, itemX - 1, itemY - 1);
         graphics.renderItem(asStack, itemX, itemY);
         if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
            List<Component> linkTip = List.of(
               CreateLang.translate("gui.factory_panel.has_link_connections").color(ScrollInput.HEADER_RGB).component(),
               CreateLang.translate("gui.factory_panel.left_click_disconnect").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component()
            );
            graphics.renderComponentTooltip(this.font, linkTip, mouseX, mouseY);
         }
      }

      int state = this.promiseExpiration.getState();
      graphics.drawString(
         this.font,
         CreateLang.text(state == -1 ? " /" : (state == 0 ? "30s" : state + "m")).component(),
         this.promiseExpiration.getX() + 3,
         this.promiseExpiration.getY() + 4,
         -1118482,
         true
      );
      ItemStack asStack = PackageStyles.getDefaultBox();
      int itemX = x + 68;
      int itemY = y + this.windowHeight - 24;
      graphics.renderItem(asStack, itemX, itemY);
      int promised = this.behaviour.getPromised();
      graphics.renderItemDecorations(this.font, asStack, itemX, itemY, promised + "");
      if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
         List<Component> promiseTip = List.of();
         if (promised == 0) {
            promiseTip = List.of(
               CreateLang.translate("gui.factory_panel.no_open_promises").color(ScrollInput.HEADER_RGB).component(),
               CreateLang.translate(this.restocker ? "gui.factory_panel.restocker_promises_tip" : "gui.factory_panel.recipe_promises_tip")
                  .style(ChatFormatting.GRAY)
                  .component(),
               CreateLang.translate(this.restocker ? "gui.factory_panel.restocker_promises_tip_1" : "gui.factory_panel.recipe_promises_tip_1")
                  .style(ChatFormatting.GRAY)
                  .component(),
               CreateLang.translate("gui.factory_panel.promise_prevents_oversending").style(ChatFormatting.GRAY).component()
            );
         } else {
            promiseTip = List.of(
               CreateLang.translate("gui.factory_panel.promised_items").color(ScrollInput.HEADER_RGB).component(),
               CreateLang.text(this.behaviour.getFilter().getHoverName().getString() + " x" + promised).component(),
               CreateLang.translate("gui.factory_panel.left_click_reset").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component()
            );
         }

         graphics.renderComponentTooltip(this.font, promiseTip, mouseX, mouseY);
      }

      ms.popPose();
   }

   private void renderInputItem(GuiGraphics graphics, int slot, BigItemStack itemStack, int mouseX, int mouseY) {
      int inputX = this.guiLeft + (this.restocker ? 88 : 68 + slot % 3 * 20);
      int inputY = this.guiTop + (this.restocker ? 12 : 28) + slot / 3 * 20;
      graphics.renderItem(itemStack.stack, inputX, inputY);
      if (!this.craftingActive && !this.restocker && !itemStack.stack.isEmpty()) {
         graphics.renderItemDecorations(this.font, itemStack.stack, inputX, inputY, itemStack.count + "");
      }

      if (mouseX >= inputX - 2 && mouseX < inputX - 2 + 20 && mouseY >= inputY - 2 && mouseY < inputY - 2 + 20) {
         if (this.craftingActive) {
            graphics.renderComponentTooltip(
               this.font,
               List.of(
                  CreateLang.translate("gui.factory_panel.crafting_input").color(ScrollInput.HEADER_RGB).component(),
                  CreateLang.translate("gui.factory_panel.crafting_input_tip").style(ChatFormatting.GRAY).component(),
                  CreateLang.translate("gui.factory_panel.crafting_input_tip_1").style(ChatFormatting.GRAY).component()
               ),
               mouseX,
               mouseY
            );
         } else if (itemStack.stack.isEmpty()) {
            graphics.renderComponentTooltip(
               this.font,
               List.of(
                  CreateLang.translate("gui.factory_panel.empty_panel").color(ScrollInput.HEADER_RGB).component(),
                  CreateLang.translate("gui.factory_panel.left_click_disconnect").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component()
               ),
               mouseX,
               mouseY
            );
         } else if (this.restocker) {
            graphics.renderComponentTooltip(
               this.font,
               List.of(
                  CreateLang.translate("gui.factory_panel.sending_item", CreateLang.itemName(itemStack.stack).string())
                     .color(ScrollInput.HEADER_RGB)
                     .component(),
                  CreateLang.translate("gui.factory_panel.sending_item_tip").style(ChatFormatting.GRAY).component(),
                  CreateLang.translate("gui.factory_panel.sending_item_tip_1").style(ChatFormatting.GRAY).component()
               ),
               mouseX,
               mouseY
            );
         } else {
            graphics.renderComponentTooltip(
               this.font,
               List.of(
                  CreateLang.translate(
                        "gui.factory_panel.sending_item", CreateLang.itemName(itemStack.stack).add(CreateLang.text(" x" + itemStack.count)).string()
                     )
                     .color(ScrollInput.HEADER_RGB)
                     .component(),
                  CreateLang.translate("gui.factory_panel.scroll_to_change_amount").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component(),
                  CreateLang.translate("gui.factory_panel.left_click_disconnect").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component()
               ),
               mouseX,
               mouseY
            );
         }
      }
   }

   private void showAddressBoxTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
      if (this.addressBox.getValue().isBlank()) {
         if (this.restocker) {
            graphics.renderComponentTooltip(
               this.font,
               List.of(
                  CreateLang.translate("gui.factory_panel.restocker_address").color(ScrollInput.HEADER_RGB).component(),
                  CreateLang.translate("gui.factory_panel.restocker_address_tip").style(ChatFormatting.GRAY).component(),
                  CreateLang.translate("gui.factory_panel.restocker_address_tip_1").style(ChatFormatting.GRAY).component(),
                  CreateLang.translate("gui.schedule.lmb_edit").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component()
               ),
               mouseX,
               mouseY
            );
         } else {
            graphics.renderComponentTooltip(
               this.font,
               List.of(
                  CreateLang.translate("gui.factory_panel.recipe_address").color(ScrollInput.HEADER_RGB).component(),
                  CreateLang.translate("gui.factory_panel.recipe_address_tip").style(ChatFormatting.GRAY).component(),
                  CreateLang.translate("gui.factory_panel.recipe_address_tip_1").style(ChatFormatting.GRAY).component(),
                  CreateLang.translate("gui.schedule.lmb_edit").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component()
               ),
               mouseX,
               mouseY
            );
         }
      } else {
         graphics.renderComponentTooltip(
            this.font,
            List.of(
               CreateLang.translate(this.restocker ? "gui.factory_panel.restocker_address_given" : "gui.factory_panel.recipe_address_given")
                  .color(ScrollInput.HEADER_RGB)
                  .component(),
               CreateLang.text("'" + this.addressBox.getValue() + "'").style(ChatFormatting.GRAY).component()
            ),
            mouseX,
            mouseY
         );
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int pButton) {
      if (this.getFocused() != null && !this.getFocused().isMouseOver(mouseX, mouseY)) {
         this.setFocused(null);
      }

      int x = this.guiLeft;
      int y = this.guiTop;
      if (!this.craftingActive) {
         for (int i = 0; i < this.connections.size(); i++) {
            int inputX = x + 68 + i % 3 * 20;
            int inputY = y + 28 + i / 3 * 20;
            if (mouseX >= (double)inputX && mouseX < (double)(inputX + 16) && mouseY >= (double)inputY && mouseY < (double)(inputY + 16)) {
               this.sendIt(this.connections.get(i).from, false);
               this.playButtonSound();
               return true;
            }
         }
      }

      int itemX = x + 68;
      int itemY = y + this.windowHeight - 24;
      if (mouseX >= (double)itemX && mouseX < (double)(itemX + 16) && mouseY >= (double)itemY && mouseY < (double)(itemY + 16)) {
         this.sendIt(null, true);
         this.playButtonSound();
         return true;
      } else {
         itemX = x + 9;
         itemY = y + this.windowHeight - 24;
         if (mouseX >= (double)itemX && mouseX < (double)(itemX + 16) && mouseY >= (double)itemY && mouseY < (double)(itemY + 16)) {
            this.sendRedstoneReset = true;
            this.sendIt(null, false);
            this.playButtonSound();
            return true;
         } else {
            return super.mouseClicked(mouseX, mouseY, pButton);
         }
      }
   }

   public void playButtonSound() {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.25F));
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      int x = this.guiLeft;
      int y = this.guiTop;
      if (this.addressBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
         return true;
      } else if (this.craftingActive) {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      } else {
         for (int i = 0; i < this.inputConfig.size(); i++) {
            int inputX = x + 68 + i % 3 * 20;
            int inputY = y + 26 + i / 3 * 20;
            if (mouseX >= (double)inputX && mouseX < (double)(inputX + 16) && mouseY >= (double)inputY && mouseY < (double)(inputY + 16)) {
               BigItemStack itemStack = this.inputConfig.get(i);
               if (itemStack.stack.isEmpty()) {
                  return true;
               }

               itemStack.count = Mth.clamp((int)((double)itemStack.count + Math.signum(scrollY) * (double)(hasShiftDown() ? 10 : 1)), 1, 64);
               return true;
            }
         }

         if (!this.restocker) {
            int outputX = x + 160;
            int outputY = y + 48;
            if (mouseX >= (double)outputX && mouseX < (double)(outputX + 16) && mouseY >= (double)outputY && mouseY < (double)(outputY + 16)) {
               BigItemStack itemStack = this.outputConfig;
               itemStack.count = Mth.clamp((int)((double)itemStack.count + Math.signum(scrollY) * (double)(hasShiftDown() ? 10 : 1)), 1, 64);
               return true;
            }
         }

         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   public void removed() {
      this.sendIt(null, false);
      super.removed();
   }

   private void sendIt(@Nullable FactoryPanelPosition toRemove, boolean clearPromises) {
      Map<FactoryPanelPosition, Integer> inputs = new HashMap<>();
      if (this.inputConfig.size() == this.connections.size()) {
         for (int i = 0; i < this.inputConfig.size(); i++) {
            BigItemStack stackInConfig = this.inputConfig.get(i);
            int var10002 = this.craftingActive
               ? (int)this.craftingIngredients
                  .stream()
                  .filter(b -> !b.stack.isEmpty() && ItemStack.isSameItemSameComponents(b.stack, stackInConfig.stack))
                  .count()
               : stackInConfig.count;
            inputs.put(this.connections.get(i).from, var10002);
         }
      }

      List<ItemStack> craftingArrangement = this.craftingActive ? this.craftingIngredients.stream().map(b -> b.stack).toList() : List.of();
      FactoryPanelPosition pos = this.behaviour.getPanelPosition();
      int promiseExp = this.promiseExpiration.getState();
      String address = this.addressBox.getValue();
      FactoryPanelConfigurationPacket packet = new FactoryPanelConfigurationPacket(
         pos, address, inputs, craftingArrangement, this.outputConfig.count, promiseExp, toRemove, clearPromises, this.sendReset, this.sendRedstoneReset
      );
      CatnipServices.NETWORK.sendToServer(packet);
   }

   private void searchForCraftingRecipe() {
      ItemStack output = this.outputConfig.stack;
      if (!output.isEmpty()) {
         if (!this.behaviour.targetedBy.isEmpty()) {
            Set<Item> itemsToUse = this.inputConfig.stream().map(b -> b.stack).filter(i -> !i.isEmpty()).map(i -> i.getItem()).collect(Collectors.toSet());
            ClientLevel level = Minecraft.getInstance().level;
            this.availableCraftingRecipe = level.getRecipeManager()
               .getAllRecipesFor(RecipeType.CRAFTING)
               .parallelStream()
               .filter(r -> output.getItem() == ((CraftingRecipe)r.value()).getResultItem(level.registryAccess()).getItem())
               .filter(r -> {
                  if (AllRecipeTypes.shouldIgnoreInAutomation((RecipeHolder<?>)r)) {
                     return false;
                  } else {
                     Set<Item> itemsUsed = new HashSet<>();

                     for (Ingredient ingredient : ((CraftingRecipe)r.value()).getIngredients()) {
                        if (!ingredient.isEmpty()) {
                           boolean available = false;

                           for (BigItemStack bis : this.inputConfig) {
                              if (!bis.stack.isEmpty() && ingredient.test(bis.stack)) {
                                 available = true;
                                 itemsUsed.add(bis.stack.getItem());
                                 break;
                              }
                           }

                           if (!available) {
                              return false;
                           }
                        }
                     }

                     return itemsUsed.size() >= itemsToUse.size();
                  }
               })
               .findAny()
               .<CraftingRecipe>map(RecipeHolder::value)
               .orElse(null);
         }
      }
   }
}
