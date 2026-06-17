package com.simibubi.create.content.logistics.packagePort;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.GuiGameElement.GuiRenderBuilder;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class PackagePortScreen extends AbstractSimiContainerScreen<PackagePortMenu> {
   private boolean frogMode;
   private AllGuiTextures background;
   private EditBox addressBox;
   private IconButton confirmButton;
   private IconButton dontAcceptPackages;
   private IconButton acceptPackages;
   private ItemStack icon;
   private List<Rect2i> extraAreas = Collections.emptyList();

   public PackagePortScreen(PackagePortMenu container, Inventory inv, Component title) {
      super(container, inv, title);
      this.background = AllGuiTextures.FROGPORT_BG;
      this.frogMode = container.contentHolder instanceof FrogportBlockEntity;
      this.icon = new ItemStack(container.contentHolder.getBlockState().getBlock().asItem());
   }

   @Override
   protected void init() {
      this.setWindowSize(this.background.getWidth(), this.background.getHeight() + AllGuiTextures.PLAYER_INVENTORY.getHeight());
      super.init();
      this.clearWidgets();
      int x = this.getGuiLeft();
      int y = this.getGuiTop();
      Consumer<String> onTextChanged = s -> this.addressBox.setX(this.nameBoxX(s, this.addressBox));
      this.addressBox = new EditBox(new NoShadowFontWrapper(this.font), x + 23, y - 11, this.background.getWidth() - 20, 10, Component.empty());
      this.addressBox.setBordered(false);
      this.addressBox.setMaxLength(25);
      this.addressBox.setTextColor(4013128);
      this.addressBox.setValue(((PackagePortMenu)this.menu).contentHolder.addressFilter);
      this.addressBox.setFocused(false);
      this.addressBox.mouseClicked(0.0, 0.0, 0);
      this.addressBox.setResponder(onTextChanged);
      this.addressBox.setX(this.nameBoxX(this.addressBox.getValue(), this.addressBox));
      this.addRenderableWidget(this.addressBox);
      this.confirmButton = new IconButton(x + this.background.getWidth() - 33, y + this.background.getHeight() - 24, AllIcons.I_CONFIRM);
      this.confirmButton.withCallback(() -> this.minecraft.player.closeContainer());
      this.addRenderableWidget(this.confirmButton);
      this.acceptPackages = new IconButton(x + 37, y + this.background.getHeight() - 24, AllIcons.I_SEND_AND_RECEIVE);
      this.acceptPackages.withCallback(() -> {
         this.acceptPackages.green = true;
         this.dontAcceptPackages.green = false;
      });
      this.acceptPackages.green = ((PackagePortMenu)this.menu).contentHolder.acceptsPackages;
      this.acceptPackages.setToolTip(CreateLang.translateDirect("gui.package_port.send_and_receive"));
      this.addRenderableWidget(this.acceptPackages);
      this.dontAcceptPackages = new IconButton(x + 37 + 18, y + this.background.getHeight() - 24, AllIcons.I_SEND_ONLY);
      this.dontAcceptPackages.withCallback(() -> {
         this.acceptPackages.green = false;
         this.dontAcceptPackages.green = true;
      });
      this.dontAcceptPackages.green = !((PackagePortMenu)this.menu).contentHolder.acceptsPackages;
      this.dontAcceptPackages.setToolTip(CreateLang.translateDirect("gui.package_port.send_only"));
      this.addRenderableWidget(this.dontAcceptPackages);
      this.containerTick();
      this.extraAreas = ImmutableList.of(new Rect2i(x + this.background.getWidth(), y + this.background.getHeight() - 50, 70, 60));
   }

   private int nameBoxX(String s, EditBox nameBox) {
      return this.getGuiLeft() + this.background.getWidth() / 2 - (Math.min(this.font.width(s), nameBox.getWidth()) + 10) / 2;
   }

   @Override
   protected void containerTick() {
      this.acceptPackages.visible = ((PackagePortMenu)this.menu).contentHolder.target != null;
      this.dontAcceptPackages.visible = ((PackagePortMenu)this.menu).contentHolder.target != null;
      super.containerTick();
   }

   protected void renderBg(GuiGraphics graphics, float pPartialTick, int pMouseX, int pMouseY) {
      int x = this.getGuiLeft();
      int y = this.getGuiTop();
      AllGuiTextures header = this.frogMode ? AllGuiTextures.FROGPORT_HEADER : AllGuiTextures.POSTBOX_HEADER;
      header.render(graphics, x, y - header.getHeight());
      this.background.render(graphics, x, y);
      String text = this.addressBox.getValue();
      if (!this.addressBox.isFocused()) {
         if (this.addressBox.getValue().isEmpty()) {
            text = this.icon.getHoverName().getString();
            graphics.drawString(this.font, text, this.nameBoxX(text, this.addressBox), y - 11, 4013128, false);
         }

         AllGuiTextures.FROGPORT_EDIT_NAME.render(graphics, this.nameBoxX(text, this.addressBox) + this.font.width(text) + 5, y - 14);
      }

      ((GuiRenderBuilder)GuiGameElement.of(this.icon).at((float)(x + this.background.getWidth() + 6), (float)(y + this.background.getHeight() - 56), -200.0F))
         .scale(4.0)
         .render(graphics);
      int invX = this.leftPos + 30;
      int invY = this.topPos + 8 + this.imageHeight - AllGuiTextures.PLAYER_INVENTORY.getHeight();
      this.renderPlayerInventory(graphics, invX, invY);
      if (((PackagePortMenu)this.menu).contentHolder.target != null) {
         x += 13;
         y += 58;
         AllGuiTextures.FROGPORT_SLOT.render(graphics, x, y);
         graphics.renderItem(((PackagePortMenu)this.menu).contentHolder.target.getIcon(), x + 1, y + 1);
         if (this.addressBox.isHovered()) {
            graphics.renderComponentTooltip(
               this.font,
               List.of(
                  CreateLang.translate("gui.package_port.catch_packages").color(AbstractSimiWidget.HEADER_RGB).component(),
                  CreateLang.translate("gui.package_port.catch_packages_empty").style(ChatFormatting.GRAY).component(),
                  CreateLang.translate("gui.package_port.catch_packages_wildcard").style(ChatFormatting.GRAY).component()
               ),
               pMouseX,
               pMouseY
            );
         }
      }
   }

   @Override
   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      boolean hitEnter = this.getFocused() instanceof EditBox && (pKeyCode == 257 || pKeyCode == 335);
      if (hitEnter && this.addressBox.isFocused()) {
         this.addressBox.setFocused(false);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   public void removed() {
      CatnipServices.NETWORK
         .sendToServer(
            new PackagePortConfigurationPacket(((PackagePortMenu)this.menu).contentHolder.getBlockPos(), this.addressBox.getValue(), this.acceptPackages.green)
         );
      super.removed();
   }

   @Override
   public List<Rect2i> getExtraAreas() {
      return this.extraAreas;
   }
}
