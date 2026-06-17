package net.createmod.ponder.foundation;

import com.google.common.base.Strings;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.gui.NavigatableSimiScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.enums.PonderKeybinds;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class PonderTooltipHandler {
   private static final Color borderA = new Color(5243135, false).setImmutable();
   private static final Color borderB = new Color(5592575, false).setImmutable();
   private static final Color borderC = new Color(16777215, false).setImmutable();
   public static boolean enable = true;
   static LerpedFloat holdKeyProgress = LerpedFloat.linear().startWithValue(0.0);
   static ItemStack hoveredStack = ItemStack.EMPTY;
   static ItemStack trackingStack = ItemStack.EMPTY;
   static boolean subject = false;
   static boolean deferTick = false;
   static final List<Consumer<ItemStack>> hoveredStackCallbacks = new ArrayList<>();
   public static final String HOLD_TO_PONDER = "ui.hold_to_ponder";
   public static final String SUBJECT = "ui.subject";

   public static void tick() {
      deferTick = true;
   }

   public static void deferredTick() {
      deferTick = false;
      Minecraft instance = Minecraft.getInstance();
      Screen currentScreen = instance.screen;
      if (!hoveredStack.isEmpty() && !trackingStack.isEmpty()) {
         float value = holdKeyProgress.getValue();
         if (RenderSystem.isOnRenderThread() && !subject && PonderKeybinds.PONDER.isDown() && currentScreen != null) {
            if (value >= 1.0F) {
               if (currentScreen instanceof NavigatableSimiScreen) {
                  ((NavigatableSimiScreen)currentScreen).centerScalingOnMouse();
               }

               ScreenOpener.transitionTo(PonderUI.of(trackingStack));
               holdKeyProgress.startWithValue(0.0);
               return;
            }

            holdKeyProgress.setValue((double)Math.min(1.0F, value + Math.max(0.25F, value) * 0.25F));
         } else {
            holdKeyProgress.setValue((double)Math.max(0.0F, value - 0.05F));
         }

         hoveredStack = ItemStack.EMPTY;
      } else {
         trackingStack = ItemStack.EMPTY;
         holdKeyProgress.startWithValue(0.0);
      }
   }

   public static void addToTooltip(List<Component> toolTip, ItemStack stack) {
      if (enable) {
         if (!NavigatableSimiScreen.isCurrentlyRenderingPreviousScreen()) {
            updateHovered(stack);
            if (deferTick) {
               deferredTick();
            }

            if (trackingStack == stack) {
               float renderPartialTicks = AnimationTickHolder.getPartialTicksUI();
               Component component = (Component)(subject
                  ? Ponder.lang().translate("ui.subject").component().withStyle(ChatFormatting.GREEN)
                  : makeProgressBar(Math.min(1.0F, holdKeyProgress.getValue(renderPartialTicks) * 8.0F / 7.0F)));
               if (toolTip.size() < 2) {
                  toolTip.add(component);
               } else {
                  toolTip.add(1, component);
               }
            }
         }
      }
   }

   protected static void updateHovered(ItemStack stack) {
      Minecraft instance = Minecraft.getInstance();
      Screen currentScreen = instance.screen;
      boolean inPonderUI = currentScreen instanceof PonderUI;
      ItemStack prevStack = trackingStack;
      hoveredStack = ItemStack.EMPTY;
      subject = false;
      if (inPonderUI) {
         PonderUI ponderUI = (PonderUI)currentScreen;
         ItemStack uiSubject = ponderUI.getSubject();
         if (!uiSubject.isEmpty() && stack.is(uiSubject.getItem())) {
            subject = true;
         }
      }

      if (!stack.isEmpty()) {
         if (PonderIndex.getSceneAccess().doScenesExistForId(RegisteredObjectsHelper.getKeyOrThrow(stack.getItem()))) {
            if (prevStack.isEmpty() || !prevStack.is(stack.getItem())) {
               holdKeyProgress.startWithValue(0.0);
            }

            hoveredStack = stack;
            trackingStack = stack;

            for (Consumer<ItemStack> hoveredStackCallback : hoveredStackCallbacks) {
               hoveredStackCallback.accept(hoveredStack.copy());
            }
         }
      }
   }

   public static Optional<Couple<Color>> handleTooltipColor(ItemStack stack) {
      if (trackingStack != stack) {
         return Optional.empty();
      } else if (holdKeyProgress.getValue() == 0.0F) {
         return Optional.empty();
      } else {
         float renderPartialTicks = AnimationTickHolder.getPartialTicksUI();
         float progress = Math.min(1.0F, holdKeyProgress.getValue(renderPartialTicks) * 8.0F / 7.0F);
         Color startC = getSmoothColorForProgress(progress);
         Color endC = getSmoothColorForProgress(progress);
         return Optional.of(Couple.create(startC, endC));
      }
   }

   private static Color getSmoothColorForProgress(float progress) {
      return (double)progress < 0.5 ? borderA.mixWith(borderB, progress * 2.0F) : borderB.mixWith(borderC, (progress - 0.5F) * 2.0F);
   }

   private static Component makeProgressBar(float progress) {
      MutableComponent holdW = Ponder.lang()
         .translate("ui.hold_to_ponder", PonderKeybinds.PONDER.message().copy().withStyle(ChatFormatting.GRAY))
         .style(ChatFormatting.DARK_GRAY)
         .component();
      Font fontRenderer = Minecraft.getInstance().font;
      float charWidth = (float)fontRenderer.width("|");
      float tipWidth = (float)fontRenderer.width(holdW);
      int total = (int)(tipWidth / charWidth);
      int current = (int)(progress * (float)total);
      if (progress > 0.0F) {
         String bars = "";
         bars = bars + ChatFormatting.GRAY + Strings.repeat("|", current);
         if (progress < 1.0F) {
            bars = bars + ChatFormatting.DARK_GRAY + Strings.repeat("|", total - current);
         }

         return Component.literal(bars);
      } else {
         return holdW;
      }
   }

   public static synchronized void registerHoveredPonderStackCallback(Consumer<ItemStack> consumer) {
      hoveredStackCallbacks.add(consumer);
   }

   public static synchronized void removeHoveredPonderStackCallback(Consumer<ItemStack> consumer) {
      hoveredStackCallbacks.remove(consumer);
   }
}
