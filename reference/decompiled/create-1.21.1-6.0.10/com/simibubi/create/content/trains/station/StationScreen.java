package com.simibubi.create.content.trains.station;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.decoration.slidingDoor.DoorControl;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainIconType;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.Consumer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

public class StationScreen extends AbstractStationScreen {
   private EditBox nameBox;
   private EditBox trainNameBox;
   private IconButton newTrainButton;
   private IconButton disassembleTrainButton;
   private IconButton dropScheduleButton;
   private int leavingAnimation;
   private LerpedFloat trainPosition;
   private DoorControl doorControl;
   private ScrollInput colorTypeScroll;
   private int messedWithColors;
   private boolean switchingToAssemblyMode;

   public StationScreen(StationBlockEntity be, GlobalStation station) {
      super(be, station);
      this.background = AllGuiTextures.STATION;
      this.leavingAnimation = 0;
      this.trainPosition = LerpedFloat.linear().startWithValue(0.0);
      this.switchingToAssemblyMode = false;
      this.doorControl = be.doorControls.mode;
   }

   @Override
   protected void init() {
      super.init();
      int x = this.guiLeft;
      int y = this.guiTop;
      Consumer<String> onTextChanged = s -> this.nameBox.setX(this.nameBoxX(s, this.nameBox));
      this.nameBox = new EditBox(new NoShadowFontWrapper(this.font), x + 23, y + 4, this.background.getWidth() - 20, 10, Component.literal(this.station.name));
      this.nameBox.setBordered(false);
      this.nameBox.setMaxLength(25);
      this.nameBox.setTextColor(5841956);
      this.nameBox.setValue(this.station.name);
      this.nameBox.setFocused(false);
      this.nameBox.mouseClicked(0.0, 0.0, 0);
      this.nameBox.setResponder(onTextChanged);
      this.nameBox.setX(this.nameBoxX(this.nameBox.getValue(), this.nameBox));
      this.addRenderableWidget(this.nameBox);
      Runnable assemblyCallback = () -> {
         this.switchingToAssemblyMode = true;
         this.minecraft.setScreen(new AssemblyScreen(this.blockEntity, this.station));
      };
      this.newTrainButton = new WideIconButton(x + 84, y + 65, AllGuiTextures.I_NEW_TRAIN);
      this.newTrainButton.withCallback(assemblyCallback);
      this.addRenderableWidget(this.newTrainButton);
      this.disassembleTrainButton = new WideIconButton(x + 94, y + 65, AllGuiTextures.I_DISASSEMBLE_TRAIN);
      this.disassembleTrainButton.active = false;
      this.disassembleTrainButton.visible = false;
      this.disassembleTrainButton.withCallback(assemblyCallback);
      this.addRenderableWidget(this.disassembleTrainButton);
      this.dropScheduleButton = new IconButton(x + 73, y + 65, AllIcons.I_VIEW_SCHEDULE);
      this.dropScheduleButton.active = false;
      this.dropScheduleButton.visible = false;
      this.dropScheduleButton.withCallback(() -> CatnipServices.NETWORK.sendToServer(StationEditPacket.dropSchedule(this.blockEntity.getBlockPos())));
      this.addRenderableWidget(this.dropScheduleButton);
      this.colorTypeScroll = new ScrollInput(x + 166, y + 17, 22, 14).titled(CreateLang.translateDirect("station.train_map_color"));
      this.colorTypeScroll.withRange(0, 16);
      this.colorTypeScroll.withStepFunction(ctx -> this.colorTypeScroll.standardStep().apply(ctx));
      this.colorTypeScroll.calling(s -> {
         Train train = this.displayedTrain.get();
         if (train != null) {
            train.mapColorIndex = s;
            this.messedWithColors = 10;
         }
      });
      this.colorTypeScroll.active = this.colorTypeScroll.visible = false;
      this.addRenderableWidget(this.colorTypeScroll);
      onTextChanged = s -> this.trainNameBox.setX(this.nameBoxX(s, this.trainNameBox));
      this.trainNameBox = new EditBox(this.font, x + 23, y + 47, this.background.getWidth() - 75, 10, CommonComponents.EMPTY);
      this.trainNameBox.setBordered(false);
      this.trainNameBox.setMaxLength(35);
      this.trainNameBox.setTextColor(13027014);
      this.trainNameBox.setFocused(false);
      this.trainNameBox.mouseClicked(0.0, 0.0, 0);
      this.trainNameBox.setResponder(onTextChanged);
      this.trainNameBox.active = false;
      this.tickTrainDisplay();
      Pair<ScrollInput, Label> doorControlWidgets = DoorControl.createWidget(x + 35, y + 102, mode -> this.doorControl = mode, this.doorControl);
      this.addRenderableWidget((ScrollInput)doorControlWidgets.getFirst());
      this.addRenderableWidget((Label)doorControlWidgets.getSecond());
   }

