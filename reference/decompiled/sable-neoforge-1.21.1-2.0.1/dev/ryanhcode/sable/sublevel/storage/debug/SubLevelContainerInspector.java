package dev.ryanhcode.sable.sublevel.storage.debug;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.SubLevel;
import foundry.veil.api.client.editor.SingleWindowInspector;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import java.util.BitSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class SubLevelContainerInspector extends SingleWindowInspector {
   public static final Component TITLE = Component.translatable("inspector.sable.sub_level_container.title");

   protected void renderComponents() {
      IntegratedServer singleplayerServer = Minecraft.getInstance().getSingleplayerServer();
      Minecraft minecraft = Minecraft.getInstance();
      ClientLevel clientLevel = minecraft.level;
      if (clientLevel != null) {
         SubLevelContainer clientPlotContainer = ((SubLevelContainerHolder)clientLevel).sable$getPlotContainer();
         this.renderPlotContainer("Client", clientPlotContainer);
         if (singleplayerServer != null) {
            ServerLevel serverLevel = singleplayerServer.getLevel(clientLevel.dimension());

            assert serverLevel != null : "Server level is null";

            ImGui.sameLine();
            SubLevelContainer serverPlotContainer = ((SubLevelContainerHolder)serverLevel).sable$getPlotContainer();
            this.renderPlotContainer("Server", serverPlotContainer);
         }
      } else {
         ImGui.textDisabled("No level loaded");
      }
   }

   private void renderPlotContainer(String name, SubLevelContainer plotContainer) {
      int sideLength = 1 << plotContainer.getLogSideLength();
      float buttonStartX = ImGui.getCursorScreenPosX();
      float buttonStartY = ImGui.getCursorScreenPosY();
      float sizePixels = ImGui.getWindowHeight() - 40.0F;
      ImGui.button(name, sizePixels, sizePixels);
      ImDrawList drawList = ImGui.getWindowDrawList();
      drawList.addRect(buttonStartX, buttonStartY, buttonStartX + sizePixels, buttonStartY + sizePixels, -1);
      ImVec2 mousePos = ImGui.getMousePos();
      float mouseX = mousePos.x - buttonStartX;
      float mouseY = mousePos.y - buttonStartY;
      int selectedXCell = (int)(mouseX / (sizePixels / (float)sideLength));
      int selectedYCell = (int)(mouseY / (sizePixels / (float)sideLength));
      boolean hovered = ImGui.isItemHovered();

      for (int x = 0; x < sideLength; x++) {
         for (int z = 0; z < sideLength; z++) {
            BitSet occupancy = plotContainer.getOccupancy();
            boolean isOccupied = occupancy.get(plotContainer.getIndex(x, z));
            boolean hasSubLevel = plotContainer.getAllSubLevels().get(plotContainer.getIndex(x, z)) != null;
            float xStart = buttonStartX + (float)x * (sizePixels / (float)sideLength);
            float zStart = buttonStartY + (float)z * (sizePixels / (float)sideLength);
            float xEnd = xStart + sizePixels / (float)sideLength;
            float zEnd = zStart + sizePixels / (float)sideLength;
            boolean cellSelected = hovered && x == selectedXCell && z == selectedYCell;
            int occupiedColor = hasSubLevel ? -16711936 : -16751104;
            drawList.addRectFilled(xStart, zStart, xEnd, zEnd, isOccupied ? occupiedColor : -13421773);
            drawList.addRect(xStart, zStart, xEnd, zEnd, -12303292);
            if (cellSelected) {
               drawList.addRectFilled(xStart, zStart, xEnd, zEnd, -1433892728);
            }
         }
      }

      SubLevel selectedSubLevel = plotContainer.getSubLevel(selectedXCell, selectedYCell);
      if (selectedSubLevel != null) {
         int chunkCount = selectedSubLevel.getPlot().getLoadedChunks().size();
         ImGui.setTooltip(String.format("Loaded SubLevel\nChunks: %d", chunkCount));
      }

      if (hovered) {
         float textY = buttonStartY + sizePixels - 20.0F;
         String text = String.format("%d, %d", selectedXCell, selectedYCell);
         float textWidth = ImGui.calcTextSize(text).x;
         float textX = buttonStartX + sizePixels - textWidth - 5.0F;
         drawList.addText(textX, textY, -1, text);
      }

      drawList.addText(buttonStartX + 5.0F, buttonStartY + 5.0F, -1, name);
      drawList.addText(buttonStartX + 5.0F, buttonStartY + 25.0F, -1, "%d loaded sub-level(s)".formatted(plotContainer.getLoadedCount()));
   }

   public Component getDisplayName() {
      return TITLE;
   }
}
