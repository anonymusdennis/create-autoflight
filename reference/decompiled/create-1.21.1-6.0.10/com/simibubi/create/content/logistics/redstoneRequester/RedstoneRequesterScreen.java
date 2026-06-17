package com.simibubi.create.content.logistics.redstoneRequester;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class RedstoneRequesterScreen extends AbstractSimiContainerScreen<RedstoneRequesterMenu> {
   private AddressEditBox addressBox;
   private IconButton confirmButton;
   private List<Rect2i> extraAreas = Collections.emptyList();
   private List<Integer> amounts = new ArrayList<>();
   private IconButton dontAllowPartial;
   private IconButton allowPartial;

   public RedstoneRequesterScreen(RedstoneRequesterMenu container, Inventory inv, Component title) {
      super(container, inv, title);

      for (int i = 0; i < 9; i++) {
         this.amounts.add(1);
      }

      List<BigItemStack> stacks = ((RedstoneRequesterMenu)this.menu).contentHolder.encodedRequest.stacks();

      for (int i = 0; i < stacks.size(); i++) {
         this.amounts.set(i, Math.max(1, stacks.get(i).count));
      }
   }

   @Override
   protected void containerTick() {
      super.containerTick();
      this.addressBox.tick();

      for (int i = 0; i < this.amounts.size(); i++) {
         if (((RedstoneRequesterMenu)this.menu).ghostInventory.getStackInSlot(i).isEmpty()) {
            this.amounts.set(i, 1);
         }
      }
   }

   @Override
   protected void init() {
      int bgHeight = AllGuiTextures.REDSTONE_REQUESTER.getHeight();
      int bgWidth = AllGuiTextures.REDSTONE_REQUESTER.getWidth();
      this.setWindowSize(bgWidth, bgHeight + AllGuiTextures.PLAYER_INVENTORY.getHeight());
      super.init();
      this.clearWidgets();
      int x = this.getGuiLeft();
      int y = this.getGuiTop();
      if (this.addressBox == null) {
         this.addressBox = new AddressEditBox(this, new NoShadowFontWrapper(this.font), x + 55, y + 68, 110, 10, false);
         this.addressBox.setValue(((RedstoneRequesterMenu)this.menu).contentHolder.encodedTargetAdress);
         this.addressBox.setTextColor(5592405);
      }

      this.addRenderableWidget(this.addressBox);
      this.confirmButton = new IconButton(x + bgWidth - 30, y + bgHeight - 25, AllIcons.I_CONFIRM);
      this.confirmButton.withCallback(() -> this.minecraft.player.closeContainer());
      this.addRenderableWidget(this.confirmButton);
      this.allowPartial = new IconButton(x + 12, y + bgHeight - 25, AllIcons.I_PARTIAL_REQUESTS);
      this.allowPartial.withCallback(() -> {
         this.allowPartial.green = true;
         this.dontAllowPartial.green = false;
      });
      this.allowPartial.green = ((RedstoneRequesterMenu)this.menu).contentHolder.allowPartialRequests;
      this.allowPartial.setToolTip(CreateLang.translate("gui.redstone_requester.allow_partial").component());
      this.addRenderableWidget(this.allowPartial);
      this.dontAllowPartial = new IconButton(x + 12 + 18, y + bgHeight - 25, AllIcons.I_FULL_REQUESTS);
      this.dontAllowPartial.withCallback(() -> {
         this.allowPartial.green = false;
         this.dontAllowPartial.green = true;
      });
      this.dontAllowPartial.green = !((RedstoneRequesterMenu)this.menu).contentHolder.allowPartialRequests;
      this.dontAllowPartial.setToolTip(CreateLang.translate("gui.redstone_requester.dont_allow_partial").component());
      this.addRenderableWidget(this.dontAllowPartial);
      this.extraAreas = List.of(new Rect2i(x + bgWidth, y + bgHeight - 50, 70, 60));
   }

   protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
      int x = this.getGuiLeft();
      int y = this.getGuiTop();
      AllGuiTextures.REDSTONE_REQUESTER.render(pGuiGraphics, x + 3, y);
      this.renderPlayerInventory(pGuiGraphics, x - 3, y + 124);
      ItemStack stack = AllBlocks.REDSTONE_REQUESTER.asStack();
      Component title = CreateLang.text(stack.getHoverName().getString()).component();
      pGuiGraphics.drawString(this.font, title, x + 117 - this.font.width(title) / 2, y + 4, 4013128, false);
      GuiGameElement.of(stack).scale(3.0).render(pGuiGraphics, x + 245, y + 80);
   }

   @Override
   protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderForeground(graphics, mouseX, mouseY, partialTicks);
      int x = this.getGuiLeft();
      int y = this.getGuiTop();

      for (int i = 0; i < this.amounts.size(); i++) {
         int inputX = x + 27 + i * 20;
         int inputY = y + 28;
         ItemStack itemStack = ((RedstoneRequesterMenu)this.menu).ghostInventory.getStackInSlot(i);
         if (!itemStack.isEmpty()) {
            PoseStack ms = graphics.pose();
            ms.pushPose();
            ms.translate(0.0F, 0.0F, 100.0F);
            graphics.renderItemDecorations(this.font, itemStack, inputX, inputY, this.amounts.get(i) + "");
            ms.popPose();
         }
      }

      if (this.addressBox.isHovered() && !this.addressBox.isFocused()) {
         if (this.addressBox.getValue().isBlank()) {
            graphics.renderComponentTooltip(
               this.font,
               List.of(
                  CreateLang.translate("gui.redstone_requester.requester_address").color(ScrollInput.HEADER_RGB).component(),
                  CreateLang.translate("gui.redstone_requester.requester_address_tip").style(ChatFormatting.GRAY).component(),
                  CreateLang.translate("gui.redstone_requester.requester_address_tip_1").style(ChatFormatting.GRAY).component(),
                  CreateLang.translate("gui.schedule.lmb_edit").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component()
               ),
               mouseX,
               mouseY
            );
         } else {
            graphics.renderComponentTooltip(
               this.font,
               List.of(
                  CreateLang.translate("gui.redstone_requester.requester_address_given").color(ScrollInput.HEADER_RGB).component(),
                  CreateLang.text("'" + this.addressBox.getValue() + "'").style(ChatFormatting.GRAY).component()
               ),
               mouseX,
               mouseY
            );
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      int x = this.getGuiLeft();
      int y = this.getGuiTop();
      if (this.addressBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
         return true;
      } else {
         for (int i = 0; i < this.amounts.size(); i++) {
            int inputX = x + 27 + i * 20;
            int inputY = y + 28;
            if (mouseX >= (double)inputX && mouseX < (double)(inputX + 16) && mouseY >= (double)inputY && mouseY < (double)(inputY + 16)) {
               ItemStack itemStack = ((RedstoneRequesterMenu)this.menu).ghostInventory.getStackInSlot(i);
               if (itemStack.isEmpty()) {
                  return true;
               }

               this.amounts.set(i, Mth.clamp((int)((double)this.amounts.get(i).intValue() + Math.signum(scrollY) * (double)(hasShiftDown() ? 10 : 1)), 1, 256));
               return true;
            }
         }

         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   protected List<Component> getTooltipFromContainerItem(ItemStack pStack) {
      List<Component> tooltip = super.getTooltipFromContainerItem(pStack);
      if (!(this.hoveredSlot instanceof SlotItemHandler)) {
         return tooltip;
      } else {
         int slotIndex = this.hoveredSlot.getSlotIndex();
         return slotIndex >= this.amounts.size()
            ? tooltip
            : List.of(
               CreateLang.translate("gui.factory_panel.send_item", CreateLang.itemName(pStack).add(CreateLang.text(" x" + this.amounts.get(slotIndex))))
                  .color(ScrollInput.HEADER_RGB)
                  .component(),
               CreateLang.translate("gui.factory_panel.scroll_to_change_amount").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component(),
               CreateLang.translate("gui.scrollInput.shiftScrollsFaster").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component()
            );
      }
   }

   @Override
   public List<Rect2i> getExtraAreas() {
      return this.extraAreas;
   }

   public void removed() {
      CatnipServices.NETWORK
         .sendToServer(
            new RedstoneRequesterConfigurationPacket(
               ((RedstoneRequesterMenu)this.menu).contentHolder.getBlockPos(), this.addressBox.getValue(), this.allowPartial.green, this.amounts
            )
         );
      super.removed();
   }
}