   @Override
   public void tick() {
      this.tickTrainDisplay();
      if (this.getFocused() != this.nameBox) {
         this.nameBox.setCursorPosition(this.nameBox.getValue().length());
         this.nameBox.setHighlightPos(this.nameBox.getCursorPosition());
      }

      if (this.getFocused() != this.trainNameBox || !this.trainNameBox.active) {
         this.trainNameBox.setCursorPosition(this.trainNameBox.getValue().length());
         this.trainNameBox.setHighlightPos(this.trainNameBox.getCursorPosition());
      }

      if (this.messedWithColors > 0) {
         this.messedWithColors--;
         if (this.messedWithColors == 0) {
            this.syncTrainNameAndColor();
         }
      }

      super.tick();
      this.updateAssemblyTooltip(
         this.blockEntity.edgePoint.isOnCurve()
            ? "no_assembly_curve"
            : (
               !this.blockEntity.edgePoint.isOrthogonal()
                  ? "no_assembly_diagonal"
                  : (this.trainPresent() && !this.blockEntity.trainCanDisassemble ? "train_not_aligned" : null)
            )
      );
   }

   private void tickTrainDisplay() {
      Train train = this.displayedTrain.get();
      if (train == null) {
         if (this.trainNameBox.active) {
            this.trainNameBox.active = false;
            this.removeWidget(this.trainNameBox);
         }

         this.leavingAnimation = 0;
         this.newTrainButton.active = this.blockEntity.edgePoint.isOrthogonal();
         this.newTrainButton.visible = true;
         this.colorTypeScroll.visible = false;
         this.colorTypeScroll.active = false;
         Train imminentTrain = this.getImminent();
         if (imminentTrain != null) {
            this.displayedTrain = new WeakReference<>(imminentTrain);
            this.newTrainButton.active = false;
            this.newTrainButton.visible = false;
            this.disassembleTrainButton.active = false;
            this.disassembleTrainButton.visible = true;
            this.dropScheduleButton.active = this.blockEntity.trainHasSchedule;
            this.dropScheduleButton.visible = true;
            if (this.mapModsPresent()) {
               this.colorTypeScroll.setState(imminentTrain.mapColorIndex);
               this.colorTypeScroll.visible = true;
               this.colorTypeScroll.active = true;
            }

            this.trainNameBox.active = true;
            this.trainNameBox.setValue(imminentTrain.name.getString());
            this.trainNameBox.setX(this.nameBoxX(this.trainNameBox.getValue(), this.trainNameBox));
            this.addRenderableWidget(this.trainNameBox);
            int trainIconWidth = this.getTrainIconWidth(imminentTrain);
            int targetPos = this.background.getWidth() / 2 - trainIconWidth / 2;
            if (trainIconWidth > 130) {
               targetPos -= trainIconWidth - 130;
            }

            float f = (float)(imminentTrain.navigation.distanceToDestination / 15.0);
            if (this.trainPresent()) {
               f = 0.0F;
            }

            this.trainPosition.startWithValue((double)((float)targetPos - (float)(targetPos + 5) * f));
         }
      } else {
         int trainIconWidthx = this.getTrainIconWidth(train);
         int targetPosx = this.background.getWidth() / 2 - trainIconWidthx / 2;
         if (trainIconWidthx > 130) {
            targetPosx -= trainIconWidthx - 130;
         }

         if (this.leavingAnimation > 0) {
            this.colorTypeScroll.visible = false;
            this.colorTypeScroll.active = false;
            this.disassembleTrainButton.active = false;
            float f = 1.0F - (float)this.leavingAnimation / 80.0F;
            this.trainPosition.setValue((double)((float)targetPosx + f * f * f * (float)(this.background.getWidth() - targetPosx + 5)));
            this.leavingAnimation--;
            if (this.leavingAnimation <= 0) {
               this.displayedTrain = new WeakReference<>(null);
               this.disassembleTrainButton.visible = false;
               this.dropScheduleButton.active = false;
               this.dropScheduleButton.visible = false;
            }
         } else if (this.getImminent() != train) {
            this.leavingAnimation = 80;
         } else {
            boolean trainAtStation = this.trainPresent();
            this.disassembleTrainButton.active = trainAtStation && this.blockEntity.trainCanDisassemble && this.blockEntity.edgePoint.isOrthogonal();
            this.dropScheduleButton.active = this.blockEntity.trainHasSchedule;
            if (this.blockEntity.trainHasSchedule) {
               this.dropScheduleButton
                  .setToolTip(CreateLang.translateDirect(this.blockEntity.trainHasAutoSchedule ? "station.remove_auto_schedule" : "station.remove_schedule"));
            } else {
               this.dropScheduleButton.getToolTip().clear();
            }

            float f = trainAtStation ? 0.0F : (float)(train.navigation.distanceToDestination / 30.0);
            this.trainPosition.setValue((double)((float)targetPosx - (float)(targetPosx + trainIconWidthx) * f));
         }
      }
   }

