package com.simibubi.create.content.redstone.thresholdSwitch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.GuiGameElement.GuiRenderBuilder;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.foundation.ui.PonderTagScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ThresholdSwitchScreen extends AbstractSimiScreen {
   private ScrollInput offBelow;
   private ScrollInput onAbove;
   private SelectionScrollInput inStacks;
   private IconButton confirmButton;
   private IconButton flipSignals;
   private final Component invertSignal = CreateLang.translateDirect("gui.threshold_switch.invert_signal");
   private final ItemStack renderedItem = new ItemStack((ItemLike)AllBlocks.THRESHOLD_SWITCH.get());
   private AllGuiTextures background = AllGuiTextures.THRESHOLD_SWITCH;
   private ThresholdSwitchBlockEntity blockEntity;
   private int lastModification;

   public ThresholdSwitchScreen(ThresholdSwitchBlockEntity be) {
      super(CreateLang.translateDirect("gui.threshold_switch.title"));
      this.blockEntity = be;
      this.lastModification = -1;
   }

   protected void init() {
      this.setWindowSize(this.background.getWidth(), this.background.getHeight());
      this.setWindowOffset(-20, 0);
      super.init();
      int x = this.guiLeft;
      int y = this.guiTop;
      this.inStacks = (SelectionScrollInput)new SelectionScrollInput(x + 100, y + 23, 52, 42)
         .forOptions(
            List.of(CreateLang.translateDirect("schedule.condition.threshold.items"), CreateLang.translateDirect("schedule.condition.threshold.stacks"))
         )
         .titled(CreateLang.translateDirect("schedule.condition.threshold.item_measure"))
         .setState(this.blockEntity.inStacks ? 1 : 0);
      this.offBelow = new ScrollInput(x + 48, y + 47, 1, 18)
         .withRange(this.blockEntity.getMinLevel(), this.blockEntity.getMaxLevel() + 1 - this.getValueStep())
         .titled(CreateLang.translateDirect("gui.threshold_switch.lower_threshold"))
         .calling(state -> {
            this.lastModification = 0;
            int valueStep = this.getValueStep();
            if (this.onAbove.getState() / valueStep != 0 || state / valueStep != 0) {
               if (this.onAbove.getState() / valueStep <= state / valueStep) {
                  this.onAbove.setState((state + valueStep) / valueStep * valueStep);
                  this.onAbove.onChanged();
               }
            }
         })
         .withStepFunction(sc -> sc.shift ? 10 * this.getValueStep() : this.getValueStep())
         .setState(this.blockEntity.offWhenBelow);
      this.onAbove = new ScrollInput(x + 48, y + 23, 1, 18)
         .withRange(this.blockEntity.getMinLevel() + this.getValueStep(), this.blockEntity.getMaxLevel() + 1)
         .titled(CreateLang.translateDirect("gui.threshold_switch.upper_threshold"))
         .calling(state -> {
            this.lastModification = 0;
            int valueStep = this.getValueStep();
            if (this.offBelow.getState() / valueStep != 0 || state / valueStep != 0) {
               if (this.offBelow.getState() / valueStep >= state / valueStep) {
                  this.offBelow.setState((state - valueStep) / valueStep * valueStep);
                  this.offBelow.onChanged();
               }
            }
         })
         .withStepFunction(sc -> sc.shift ? 10 * this.getValueStep() : this.getValueStep())
         .setState(this.blockEntity.onWhenAbove);
      this.onAbove.onChanged();
      this.offBelow.onChanged();
      this.addRenderableWidget(this.onAbove);
      this.addRenderableWidget(this.offBelow);
      this.addRenderableWidget(this.inStacks);
      this.confirmButton = new IconButton(x + this.background.getWidth() - 33, y + this.background.getHeight() - 24, AllIcons.I_CONFIRM);
      this.confirmButton.withCallback(() -> this.onClose());
      this.addRenderableWidget(this.confirmButton);
      this.flipSignals = new IconButton(x + this.background.getWidth() - 62, y + this.background.getHeight() - 24, AllIcons.I_FLIP);
      this.flipSignals.withCallback(() -> this.send(!this.blockEntity.isInverted()));
      this.flipSignals.setToolTip(this.invertSignal);
      this.addRenderableWidget(this.flipSignals);
      this.updateInputBoxes();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int pButton) {
      int itemX = this.guiLeft + 13;
      int itemY = this.guiTop + 80;
      if (mouseX >= (double)itemX && mouseX < (double)(itemX + 16) && mouseY >= (double)itemY && mouseY < (double)(itemY + 16)) {
         ScreenOpener.open(new PonderTagScreen(AllCreatePonderTags.THRESHOLD_SWITCH_TARGETS));
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, pButton);
      }
   }

   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      int x = this.guiLeft;
      int y = this.guiTop;
      this.background.render(graphics, x, y);
      graphics.drawString(this.font, this.title, x + this.background.getWidth() / 2 - this.font.width(this.title) / 2, y + 4, 5841956, false);
      ThresholdSwitchBlockEntity.ThresholdType typeOfCurrentTarget = this.blockEntity.getTypeOfCurrentTarget();
      boolean forItems = typeOfCurrentTarget == ThresholdSwitchBlockEntity.ThresholdType.ITEM;
      AllGuiTextures inputBg = forItems ? AllGuiTextures.THRESHOLD_SWITCH_ITEMCOUNT_INPUTS : AllGuiTextures.THRESHOLD_SWITCH_MISC_INPUTS;
      inputBg.render(graphics, x + 44, y + 21);
      inputBg.render(graphics, x + 44, y + 21 + 24);
      int valueStep = 1;
      boolean stacks = this.inStacks.getState() == 1;
      if (typeOfCurrentTarget == ThresholdSwitchBlockEntity.ThresholdType.FLUID) {
         valueStep = 1000;
      }

      if (forItems) {
         Component suffix = this.inStacks.getState() == 0
            ? CreateLang.translateDirect("schedule.condition.threshold.items")
            : CreateLang.translateDirect("schedule.condition.threshold.stacks");
         valueStep = this.inStacks.getState() == 0 ? 1 : 64;
         graphics.drawString(this.font, suffix, x + 105, y + 28, -1, true);
         graphics.drawString(this.font, suffix, x + 105, y + 28 + 24, -1, true);
      }

      graphics.drawString(
         this.font,
         Component.literal(
            "≥ "
               + (
                  typeOfCurrentTarget == ThresholdSwitchBlockEntity.ThresholdType.UNSUPPORTED
                     ? ""
                     : (forItems ? this.onAbove.getState() / valueStep : this.blockEntity.format(this.onAbove.getState() / valueStep, stacks).getString())
               )
         ),
         x + 53,
         y + 28,
         -1,
         true
      );
      graphics.drawString(
         this.font,
         Component.literal(
            "≤ "
               + (
                  typeOfCurrentTarget == ThresholdSwitchBlockEntity.ThresholdType.UNSUPPORTED
                     ? ""
                     : (forItems ? this.offBelow.getState() / valueStep : this.blockEntity.format(this.offBelow.getState() / valueStep, stacks).getString())
               )
         ),
         x + 53,
         y + 28 + 24,
         -1,
         true
      );
      ((GuiRenderBuilder)GuiGameElement.of(this.renderedItem)
            .at((float)(x + this.background.getWidth() + 6), (float)(y + this.background.getHeight() - 56), -200.0F))
         .scale(5.0)
         .render(graphics);
      int itemX = x + 13;
      int itemY = y + 80;
      ItemStack displayItem = this.blockEntity.getDisplayItemForScreen();
      ((GuiRenderBuilder)GuiGameElement.of(displayItem.isEmpty() ? new ItemStack(Items.BARRIER) : displayItem).at((float)itemX, (float)itemY, 0.0F))
         .render(graphics);
      int torchX = x + 23;
      int torchY = y + 24;
      boolean highlightTopRow = this.blockEntity.isInverted() ^ this.blockEntity.isPowered();
      AllGuiTextures.THRESHOLD_SWITCH_CURRENT_STATE.render(graphics, torchX - 3, torchY - 4 + (highlightTopRow ? 0 : 24));
      PoseStack ms = graphics.pose();
      ms.pushPose();
      ms.translate((float)(torchX - 5), (float)(torchY + 14), 200.0F);
      ((PoseTransformStack)TransformStack.of(ms).rotateXDegrees(-22.5F)).rotateYDegrees(45.0F);

      for (boolean power : Iterate.trueAndFalse) {
         GuiGameElement.of((BlockState)Blocks.REDSTONE_TORCH.defaultBlockState().setValue(RedstoneTorchBlock.LIT, this.blockEntity.isInverted() ^ power))
            .scale(20.0)
            .render(graphics);
         ms.translate(0.0F, 26.0F, 0.0F);
      }

      ms.popPose();
      if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
         ArrayList<Component> list = new ArrayList<>();
         if (displayItem.isEmpty()) {
            list.add(CreateLang.translateDirect("gui.threshold_switch.not_attached"));
            list.add(CreateLang.translateDirect("display_link.view_compatible").withStyle(ChatFormatting.DARK_GRAY));
            graphics.renderComponentTooltip(this.font, list, mouseX, mouseY);
         } else {
            list.add(displayItem.getHoverName());
            if (typeOfCurrentTarget == ThresholdSwitchBlockEntity.ThresholdType.UNSUPPORTED) {
               list.add(CreateLang.translateDirect("gui.threshold_switch.incompatible").withStyle(ChatFormatting.GRAY));
               list.add(CreateLang.translateDirect("display_link.view_compatible").withStyle(ChatFormatting.DARK_GRAY));
               graphics.renderComponentTooltip(this.font, list, mouseX, mouseY);
            } else {
               CreateLang.translate("gui.threshold_switch.currently", this.blockEntity.format(this.blockEntity.currentLevel / valueStep, stacks))
                  .style(ChatFormatting.DARK_AQUA)
                  .addTo(list);
               if (this.blockEntity.currentMinLevel / valueStep == 0) {
                  CreateLang.translate("gui.threshold_switch.range_max", this.blockEntity.format(this.blockEntity.currentMaxLevel / valueStep, stacks))
                     .style(ChatFormatting.GRAY)
                     .addTo(list);
               } else {
                  CreateLang.translate(
                        "gui.threshold_switch.range",
                        this.blockEntity.currentMinLevel / valueStep,
                        this.blockEntity.format(this.blockEntity.currentMaxLevel / valueStep, stacks)
                     )
                     .style(ChatFormatting.GRAY)
                     .addTo(list);
               }

               list.add(CreateLang.translateDirect("display_link.view_compatible").withStyle(ChatFormatting.DARK_GRAY));
               graphics.renderComponentTooltip(this.font, list, mouseX, mouseY);
            }
         }
      } else {
         for (boolean power : Iterate.trueAndFalse) {
            int thisTorchY = power ? torchY : torchY + 26;
            if (mouseX >= torchX && mouseX < torchX + 16 && mouseY >= thisTorchY && mouseY < thisTorchY + 16) {
               graphics.renderComponentTooltip(
                  this.font,
                  List.of(
                     CreateLang.translate(power ^ this.blockEntity.isInverted() ? "gui.threshold_switch.power_on_when" : "gui.threshold_switch.power_off_when")
                        .color(AbstractSimiWidget.HEADER_RGB)
                        .component()
                  ),
                  mouseX,
                  mouseY
               );
               return;
            }
         }
      }
   }

   public void tick() {
      super.tick();
      if (this.lastModification >= 0) {
         this.lastModification++;
      }

      if (this.lastModification >= 20) {
         this.lastModification = -1;
         this.send(this.blockEntity.isInverted());
      }

      if (this.inStacks != null) {
         this.updateInputBoxes();
      }
   }

   private void updateInputBoxes() {
      ThresholdSwitchBlockEntity.ThresholdType typeOfCurrentTarget = this.blockEntity.getTypeOfCurrentTarget();
      boolean forItems = typeOfCurrentTarget == ThresholdSwitchBlockEntity.ThresholdType.ITEM;
      int valueStep = this.getValueStep();
      this.inStacks.active = this.inStacks.visible = forItems;
      this.onAbove.setWidth(forItems ? 48 : 103);
      this.offBelow.setWidth(forItems ? 48 : 103);
      this.onAbove.visible = typeOfCurrentTarget != ThresholdSwitchBlockEntity.ThresholdType.UNSUPPORTED;
      this.offBelow.visible = typeOfCurrentTarget != ThresholdSwitchBlockEntity.ThresholdType.UNSUPPORTED;
      int min = this.blockEntity.currentMinLevel + valueStep;
      int max = this.blockEntity.currentMaxLevel;
      this.onAbove.withRange(min, max + 1);
      int roundedState = Mth.clamp(this.onAbove.getState() / valueStep * valueStep, min, max);
      if (roundedState != this.onAbove.getState()) {
         this.onAbove.setState(roundedState);
         this.onAbove.onChanged();
      }

      min = this.blockEntity.currentMinLevel;
      max = this.blockEntity.currentMaxLevel - valueStep;
      this.offBelow.withRange(min, max + 1);
      roundedState = Mth.clamp(this.offBelow.getState() / valueStep * valueStep, min, max);
      if (roundedState != this.offBelow.getState()) {
         this.offBelow.setState(roundedState);
         this.offBelow.onChanged();
      }
   }

   private int getValueStep() {
      boolean stacks = this.inStacks.getState() == 1;
      int valueStep = 1;
      if (this.blockEntity.getTypeOfCurrentTarget() == ThresholdSwitchBlockEntity.ThresholdType.FLUID) {
         valueStep = 1000;
      } else if (stacks) {
         valueStep = 64;
      }

      return valueStep;
   }

   public void removed() {
      this.send(this.blockEntity.isInverted());
   }

   protected void send(boolean invert) {
      CatnipServices.NETWORK
         .sendToServer(
            new ConfigureThresholdSwitchPacket(
               this.blockEntity.getBlockPos(), this.offBelow.getState(), this.onAbove.getState(), invert, this.inStacks.getState() == 1
            )
         );
   }
}
