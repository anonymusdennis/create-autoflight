package com.simibubi.create.content.trains.station;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainIconType;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.lang.ref.WeakReference;
import java.util.List;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class AssemblyScreen extends AbstractStationScreen {
   private IconButton quitAssembly;
   private IconButton toggleAssemblyButton;
   private List<ResourceLocation> iconTypes;
   private ScrollInput iconTypeScroll;

   public AssemblyScreen(StationBlockEntity be, GlobalStation station) {
      super(be, station);
      this.background = AllGuiTextures.STATION_ASSEMBLING;
   }

   @Override
   protected void init() {
      super.init();
      int x = this.guiLeft;
      int y = this.guiTop;
      int by = y + this.background.getHeight() - 24;
      Renderable widget = (Renderable)this.renderables.get(0);
      if (widget instanceof IconButton ib) {
         ib.setIcon(AllIcons.I_PRIORITY_VERY_LOW);
         ib.setToolTip(CreateLang.translateDirect("station.close"));
      }

      this.iconTypes = TrainIconType.REGISTRY.keySet().stream().toList();
      this.iconTypeScroll = new ScrollInput(x + 4, y + 17, 162, 14).titled(CreateLang.translateDirect("station.icon_type"));
      this.iconTypeScroll.withRange(0, this.iconTypes.size());
      this.iconTypeScroll.withStepFunction(ctx -> -this.iconTypeScroll.standardStep().apply(ctx));
      this.iconTypeScroll.calling(s -> {
         Train train = this.displayedTrain.get();
         if (train != null) {
            train.icon = TrainIconType.byId(this.iconTypes.get(s));
         }
      });
      this.iconTypeScroll.active = this.iconTypeScroll.visible = false;
      this.addRenderableWidget(this.iconTypeScroll);
      this.toggleAssemblyButton = new WideIconButton(x + 94, by, AllGuiTextures.I_ASSEMBLE_TRAIN);
      this.toggleAssemblyButton.active = false;
      this.toggleAssemblyButton.setToolTip(CreateLang.translateDirect("station.assemble_train"));
      this.toggleAssemblyButton.withCallback(() -> CatnipServices.NETWORK.sendToServer(StationEditPacket.tryAssemble(this.blockEntity.getBlockPos())));
      this.quitAssembly = new IconButton(x + 73, by, AllIcons.I_DISABLE);
      this.quitAssembly.active = true;
      this.quitAssembly.setToolTip(CreateLang.translateDirect("station.cancel"));
      this.quitAssembly.withCallback(() -> {
         CatnipServices.NETWORK.sendToServer(StationEditPacket.configure(this.blockEntity.getBlockPos(), false, this.station.name, null));
         this.minecraft.setScreen(new StationScreen(this.blockEntity, this.station));
      });
      this.addRenderableWidget(this.toggleAssemblyButton);
      this.addRenderableWidget(this.quitAssembly);
      this.tickTrainDisplay();
   }

   @Override
   public void tick() {
      super.tick();
      this.tickTrainDisplay();
      Train train = this.displayedTrain.get();
      this.toggleAssemblyButton.active = this.blockEntity.bogeyCount > 0 || train != null;
      if (train != null) {
         CatnipServices.NETWORK.sendToServer(StationEditPacket.configure(this.blockEntity.getBlockPos(), false, this.station.name, null));
         this.minecraft.setScreen(new StationScreen(this.blockEntity, this.station));

         for (Carriage carriage : train.carriages) {
            carriage.updateConductors();
         }
      }
   }

   private void tickTrainDisplay() {
      if (this.getImminent() == null) {
         this.displayedTrain = new WeakReference<>(null);
         this.quitAssembly.active = true;
         this.iconTypeScroll.active = this.iconTypeScroll.visible = false;
         this.toggleAssemblyButton.setToolTip(CreateLang.translateDirect("station.assemble_train"));
         this.toggleAssemblyButton.setIcon(AllGuiTextures.I_ASSEMBLE_TRAIN);
         this.toggleAssemblyButton.withCallback(() -> CatnipServices.NETWORK.sendToServer(StationEditPacket.tryAssemble(this.blockEntity.getBlockPos())));
      } else {
         CatnipServices.NETWORK.sendToServer(StationEditPacket.configure(this.blockEntity.getBlockPos(), false, this.station.name, null));
         this.minecraft.setScreen(new StationScreen(this.blockEntity, this.station));
      }
   }

   @Override
   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWindow(graphics, mouseX, mouseY, partialTicks);
      int x = this.guiLeft;
      int y = this.guiTop;
      MutableComponent header = CreateLang.translateDirect("station.assembly_title");
      graphics.drawString(this.font, header, x + this.background.getWidth() / 2 - this.font.width(header) / 2, y + 4, 926259, false);
      AssemblyException lastAssemblyException = this.blockEntity.lastException;
      if (lastAssemblyException != null) {
         MutableComponent text = CreateLang.translateDirect("station.failed");
         graphics.drawString(this.font, text, x + 97 - this.font.width(text) / 2, y + 47, 7822171, false);
         int offset = 0;
         if (this.blockEntity.failedCarriageIndex != -1) {
            graphics.drawString(
               this.font, CreateLang.translateDirect("station.carriage_number", this.blockEntity.failedCarriageIndex), x + 30, y + 67, 8026746, false
            );
            offset += 10;
         }

         graphics.drawWordWrap(this.font, lastAssemblyException.component, x + 30, y + 67 + offset, 134, 7822171);
         offset += this.font.split(lastAssemblyException.component, 134).size() * 9 + 5;
         graphics.drawWordWrap(this.font, CreateLang.translateDirect("station.retry"), x + 30, y + 67 + offset, 134, 8026746);
      } else {
         int bogeyCount = this.blockEntity.bogeyCount;
         MutableComponent text = CreateLang.translateDirect(
            bogeyCount == 0 ? "station.no_bogeys" : (bogeyCount == 1 ? "station.one_bogey" : "station.more_bogeys"), bogeyCount
         );
         graphics.drawString(this.font, text, x + 97 - this.font.width(text) / 2, y + 47, 8026746, false);
         graphics.drawWordWrap(this.font, CreateLang.translateDirect("station.how_to"), x + 28, y + 62, 134, 8026746);
         graphics.drawWordWrap(this.font, CreateLang.translateDirect("station.how_to_1"), x + 28, y + 94, 134, 8026746);
         graphics.drawWordWrap(this.font, CreateLang.translateDirect("station.how_to_2"), x + 28, y + 117, 138, 8026746);
      }
   }

   public void removed() {
      super.removed();
      Train train = this.displayedTrain.get();
      if (train != null) {
         ResourceLocation iconId = this.iconTypes.get(this.iconTypeScroll.getState());
         train.icon = TrainIconType.byId(iconId);
         CatnipServices.NETWORK.sendToServer(new TrainEditPacket.Serverbound(train.id, "", iconId, train.mapColorIndex));
      }
   }

   @Override
   protected PartialModel getFlag(float partialTicks) {
      return AllPartialModels.STATION_ASSEMBLE;
   }
}