   private int nameBoxX(String s, EditBox nameBox) {
      return this.guiLeft + this.background.getWidth() / 2 - (Math.min(this.font.width(s), nameBox.getWidth()) + 10) / 2;
   }

   private void updateAssemblyTooltip(String key) {
      if (key == null) {
         this.disassembleTrainButton.setToolTip(CreateLang.translateDirect("station.disassemble_train"));
         this.newTrainButton.setToolTip(CreateLang.translateDirect("station.create_train"));
      } else {
         for (IconButton ib : new IconButton[]{this.disassembleTrainButton, this.newTrainButton}) {
            List<Component> toolTip = ib.getToolTip();
            toolTip.clear();
            toolTip.add(CreateLang.translateDirect("station." + key).withStyle(ChatFormatting.GRAY));
            toolTip.add(CreateLang.translateDirect("station." + key + "_1").withStyle(ChatFormatting.GRAY));
         }
      }
   }

   @Override
   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWindow(graphics, mouseX, mouseY, partialTicks);
      int x = this.guiLeft;
      int y = this.guiTop;
      String text = this.nameBox.getValue();
      if (!this.nameBox.isFocused()) {
         AllGuiTextures.STATION_EDIT_NAME.render(graphics, this.nameBoxX(text, this.nameBox) + this.font.width(text) + 5, y + 1);
      }

