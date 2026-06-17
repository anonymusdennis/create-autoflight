package com.simibubi.create.infrastructure.gui;

import com.simibubi.create.AllItems;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent.Init.Post;
import org.apache.commons.lang3.mutable.MutableObject;

public class OpenCreateMenuButton extends Button {
   public OpenCreateMenuButton(int x, int y) {
      super(x, y, 20, 20, CommonComponents.EMPTY, OpenCreateMenuButton::click, DEFAULT_NARRATION);
   }

   public void renderString(GuiGraphics graphics, Font pFont, int pColor) {
      ItemStack icon = AllItems.GOGGLES.asStack();
      BakedModel bakedmodel = Minecraft.getInstance().getItemRenderer().getModel(icon, Minecraft.getInstance().level, Minecraft.getInstance().player, 0);
      if (bakedmodel != null) {
         graphics.renderItem(icon, this.getX() + 2, this.getY() + 2);
      }
   }

   public static void click(Button b) {
      ScreenOpener.open(new CreateMainMenuScreen(Minecraft.getInstance().screen));
   }

   public static class MenuRows {
      public static final OpenCreateMenuButton.MenuRows MAIN_MENU = new OpenCreateMenuButton.MenuRows(
         Arrays.asList(
            new OpenCreateMenuButton.SingleMenuRow("menu.singleplayer"),
            new OpenCreateMenuButton.SingleMenuRow("menu.multiplayer"),
            new OpenCreateMenuButton.SingleMenuRow("fml.menu.mods", "menu.online"),
            new OpenCreateMenuButton.SingleMenuRow("narrator.button.language", "narrator.button.accessibility")
         )
      );
      public static final OpenCreateMenuButton.MenuRows INGAME_MENU = new OpenCreateMenuButton.MenuRows(
         Arrays.asList(
            new OpenCreateMenuButton.SingleMenuRow("menu.returnToGame"),
            new OpenCreateMenuButton.SingleMenuRow("gui.advancements", "gui.stats"),
            new OpenCreateMenuButton.SingleMenuRow("menu.sendFeedback", "menu.reportBugs"),
            new OpenCreateMenuButton.SingleMenuRow("menu.options", "menu.shareToLan"),
            new OpenCreateMenuButton.SingleMenuRow("menu.returnToMenu")
         )
      );
      protected final List<String> leftTextKeys;
      protected final List<String> rightTextKeys;

      public MenuRows(List<OpenCreateMenuButton.SingleMenuRow> rows) {
         this.leftTextKeys = rows.stream().map(OpenCreateMenuButton.SingleMenuRow::leftTextKey).collect(Collectors.toList());
         this.rightTextKeys = rows.stream().map(OpenCreateMenuButton.SingleMenuRow::rightTextKey).collect(Collectors.toList());
      }
   }

   @EventBusSubscriber({Dist.CLIENT})
   public static class OpenConfigButtonHandler {
      @SubscribeEvent
      public static void onGuiInit(Post event) {
         Screen screen = event.getScreen();
         OpenCreateMenuButton.MenuRows menu;
         int rowIdx;
         int offsetX;
         if (screen instanceof TitleScreen) {
            menu = OpenCreateMenuButton.MenuRows.MAIN_MENU;
            rowIdx = (Integer)AllConfigs.client().mainMenuConfigButtonRow.get();
            offsetX = (Integer)AllConfigs.client().mainMenuConfigButtonOffsetX.get();
         } else {
            if (!(screen instanceof PauseScreen)) {
               return;
            }

            menu = OpenCreateMenuButton.MenuRows.INGAME_MENU;
            rowIdx = (Integer)AllConfigs.client().ingameMenuConfigButtonRow.get();
            offsetX = (Integer)AllConfigs.client().ingameMenuConfigButtonOffsetX.get();
         }

         if (rowIdx != 0) {
            boolean onLeft = offsetX < 0;
            String targetMessage = I18n.get((onLeft ? menu.leftTextKeys : menu.rightTextKeys).get(rowIdx - 1), new Object[0]);
            int offsetX_ = offsetX;
            MutableObject<GuiEventListener> toAdd = new MutableObject(null);
            event.getListenersList()
               .stream()
               .filter(w -> w instanceof AbstractWidget)
               .map(w -> (AbstractWidget)w)
               .filter(w -> w.getMessage().getString().equals(targetMessage))
               .findFirst()
               .ifPresent(w -> toAdd.setValue(new OpenCreateMenuButton(w.getX() + offsetX_ + (onLeft ? -20 : w.getWidth()), w.getY())));
            if (toAdd.getValue() != null) {
               event.addListener((GuiEventListener)toAdd.getValue());
            }
         }
      }
   }

   public static record SingleMenuRow(String leftTextKey, String rightTextKey) {
      public SingleMenuRow(String centerTextKey) {
         this(centerTextKey, centerTextKey);
      }
   }
}
