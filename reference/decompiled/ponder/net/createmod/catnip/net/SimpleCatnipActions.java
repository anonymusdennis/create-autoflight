package net.createmod.catnip.net;

import net.createmod.catnip.config.ui.ConfigHelper;
import net.createmod.catnip.config.ui.ConfigModListScreen;
import net.createmod.catnip.config.ui.SubMenuConfigScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class SimpleCatnipActions {
   public static void configScreen(String value) {
      if (value.equals("")) {
         ScreenOpener.open(new ConfigModListScreen(null));
      } else {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            ConfigHelper.ConfigPath configPath;
            try {
               configPath = ConfigHelper.ConfigPath.parse(value);
            } catch (IllegalArgumentException var5) {
               player.displayClientMessage(Component.literal(var5.getMessage()), false);
               return;
            }

            try {
               ScreenOpener.open(SubMenuConfigScreen.find(configPath));
            } catch (Exception var4) {
               player.displayClientMessage(
                  Component.literal("[Catnip]: ")
                     .withStyle(ChatFormatting.YELLOW)
                     .append(Component.translatable("catnip.util.unable_to_find_config.message").withStyle(ChatFormatting.WHITE)),
                  false
               );
            }
         }
      }
   }
}
