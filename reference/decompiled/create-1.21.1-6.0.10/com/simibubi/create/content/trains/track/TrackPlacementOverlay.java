package com.simibubi.create.content.trains.track;

import com.mojang.blaze3d.platform.Window;
import com.simibubi.create.foundation.mixin.accessor.GuiAccessor;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;

public class TrackPlacementOverlay implements Layer {
   public static final TrackPlacementOverlay INSTANCE = new TrackPlacementOverlay();

   public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.options.hideGui && mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
         if (TrackPlacement.hoveringPos != null) {
            if (TrackPlacement.cached != null && TrackPlacement.cached.curve != null && TrackPlacement.cached.valid) {
               if (TrackPlacement.extraTipWarmup >= 4) {
                  if (((GuiAccessor)mc.gui).create$getToolHighlightTimer() <= 0) {
                     boolean active = mc.options.keySprint.isDown();
                     MutableComponent text = CreateLang.translateDirect(
                        "track.hold_for_smooth_curve", Component.keybind("key.sprint").withStyle(active ? ChatFormatting.WHITE : ChatFormatting.GRAY)
                     );
                     Window window = mc.getWindow();
                     int x = (window.getGuiScaledWidth() - mc.font.width(text)) / 2;
                     int y = window.getGuiScaledHeight() - 61;
                     Color color = new Color(4905802).setAlpha(Mth.clamp((float)(TrackPlacement.extraTipWarmup - 4) / 3.0F, 0.1F, 1.0F));
                     guiGraphics.drawString(mc.font, text, x, y, color.getRGB(), false);
                  }
               }
            }
         }
      }
   }
}
