package com.simibubi.create.content.equipment.goggles;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.equipment.goggles.IHaveCustomOverlayIcon;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.foundation.mixin.accessor.MouseHandlerAccessor;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.outliner.Outliner.OutlineEntry;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class GoggleOverlayRenderer {
   public static final Layer OVERLAY = GoggleOverlayRenderer::renderOverlay;
   private static final Map<Object, OutlineEntry> outlines = Outliner.getInstance().getOutlines();
   public static int hoverTicks = 0;
   public static BlockPos lastHovered = null;

   public static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.options.hideGui && mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
         if (!(mc.hitResult instanceof BlockHitResult result)) {
            lastHovered = null;
            hoverTicks = 0;
         } else {
            for (OutlineEntry entry : outlines.values()) {
               if (entry.isAlive()) {
                  Outline outline = entry.getOutline();
                  if (outline instanceof ValueBox && !((ValueBox)outline).isPassive) {
                     return;
                  }
               }
            }

            ClientLevel world = mc.level;
            BlockPos pos = result.getBlockPos();
            int prevHoverTicks = hoverTicks++;
            lastHovered = pos;
            pos = proxiedOverlayPosition(world, pos);
            BlockEntity be = world.getBlockEntity(pos);
            boolean wearingGoggles = GogglesItem.isWearingGoggles(mc.player);
            boolean isShifting = mc.player.isShiftKeyDown();
            boolean hasGoggleInformation = be instanceof IHaveGoggleInformation;
            boolean hasHoveringInformation = be instanceof IHaveHoveringInformation;
            boolean goggleAddedInformation = false;
            boolean hoverAddedInformation = false;
            ItemStack item = AllItems.GOGGLES.asStack();
            List<Component> tooltip = new ArrayList<>();
            if (be instanceof IHaveCustomOverlayIcon customOverlayIcon) {
               item = customOverlayIcon.getIcon(isShifting);
            }

            if (hasGoggleInformation && wearingGoggles) {
               IHaveGoggleInformation gte = (IHaveGoggleInformation)be;
               goggleAddedInformation = gte.addToGoggleTooltip(tooltip, isShifting);
            }

            if (hasHoveringInformation) {
               if (!tooltip.isEmpty()) {
                  tooltip.add(CommonComponents.EMPTY);
               }

               IHaveHoveringInformation hte = (IHaveHoveringInformation)be;
               hoverAddedInformation = hte.addToTooltip(tooltip, isShifting);
               if (goggleAddedInformation && !hoverAddedInformation) {
                  tooltip.remove(tooltip.size() - 1);
               }
            }

            if (be instanceof IDisplayAssemblyExceptions) {
               boolean exceptionAdded = ((IDisplayAssemblyExceptions)be).addExceptionToTooltip(tooltip);
               if (exceptionAdded) {
                  hasHoveringInformation = true;
                  hoverAddedInformation = true;
               }
            }

            if (!hasHoveringInformation && (hasHoveringInformation = hoverAddedInformation = TrainRelocator.addToTooltip(tooltip, isShifting))) {
               hoverTicks = prevHoverTicks + 1;
            }

            if (hasGoggleInformation && !goggleAddedInformation && hasHoveringInformation && !hoverAddedInformation) {
               hoverTicks = 0;
            } else {
               BlockState state = world.getBlockState(pos);
               if (wearingGoggles && AllBlocks.PISTON_EXTENSION_POLE.has(state)) {
                  Direction[] directions = Iterate.directionsInAxis(((Direction)state.getValue(PistonExtensionPoleBlock.FACING)).getAxis());
                  int poles = 1;
                  boolean pistonFound = false;

                  for (Direction dir : directions) {
                     int attachedPoles = PistonExtensionPoleBlock.PlacementHelper.get().attachedPoles(world, pos, dir);
                     poles += attachedPoles;
                     pistonFound |= world.getBlockState(pos.relative(dir, attachedPoles + 1)).getBlock() instanceof MechanicalPistonBlock;
                  }

                  if (!pistonFound) {
                     hoverTicks = 0;
                     return;
                  }

                  if (!tooltip.isEmpty()) {
                     tooltip.add(CommonComponents.EMPTY);
                  }

                  CreateLang.translate("gui.goggles.pole_length").text(" " + poles).forGoggles(tooltip);
               }

               if (tooltip.isEmpty()) {
                  hoverTicks = 0;
               } else {
                  PoseStack poseStack = guiGraphics.pose();
                  poseStack.pushPose();
                  int tooltipTextWidth = 0;

                  for (FormattedText textLine : tooltip) {
                     int textLineWidth = mc.font.width(textLine);
                     if (textLineWidth > tooltipTextWidth) {
                        tooltipTextWidth = textLineWidth;
                     }
                  }

                  int tooltipHeight = 8;
                  if (tooltip.size() > 1) {
                     tooltipHeight += 2;
                     tooltipHeight += (tooltip.size() - 1) * 10;
                  }

                  int width = guiGraphics.guiWidth();
                  int height = guiGraphics.guiHeight();
                  CClient cfg = AllConfigs.client();
                  int posX = width / 2 + (Integer)cfg.overlayOffsetX.get();
                  int posY = height / 2 + (Integer)cfg.overlayOffsetY.get();
                  posX = Math.min(posX, width - tooltipTextWidth - 20);
                  posY = Math.min(posY, height - tooltipHeight - 20);
                  float fade = Mth.clamp(((float)hoverTicks + deltaTracker.getGameTimeDeltaPartialTick(false)) / 24.0F, 0.0F, 1.0F);
                  Boolean useCustom = (Boolean)cfg.overlayCustomColor.get();
                  Color colorBackground = useCustom
                     ? new Color((Integer)cfg.overlayBackgroundColor.get())
                     : BoxElement.COLOR_VANILLA_BACKGROUND.scaleAlpha(0.75F);
                  Color colorBorderTop = useCustom
                     ? new Color((Integer)cfg.overlayBorderColorTop.get())
                     : ((Color)BoxElement.COLOR_VANILLA_BORDER.getFirst()).copy();
                  Color colorBorderBot = useCustom
                     ? new Color((Integer)cfg.overlayBorderColorBot.get())
                     : ((Color)BoxElement.COLOR_VANILLA_BORDER.getSecond()).copy();
                  if (fade < 1.0F) {
                     poseStack.translate(
                        Math.pow((double)(1.0F - fade), 3.0) * (double)Math.signum((float)((Integer)cfg.overlayOffsetX.get()).intValue() + 0.5F) * 8.0,
                        0.0,
                        0.0
                     );
                     colorBackground.scaleAlpha(fade);
                     colorBorderTop.scaleAlpha(fade);
                     colorBorderBot.scaleAlpha(fade);
                  }

                  GuiGameElement.of(item).at((float)(posX + 10), (float)(posY - 16), 450.0F).render(guiGraphics);
                  if (!Mods.MODERNUI.isLoaded()) {
                     RemovedGuiUtils.drawHoveringText(
                        guiGraphics,
                        tooltip,
                        posX,
                        posY,
                        width,
                        height,
                        -1,
                        colorBackground.getRGB(),
                        colorBorderTop.getRGB(),
                        colorBorderBot.getRGB(),
                        mc.font
                     );
                     poseStack.popPose();
                  } else {
                     MouseHandler mouseHandler = Minecraft.getInstance().mouseHandler;
                     Window window = Minecraft.getInstance().getWindow();
                     double guiScale = window.getGuiScale();
                     double cursorX = mouseHandler.xpos();
                     double cursorY = mouseHandler.ypos();
                     ((MouseHandlerAccessor)mouseHandler).create$setXPos((double)Math.round(cursorX / guiScale) * guiScale);
                     ((MouseHandlerAccessor)mouseHandler).create$setYPos((double)Math.round(cursorY / guiScale) * guiScale);
                     RemovedGuiUtils.drawHoveringText(
                        guiGraphics,
                        tooltip,
                        posX,
                        posY,
                        width,
                        height,
                        -1,
                        colorBackground.getRGB(),
                        colorBorderTop.getRGB(),
                        colorBorderBot.getRGB(),
                        mc.font
                     );
                     ((MouseHandlerAccessor)mouseHandler).create$setXPos(cursorX);
                     ((MouseHandlerAccessor)mouseHandler).create$setYPos(cursorY);
                     poseStack.popPose();
                  }
               }
            }
         }
      }
   }

   public static BlockPos proxiedOverlayPosition(Level level, BlockPos pos) {
      BlockState targetedState = level.getBlockState(pos);
      return targetedState.getBlock() instanceof IProxyHoveringInformation proxy ? proxy.getInformationSource(level, pos, targetedState) : pos;
   }
}