      graphics.renderItem(AllBlocks.TRAIN_DOOR.asStack(), x + 14, y + 103);
      Train train = this.displayedTrain.get();
      if (train == null) {
         MutableComponent header = CreateLang.translateDirect("station.idle");
         graphics.drawString(this.font, header, x + 97 - this.font.width(header) / 2, y + 47, 8026746, false);
      } else {
         float position = this.trainPosition.getValue(partialTicks);
         PoseStack ms = graphics.pose();
         ms.pushPose();
         RenderSystem.enableBlend();
         ms.translate(position, 0.0F, 0.0F);
         TrainIconType icon = train.icon;
         int offset = 0;
         List<Carriage> carriages = train.carriages;

         for (int i = carriages.size() - 1; i > 0; i--) {
            RenderSystem.setShaderColor(
               1.0F,
               1.0F,
               1.0F,
               Math.min(
                  1.0F, Math.min((position + (float)offset - 10.0F) / 30.0F, ((float)(this.background.getWidth() - 40) - position - (float)offset) / 30.0F)
               )
            );
            Carriage carriage = carriages.get(this.blockEntity.trainBackwards ? carriages.size() - i - 1 : i);
            offset += icon.render(carriage.bogeySpacing, graphics, x + offset, y + 20) + 1;
         }

         RenderSystem.setShaderColor(
            1.0F,
            1.0F,
            1.0F,
            Math.min(1.0F, Math.min((position + (float)offset - 10.0F) / 30.0F, ((float)(this.background.getWidth() - 40) - position - (float)offset) / 30.0F))
         );
         offset += icon.render(-1, graphics, x + offset, y + 20);
         RenderSystem.disableBlend();
         ms.popPose();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         AllGuiTextures.STATION_TEXTBOX_TOP.render(graphics, x + 21, y + 42);
         UIRenderHelper.drawStretched(graphics, x + 21, y + 60, 150, 26, 0, AllGuiTextures.STATION_TEXTBOX_MIDDLE);
         AllGuiTextures.STATION_TEXTBOX_BOTTOM.render(graphics, x + 21, y + 86);
         ms.pushPose();
         ms.translate(Mth.clamp(position + (float)offset - 13.0F, 25.0F, 159.0F), 0.0F, 0.0F);
         AllGuiTextures.STATION_TEXTBOX_SPEECH.render(graphics, x, y + 38);
         ms.popPose();
         text = this.trainNameBox.getValue();
         if (!this.trainNameBox.isFocused()) {
            int buttonX = this.nameBoxX(text, this.trainNameBox) + this.font.width(text) + 5;
            AllGuiTextures.STATION_EDIT_TRAIN_NAME.render(graphics, Math.min(buttonX, this.guiLeft + 156), y + 44);
            if (this.font.width(text) > this.trainNameBox.getWidth()) {
               graphics.drawString(this.font, "...", this.guiLeft + 26, this.guiTop + 47, 10921638);
            }
         }

         if (this.mapModsPresent()) {
            AllGuiTextures sprite = AllGuiTextures.TRAINMAP_SPRITES;
            sprite.bind();
            int trainColorIndex = this.colorTypeScroll.getState();
            int colorRow = trainColorIndex / 4;
            int colorCol = trainColorIndex % 4;
            int rotation = AnimationTickHolder.getTicks() / 5 % 8;

            for (int slice = 0; slice < 3; slice++) {
               int row = slice == 0 ? 1 : (slice == 2 ? 2 : 3);
               int positionX = this.colorTypeScroll.getX() + 4;
               int positionY = this.colorTypeScroll.getY() - 1;
               int sheetX = rotation * 16 + colorCol * 128;
               int sheetY = row * 16 + colorRow * 64;
               graphics.blit(sprite.location, positionX, positionY, (float)sheetX, (float)sheetY, 16, 16, sprite.getWidth(), sprite.getHeight());
            }
         }
      }
   }

   public boolean mapModsPresent() {
      return Mods.FTBCHUNKS.isLoaded() || Mods.JOURNEYMAP.isLoaded() || Mods.XAEROWORLDMAP.isLoaded();
   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (!this.nameBox.isFocused()
         && pMouseY > (double)this.guiTop
         && pMouseY < (double)(this.guiTop + 14)
         && pMouseX > (double)this.guiLeft
         && pMouseX < (double)(this.guiLeft + this.background.getWidth())) {
         this.nameBox.setFocused(true);
         this.nameBox.setHighlightPos(0);
         this.setFocused(this.nameBox);
         return true;
      } else if (this.trainNameBox.active
         && !this.trainNameBox.isFocused()
         && pMouseY > (double)(this.guiTop + 45)
         && pMouseY < (double)(this.guiTop + 58)
         && pMouseX > (double)(this.guiLeft + 25)
         && pMouseX < (double)(this.guiLeft + 168)) {
         this.trainNameBox.setFocused(true);
         this.trainNameBox.setHighlightPos(0);
         this.setFocused(this.trainNameBox);
         return true;
      } else {
         return super.mouseClicked(pMouseX, pMouseY, pButton);
      }
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      boolean hitEnter = this.getFocused() instanceof EditBox && (pKeyCode == 257 || pKeyCode == 335);
      if (hitEnter && this.nameBox.isFocused()) {
         this.nameBox.setFocused(false);
         this.syncStationName();
         return true;
      } else if (hitEnter && this.trainNameBox.isFocused()) {
         this.trainNameBox.setFocused(false);
         this.syncTrainNameAndColor();
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   private void syncTrainNameAndColor() {
      Train train = this.displayedTrain.get();
      if (train != null && !this.trainNameBox.getValue().equals(train.name.getString())) {
         CatnipServices.NETWORK.sendToServer(new TrainEditPacket.Serverbound(train.id, this.trainNameBox.getValue(), train.icon.getId(), train.mapColorIndex));
      }
   }

   private void syncStationName() {
      if (!this.nameBox.getValue().equals(this.station.name)) {
         CatnipServices.NETWORK.sendToServer(StationEditPacket.configure(this.blockEntity.getBlockPos(), false, this.nameBox.getValue(), this.doorControl));
      }
   }

   public void removed() {
      super.removed();
      if (this.nameBox != null && this.trainNameBox != null) {
         CatnipServices.NETWORK
            .sendToServer(StationEditPacket.configure(this.blockEntity.getBlockPos(), this.switchingToAssemblyMode, this.nameBox.getValue(), this.doorControl));
         Train train = this.displayedTrain.get();
         if (train != null) {
            if (!this.switchingToAssemblyMode) {
               CatnipServices.NETWORK
                  .sendToServer(new TrainEditPacket.Serverbound(train.id, this.trainNameBox.getValue(), train.icon.getId(), train.mapColorIndex));
            } else {
               this.blockEntity.imminentTrain = null;
            }
         }
      }
   }

   @Override
   protected PartialModel getFlag(float partialTicks) {
      return this.blockEntity.flag.getValue(partialTicks) > 0.75F ? AllPartialModels.STATION_ON : AllPartialModels.STATION_OFF;
   }
}
