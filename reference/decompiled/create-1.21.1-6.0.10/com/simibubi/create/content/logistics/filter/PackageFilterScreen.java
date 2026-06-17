package com.simibubi.create.content.logistics.filter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PackageFilterScreen extends AbstractFilterScreen<PackageFilterMenu> {
   private AddressEditBox addressBox;
   private boolean deferFocus;

   public PackageFilterScreen(PackageFilterMenu menu, Inventory inv, Component title) {
      super(menu, inv, title, AllGuiTextures.PACKAGE_FILTER);
   }

   @Override
   protected void containerTick() {
      super.containerTick();
      if (this.deferFocus) {
         this.deferFocus = false;
         this.setFocused(this.addressBox);
      }

      this.addressBox.tick();
   }

   @Override
   protected void init() {
      this.setWindowOffset(-11, 7);
      super.init();
      int x = this.leftPos;
      int y = this.topPos;
      this.addressBox = new AddressEditBox(this, this.font, x + 44, y + 28, 129, 9, false);
      this.addressBox.setTextColor(16777215);
      this.addressBox.setValue(((PackageFilterMenu)this.menu).address);
      this.addressBox.setResponder(this::onAddressEdited);
      this.addRenderableWidget(this.addressBox);
      this.setFocused(this.addressBox);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.render(graphics, mouseX, mouseY, partialTicks);
      PoseStack ms = graphics.pose();
      ms.pushPose();
      ms.translate((float)(this.leftPos + 16), (float)(this.topPos + 23), 0.0F);
      GuiGameElement.of(PackageStyles.getDefaultBox()).render(graphics);
      ms.popPose();
   }

   public void onAddressEdited(String s) {
      ((PackageFilterMenu)this.menu).address = s;
      CompoundTag tag = new CompoundTag();
      tag.putString("Address", s);
      CatnipServices.NETWORK.sendToServer(new FilterScreenPacket(FilterScreenPacket.Option.UPDATE_ADDRESS, tag));
   }

   @Override
   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      return super.mouseClicked(pMouseX, pMouseY, pButton);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      return this.addressBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY) ? true : super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   @Override
   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 257) {
         this.setFocused(null);
      }

      return super.keyPressed(pKeyCode, pScanCode, pModifiers);
   }

   public boolean charTyped(char pCodePoint, int pModifiers) {
      return super.charTyped(pCodePoint, pModifiers);
   }

   @Override
   protected void contentsCleared() {
      this.addressBox.setValue("");
      this.deferFocus = true;
   }

   @Override
   protected boolean isButtonEnabled(IconButton button) {
      return false;
   }

   @Override
   protected int getTitleColor() {
      return 4013128;
   }
}
