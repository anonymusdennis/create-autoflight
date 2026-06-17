package com.simibubi.create.content.schematics.cannon;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.crate.CreativeCrateBlock;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.GuiGameElement.GuiRenderBuilder;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class SchematicannonScreen extends AbstractSimiContainerScreen<SchematicannonMenu> {
   private static final AllGuiTextures BG_BOTTOM = AllGuiTextures.SCHEMATICANNON_BOTTOM;
   private static final AllGuiTextures BG_TOP = AllGuiTextures.SCHEMATICANNON_TOP;
   private final Component listPrinter = CreateLang.translateDirect("gui.schematicannon.listPrinter");
   private final String _gunpowderLevel = "gui.schematicannon.gunpowderLevel";
   private final String _shotsRemaining = "gui.schematicannon.shotsRemaining";
   private final String _showSettings = "gui.schematicannon.showOptions";
   private final String _shotsRemainingWithBackup = "gui.schematicannon.shotsRemainingWithBackup";
   private final String _slotGunpowder = "gui.schematicannon.slot.gunpowder";
   private final String _slotListPrinter = "gui.schematicannon.slot.listPrinter";
   private final String _slotSchematic = "gui.schematicannon.slot.schematic";
   private final Component optionEnabled = CreateLang.translateDirect("gui.schematicannon.optionEnabled");
   private final Component optionDisabled = CreateLang.translateDirect("gui.schematicannon.optionDisabled");
   protected List<Indicator> replaceLevelIndicators;
   protected List<IconButton> replaceLevelButtons;
   protected IconButton skipMissingButton;
   protected Indicator skipMissingIndicator;
   protected IconButton skipBlockEntitiesButton;
   protected Indicator skipBlockEntitiesIndicator;
   protected IconButton playButton;
   protected Indicator playIndicator;
   protected IconButton pauseButton;
   protected Indicator pauseIndicator;
   protected IconButton resetButton;
   protected Indicator resetIndicator;
   private IconButton confirmButton;
   private IconButton showSettingsButton;
   private Indicator showSettingsIndicator;
   protected List<AbstractWidget> placementSettingWidgets;
   private final ItemStack renderedItem = AllBlocks.SCHEMATICANNON.asStack();
   private List<Rect2i> extraAreas = Collections.emptyList();

   public SchematicannonScreen(SchematicannonMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.placementSettingWidgets = new ArrayList<>();
   }

   @Override
   protected void init() {
      this.setWindowSize(BG_TOP.getWidth(), BG_TOP.getHeight() + BG_BOTTOM.getHeight() + 2 + AllGuiTextures.PLAYER_INVENTORY.getHeight());
      this.setWindowOffset(-11, 0);
      super.init();
      int x = this.leftPos;
      int y = this.topPos;
      this.playButton = new IconButton(x + 75, y + 85, AllIcons.I_PLAY);
      this.playButton.withCallback(() -> this.sendOptionUpdate(ConfigureSchematicannonPacket.Option.PLAY, true));
      this.playIndicator = new Indicator(x + 75, y + 79, CommonComponents.EMPTY);
      this.pauseButton = new IconButton(x + 93, y + 85, AllIcons.I_PAUSE);
      this.pauseButton.withCallback(() -> this.sendOptionUpdate(ConfigureSchematicannonPacket.Option.PAUSE, true));
      this.pauseIndicator = new Indicator(x + 93, y + 79, CommonComponents.EMPTY);
      this.resetButton = new IconButton(x + 111, y + 85, AllIcons.I_STOP);
      this.resetButton.withCallback(() -> this.sendOptionUpdate(ConfigureSchematicannonPacket.Option.STOP, true));
      this.resetIndicator = new Indicator(x + 111, y + 79, CommonComponents.EMPTY);
      this.resetIndicator.state = Indicator.State.RED;
      this.addRenderableWidgets(
         new AbstractSimiWidget[]{this.playButton, this.playIndicator, this.pauseButton, this.pauseIndicator, this.resetButton, this.resetIndicator}
      );
      this.confirmButton = new IconButton(x + 180, y + 111, AllIcons.I_CONFIRM);
      this.confirmButton.withCallback(() -> this.minecraft.player.closeContainer());
      this.addRenderableWidget(this.confirmButton);
      this.showSettingsButton = new IconButton(x + 8, y + 111, AllIcons.I_PLACEMENT_SETTINGS);
      this.showSettingsButton.withCallback(() -> {
         this.showSettingsIndicator.state = this.placementSettingsHidden() ? Indicator.State.GREEN : Indicator.State.OFF;
         this.initPlacementSettings();
      });
      this.showSettingsButton.setToolTip(CreateLang.translateDirect("gui.schematicannon.showOptions"));
      this.addRenderableWidget(this.showSettingsButton);
      this.showSettingsIndicator = new Indicator(x + 9, y + 111, CommonComponents.EMPTY);
      this.extraAreas = ImmutableList.of(new Rect2i(x + BG_TOP.getWidth(), y + BG_TOP.getHeight() + BG_BOTTOM.getHeight() - 62, 84, 92));
      this.tick();
   }

   private void initPlacementSettings() {
      this.removeWidgets(this.placementSettingWidgets);
      this.placementSettingWidgets.clear();
      if (!this.placementSettingsHidden()) {
         int x = this.leftPos;
         int y = this.topPos;
         this.replaceLevelButtons = new ArrayList<>(4);
         this.replaceLevelIndicators = new ArrayList<>(4);
         List<AllIcons> icons = ImmutableList.of(AllIcons.I_DONT_REPLACE, AllIcons.I_REPLACE_SOLID, AllIcons.I_REPLACE_ANY, AllIcons.I_REPLACE_EMPTY);
         List<Component> toolTips = ImmutableList.of(
            CreateLang.translateDirect("gui.schematicannon.option.dontReplaceSolid"),
            CreateLang.translateDirect("gui.schematicannon.option.replaceWithSolid"),
            CreateLang.translateDirect("gui.schematicannon.option.replaceWithAny"),
            CreateLang.translateDirect("gui.schematicannon.option.replaceWithEmpty")
         );

         for (int i = 0; i < 4; i++) {
            this.replaceLevelIndicators.add(new Indicator(x + 33 + i * 18, y + 111, CommonComponents.EMPTY));
            IconButton replaceLevelButton = new IconButton(x + 33 + i * 18, y + 111, icons.get(i));
            int replaceMode = i;
            replaceLevelButton.withCallback(() -> {
               if (((SchematicannonMenu)this.menu).contentHolder.replaceMode != replaceMode) {
                  this.sendOptionUpdate(ConfigureSchematicannonPacket.Option.values()[replaceMode], true);
               }
            });
            replaceLevelButton.setToolTip(toolTips.get(i));
            this.replaceLevelButtons.add(replaceLevelButton);
         }

         this.placementSettingWidgets.addAll(this.replaceLevelButtons);
         this.skipMissingButton = new IconButton(x + 111, y + 111, AllIcons.I_SKIP_MISSING);
         this.skipMissingButton
            .withCallback(
               () -> this.sendOptionUpdate(ConfigureSchematicannonPacket.Option.SKIP_MISSING, !((SchematicannonMenu)this.menu).contentHolder.skipMissing)
            );
         this.skipMissingButton.setToolTip(CreateLang.translateDirect("gui.schematicannon.option.skipMissing"));
         this.skipMissingIndicator = new Indicator(x + 111, y + 111, CommonComponents.EMPTY);
         Collections.addAll(this.placementSettingWidgets, this.skipMissingButton);
         this.skipBlockEntitiesButton = new IconButton(x + 135, y + 111, AllIcons.I_SKIP_BLOCK_ENTITIES);
         this.skipBlockEntitiesButton
            .withCallback(
               () -> this.sendOptionUpdate(
                     ConfigureSchematicannonPacket.Option.SKIP_BLOCK_ENTITIES, !((SchematicannonMenu)this.menu).contentHolder.replaceBlockEntities
                  )
            );
         this.skipBlockEntitiesButton.setToolTip(CreateLang.translateDirect("gui.schematicannon.option.skipBlockEntities"));
         this.skipBlockEntitiesIndicator = new Indicator(x + 129, y + 111, CommonComponents.EMPTY);
         Collections.addAll(this.placementSettingWidgets, this.skipBlockEntitiesButton);
         this.addRenderableWidgets(this.placementSettingWidgets);
      }
   }

   protected boolean placementSettingsHidden() {
      return this.showSettingsIndicator.state == Indicator.State.OFF;
   }

   @Override
   protected void containerTick() {
      super.containerTick();
      SchematicannonBlockEntity be = ((SchematicannonMenu)this.menu).contentHolder;
      if (!this.placementSettingsHidden()) {
         for (int replaceMode = 0; replaceMode < this.replaceLevelButtons.size(); replaceMode++) {
            this.replaceLevelButtons.get(replaceMode).green = replaceMode == be.replaceMode;
            this.replaceLevelIndicators.get(replaceMode).state = replaceMode == be.replaceMode ? Indicator.State.ON : Indicator.State.OFF;
         }

         this.skipMissingButton.green = be.skipMissing;
         this.skipBlockEntitiesButton.green = !be.replaceBlockEntities;
      }

      this.playIndicator.state = Indicator.State.OFF;
      this.pauseIndicator.state = Indicator.State.OFF;
      this.resetIndicator.state = Indicator.State.OFF;
      switch (be.state) {
         case PAUSED:
            this.pauseIndicator.state = Indicator.State.YELLOW;
            this.playButton.active = true;
            this.pauseButton.active = false;
            this.resetButton.active = true;
            break;
         case RUNNING:
            this.playIndicator.state = Indicator.State.GREEN;
            this.playButton.active = false;
            this.pauseButton.active = true;
            this.resetButton.active = true;
            break;
         case STOPPED:
            this.resetIndicator.state = Indicator.State.RED;
            this.playButton.active = true;
            this.pauseButton.active = false;
            this.resetButton.active = false;
      }

      this.handleTooltips();
   }

   protected void handleTooltips() {
      if (!this.placementSettingsHidden()) {
         for (AbstractWidget w : this.placementSettingWidgets) {
            if (w instanceof IconButton) {
               IconButton button = (IconButton)w;
               if (!button.getToolTip().isEmpty()) {
                  button.setToolTip((Component)button.getToolTip().get(0));
                  button.getToolTip().add(TooltipHelper.holdShift(Palette.BLUE, hasShiftDown()));
               }
            }
         }

         if (hasShiftDown()) {
            this.fillToolTip(this.skipMissingButton, this.skipMissingIndicator, "skipMissing");
            this.fillToolTip(this.skipBlockEntitiesButton, this.skipBlockEntitiesIndicator, "skipBlockEntities");
            this.fillToolTip(this.replaceLevelButtons.get(0), this.replaceLevelIndicators.get(0), "dontReplaceSolid");
            this.fillToolTip(this.replaceLevelButtons.get(1), this.replaceLevelIndicators.get(1), "replaceWithSolid");
            this.fillToolTip(this.replaceLevelButtons.get(2), this.replaceLevelIndicators.get(2), "replaceWithAny");
            this.fillToolTip(this.replaceLevelButtons.get(3), this.replaceLevelIndicators.get(3), "replaceWithEmpty");
         }
      }
   }

   private void fillToolTip(IconButton button, Indicator indicator, String tooltipKey) {
      if (button.isHovered()) {
         boolean enabled = button.green;
         List<Component> tip = button.getToolTip();
         tip.add((enabled ? this.optionEnabled : this.optionDisabled).plainCopy().withStyle(enabled ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
         tip.addAll(TooltipHelper.cutTextComponent(CreateLang.translateDirect("gui.schematicannon.option." + tooltipKey + ".description"), Palette.ALL_GRAY));
      }
   }

   protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
      int invX = this.getLeftOfCentered(AllGuiTextures.PLAYER_INVENTORY.getWidth());
      int invY = this.topPos + BG_TOP.getHeight() + BG_BOTTOM.getHeight() + 2;
      this.renderPlayerInventory(graphics, invX, invY);
      int x = this.leftPos;
      int y = this.topPos;
      BG_TOP.render(graphics, x, y);
      BG_BOTTOM.render(graphics, x, y + BG_TOP.getHeight());
      AllGuiTextures.SCHEMATIC_TITLE.render(graphics, x, y - 2);
      SchematicannonBlockEntity be = ((SchematicannonMenu)this.menu).contentHolder;
      this.renderPrintingProgress(graphics, x, y, be.schematicProgress);
      float amount = (float)be.remainingFuel / (float)be.getShotsPerGunpowder();
      this.renderFuelBar(graphics, x, y, amount);
      this.renderChecklistPrinterProgress(graphics, x, y, be.bookPrintingProgress);
      if (!be.inventory.getStackInSlot(0).isEmpty()) {
         this.renderBlueprintHighlight(graphics, x, y);
      }

      ((GuiRenderBuilder)GuiGameElement.of(this.renderedItem)
            .at((float)(x + BG_TOP.getWidth()), (float)(y + BG_TOP.getHeight() + BG_BOTTOM.getHeight() - 48), -200.0F))
         .scale(5.0)
         .render(graphics);
      graphics.drawString(this.font, this.title, x + (BG_TOP.getWidth() - 8 - this.font.width(this.title)) / 2, y + 2, 5263440, false);
      Component msg = CreateLang.translateDirect("schematicannon.status." + be.statusMsg);
      int stringWidth = this.font.width(msg);
      if (be.missingItem != null) {
         stringWidth += 16;
         ((GuiRenderBuilder)GuiGameElement.of(be.missingItem).at((float)(x + 128), (float)(y + 49), 100.0F)).scale(1.0).render(graphics);
      }

      graphics.drawString(this.font, msg, x + 103 - stringWidth / 2, y + 53, 14544639);
      if ("schematicErrored".equals(be.statusMsg)) {
         graphics.drawString(
            this.font, CreateLang.translateDirect("schematicannon.status.schematicErroredCheckLogs"), x + 103 - stringWidth / 2, y + 65, 14544639
         );
      }
   }

   protected void renderBlueprintHighlight(GuiGraphics graphics, int x, int y) {
      AllGuiTextures.SCHEMATICANNON_HIGHLIGHT.render(graphics, x + 10, y + 60);
   }

   protected void renderPrintingProgress(GuiGraphics graphics, int x, int y, float progress) {
      progress = Math.min(progress, 1.0F);
      AllGuiTextures sprite = AllGuiTextures.SCHEMATICANNON_PROGRESS;
      graphics.blit(sprite.location, x + 44, y + 64, sprite.getStartX(), sprite.getStartY(), (int)((float)sprite.getWidth() * progress), sprite.getHeight());
   }

   protected void renderChecklistPrinterProgress(GuiGraphics graphics, int x, int y, float progress) {
      AllGuiTextures sprite = AllGuiTextures.SCHEMATICANNON_CHECKLIST_PROGRESS;
      graphics.blit(sprite.location, x + 154, y + 20, sprite.getStartX(), sprite.getStartY(), (int)((float)sprite.getWidth() * progress), sprite.getHeight());
   }

   protected void renderFuelBar(GuiGraphics graphics, int x, int y, float amount) {
      AllGuiTextures sprite = AllGuiTextures.SCHEMATICANNON_FUEL;
      if (((SchematicannonMenu)this.menu).contentHolder.hasCreativeCrate) {
         AllGuiTextures.SCHEMATICANNON_FUEL_CREATIVE.render(graphics, x + 36, y + 19);
      } else {
         graphics.blit(sprite.location, x + 36, y + 19, sprite.getStartX(), sprite.getStartY(), (int)((float)sprite.getWidth() * amount), sprite.getHeight());
      }
   }

   @Override
   protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      SchematicannonBlockEntity be = ((SchematicannonMenu)this.menu).contentHolder;
      int x = this.leftPos;
      int y = this.topPos;
      int fuelX = x + 36;
      int fuelY = y + 19;
      if (mouseX >= fuelX
         && mouseY >= fuelY
         && mouseX <= fuelX + AllGuiTextures.SCHEMATICANNON_FUEL.getWidth()
         && mouseY <= fuelY + AllGuiTextures.SCHEMATICANNON_FUEL.getHeight()) {
         List<Component> tooltip = this.getFuelLevelTooltip(be);
         graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
      }

      if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
         if (this.hoveredSlot.index == 0) {
            graphics.renderComponentTooltip(
               this.font,
               TooltipHelper.cutTextComponent(CreateLang.translateDirect("gui.schematicannon.slot.schematic"), Palette.GRAY_AND_BLUE),
               mouseX,
               mouseY
            );
         }

         if (this.hoveredSlot.index == 2) {
            graphics.renderComponentTooltip(
               this.font,
               TooltipHelper.cutTextComponent(CreateLang.translateDirect("gui.schematicannon.slot.listPrinter"), Palette.GRAY_AND_BLUE),
               mouseX,
               mouseY
            );
         }

         if (this.hoveredSlot.index == 4) {
            graphics.renderComponentTooltip(
               this.font,
               TooltipHelper.cutTextComponent(CreateLang.translateDirect("gui.schematicannon.slot.gunpowder"), Palette.GRAY_AND_BLUE),
               mouseX,
               mouseY
            );
         }
      }

      if (be.missingItem != null) {
         int missingBlockX = x + 128;
         int missingBlockY = y + 49;
         if (mouseX >= missingBlockX && mouseY >= missingBlockY && mouseX <= missingBlockX + 16 && mouseY <= missingBlockY + 16) {
            graphics.renderTooltip(this.font, be.missingItem, mouseX, mouseY);
         }
      }

      int paperX = x + 112;
      int paperY = y + 19;
      if (mouseX >= paperX && mouseY >= paperY && mouseX <= paperX + 16 && mouseY <= paperY + 16) {
         graphics.renderTooltip(this.font, this.listPrinter, mouseX, mouseY);
      }

      super.renderForeground(graphics, mouseX, mouseY, partialTicks);
   }

   protected List<Component> getFuelLevelTooltip(SchematicannonBlockEntity be) {
      int shotsLeft = be.remainingFuel;
      int shotsLeftWithItems = shotsLeft + be.inventory.getStackInSlot(4).getCount() * be.getShotsPerGunpowder();
      List<Component> tooltip = new ArrayList<>();
      if (be.hasCreativeCrate) {
         tooltip.add(CreateLang.translateDirect("gui.schematicannon.gunpowderLevel", "100"));
         tooltip.add(
            Component.literal("(").append(((CreativeCrateBlock)AllBlocks.CREATIVE_CRATE.get()).getName()).append(")").withStyle(ChatFormatting.DARK_PURPLE)
         );
         return tooltip;
      } else {
         int fillPercent = (int)((float)be.remainingFuel / (float)be.getShotsPerGunpowder() * 100.0F);
         tooltip.add(CreateLang.translateDirect("gui.schematicannon.gunpowderLevel", fillPercent));
         tooltip.add(
            CreateLang.translateDirect("gui.schematicannon.shotsRemaining", Component.literal(Integer.toString(shotsLeft)).withStyle(ChatFormatting.BLUE))
               .withStyle(ChatFormatting.GRAY)
         );
         if (shotsLeftWithItems != shotsLeft) {
            tooltip.add(
               CreateLang.translateDirect(
                     "gui.schematicannon.shotsRemainingWithBackup", Component.literal(Integer.toString(shotsLeftWithItems)).withStyle(ChatFormatting.BLUE)
                  )
                  .withStyle(ChatFormatting.GRAY)
            );
         }

         return tooltip;
      }
   }

   protected void sendOptionUpdate(ConfigureSchematicannonPacket.Option option, boolean set) {
      CatnipServices.NETWORK.sendToServer(new ConfigureSchematicannonPacket(option, set));
   }

   @Override
   public List<Rect2i> getExtraAreas() {
      return this.extraAreas;
   }
}
