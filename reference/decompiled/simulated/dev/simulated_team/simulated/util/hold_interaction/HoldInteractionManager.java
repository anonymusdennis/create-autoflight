package dev.simulated_team.simulated.util.hold_interaction;

import dev.simulated_team.simulated.util.SimDistUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class HoldInteractionManager {
   @Nullable
   private static BlockHoldInteraction active = null;
   private static int crouchBlockTicks = 0;

   public static boolean isActive() {
      return active != null;
   }

   public static boolean isActive(BlockHoldInteraction blockHoldInteraction) {
      return blockHoldInteraction == active;
   }

   public static void start(BlockHoldInteraction blockHoldInteraction) {
      if (active != null) {
         active.stop();
      }

      active = blockHoldInteraction;
      active.start();
   }

   public static void stop() {
      if (active != null) {
         active.stop();
         active = null;
      }
   }

   public static void tick(Level level, LocalPlayer player) {
      if (crouchBlockTicks > 0) {
         if (!unblockedShift()) {
            crouchBlockTicks = 0;
         }

         crouchBlockTicks--;
      }

      if (active != null && level != null) {
         crouchBlockTicks = active.getCrouchBlockingTicks();
         if (active.activeTick(level, player)) {
            active.stop();
            active = null;
         }
      }
   }

   public static void renderOverlay(GuiGraphics graphics, int width, int height) {
      if (active != null) {
         active.renderOverlay(graphics, width, height, Minecraft.getInstance().options.hideGui);
      }
   }

   public static boolean canCrouch() {
      return crouchBlockTicks <= 0;
   }

   public static boolean unblockedShift() {
      return ((LocalPlayer)SimDistUtil.getClientPlayer()).input.shiftKeyDown;
   }
}
