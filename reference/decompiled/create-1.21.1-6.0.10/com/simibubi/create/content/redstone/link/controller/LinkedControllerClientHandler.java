package com.simibubi.create.content.redstone.link.controller;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.ControlsUtil;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LinkedControllerClientHandler {
   public static final Layer OVERLAY = LinkedControllerClientHandler::renderOverlay;
   public static LinkedControllerClientHandler.Mode MODE = LinkedControllerClientHandler.Mode.IDLE;
   public static int PACKET_RATE = 5;
   public static Collection<Integer> currentlyPressed = new HashSet<>();
   private static BlockPos lecternPos;
   private static BlockPos selectedLocation = BlockPos.ZERO;
   private static int packetCooldown;

   public static void toggleBindMode(BlockPos location) {
      if (MODE == LinkedControllerClientHandler.Mode.IDLE) {
         MODE = LinkedControllerClientHandler.Mode.BIND;
         selectedLocation = location;
      } else {
         MODE = LinkedControllerClientHandler.Mode.IDLE;
         onReset();
      }
   }

   public static void toggle() {
      if (MODE == LinkedControllerClientHandler.Mode.IDLE) {
         MODE = LinkedControllerClientHandler.Mode.ACTIVE;
         lecternPos = null;
      } else {
         MODE = LinkedControllerClientHandler.Mode.IDLE;
         onReset();
      }
   }

   public static void activateInLectern(BlockPos lecternAt) {
      if (MODE == LinkedControllerClientHandler.Mode.IDLE) {
         MODE = LinkedControllerClientHandler.Mode.ACTIVE;
         lecternPos = lecternAt;
      }
   }

   public static void deactivateInLectern() {
      if (MODE == LinkedControllerClientHandler.Mode.ACTIVE && inLectern()) {
         MODE = LinkedControllerClientHandler.Mode.IDLE;
         onReset();
      }
   }

   public static boolean inLectern() {
      return lecternPos != null;
   }

   protected static void onReset() {
      ControlsUtil.getControls().forEach(kb -> kb.setDown(ControlsUtil.isActuallyPressed(kb)));
      packetCooldown = 0;
      selectedLocation = BlockPos.ZERO;
      if (inLectern()) {
         CatnipServices.NETWORK.sendToServer(new LinkedControllerStopLecternPacket(lecternPos));
      }

      lecternPos = null;
      if (!currentlyPressed.isEmpty()) {
         CatnipServices.NETWORK.sendToServer(new LinkedControllerInputPacket(currentlyPressed, false));
      }

      currentlyPressed.clear();
      LinkedControllerItemRenderer.resetButtons();
   }

   public static void tick() {
      LinkedControllerItemRenderer.tick();
      if (MODE != LinkedControllerClientHandler.Mode.IDLE) {
         if (packetCooldown > 0) {
            packetCooldown--;
         }

         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         ItemStack heldItem = player.getMainHandItem();
         if (player.isSpectator()) {
            MODE = LinkedControllerClientHandler.Mode.IDLE;
            onReset();
         } else {
            if (!inLectern() && !AllItems.LINKED_CONTROLLER.isIn(heldItem)) {
               heldItem = player.getOffhandItem();
               if (!AllItems.LINKED_CONTROLLER.isIn(heldItem)) {
                  MODE = LinkedControllerClientHandler.Mode.IDLE;
                  onReset();
                  return;
               }
            }

            if (inLectern()
               && ((LecternControllerBlock)AllBlocks.LECTERN_CONTROLLER.get())
                  .getBlockEntityOptional(mc.level, lecternPos)
                  .map(be -> !be.isUsedBy(mc.player))
                  .orElse(true)) {
               deactivateInLectern();
            } else if (mc.screen != null) {
               MODE = LinkedControllerClientHandler.Mode.IDLE;
               onReset();
            } else if (InputConstants.isKeyDown(mc.getWindow().getWindow(), 256)) {
               MODE = LinkedControllerClientHandler.Mode.IDLE;
               onReset();
            } else {
               List<KeyMapping> controls = ControlsUtil.getControls();
               Collection<Integer> pressedKeys = new HashSet<>();

               for (int i = 0; i < controls.size(); i++) {
                  if (ControlsUtil.isActuallyPressed(controls.get(i))) {
                     pressedKeys.add(i);
                  }
               }

               Collection<Integer> newKeys = new HashSet<>(pressedKeys);
               Collection<Integer> releasedKeys = currentlyPressed;
               newKeys.removeAll(releasedKeys);
               releasedKeys.removeAll(pressedKeys);
               if (MODE == LinkedControllerClientHandler.Mode.ACTIVE) {
                  if (!releasedKeys.isEmpty()) {
                     CatnipServices.NETWORK.sendToServer(new LinkedControllerInputPacket(releasedKeys, false, lecternPos));
                     AllSoundEvents.CONTROLLER_CLICK.playAt(player.level(), player.blockPosition(), 1.0F, 0.5F, true);
                  }

                  if (!newKeys.isEmpty()) {
                     CatnipServices.NETWORK.sendToServer(new LinkedControllerInputPacket(newKeys, true, lecternPos));
                     packetCooldown = PACKET_RATE;
                     AllSoundEvents.CONTROLLER_CLICK.playAt(player.level(), player.blockPosition(), 1.0F, 0.75F, true);
                  }

                  if (packetCooldown == 0 && !pressedKeys.isEmpty()) {
                     CatnipServices.NETWORK.sendToServer(new LinkedControllerInputPacket(pressedKeys, true, lecternPos));
                     packetCooldown = PACKET_RATE;
                  }
               }

               if (MODE == LinkedControllerClientHandler.Mode.BIND) {
                  VoxelShape shape = mc.level.getBlockState(selectedLocation).getShape(mc.level, selectedLocation);
                  if (!shape.isEmpty()) {
                     Outliner.getInstance().showAABB("controller", shape.bounds().move(selectedLocation)).colored(12008493).lineWidth(0.0625F);
                  }

                  Iterator var8 = newKeys.iterator();
                  if (var8.hasNext()) {
                     Integer integer = (Integer)var8.next();
                     LinkBehaviour linkBehaviour = BlockEntityBehaviour.get(mc.level, selectedLocation, LinkBehaviour.TYPE);
                     if (linkBehaviour != null) {
                        CatnipServices.NETWORK.sendToServer(new LinkedControllerBindPacket(integer, selectedLocation));
                        CreateLang.translate("linked_controller.key_bound", controls.get(integer).getTranslatedKeyMessage().getString()).sendStatus(mc.player);
                     }

                     MODE = LinkedControllerClientHandler.Mode.IDLE;
                  }
               }

               currentlyPressed = pressedKeys;
               controls.forEach(kb -> kb.setDown(false));
            }
         }
      }
   }

   public static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
      int width1 = guiGraphics.guiWidth();
      int height1 = guiGraphics.guiHeight();
      Minecraft mc = Minecraft.getInstance();
      if (!mc.options.hideGui) {
         if (MODE == LinkedControllerClientHandler.Mode.BIND) {
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            Screen tooltipScreen = new Screen(CommonComponents.EMPTY) {
            };
            tooltipScreen.init(mc, width1, height1);
            Object[] keys = new Object[6];
            List<KeyMapping> controls = ControlsUtil.getControls();

            for (int i = 0; i < controls.size(); i++) {
               KeyMapping keyBinding = controls.get(i);
               keys[i] = keyBinding.getTranslatedKeyMessage().getString();
            }

            List<Component> list = new ArrayList<>();
            list.add(CreateLang.translateDirect("linked_controller.bind_mode").withStyle(ChatFormatting.GOLD));
            list.addAll(TooltipHelper.cutTextComponent(CreateLang.translateDirect("linked_controller.press_keybind", keys), Palette.ALL_GRAY));
            int width = 0;
            int height = list.size() * 9;

            for (Component iTextComponent : list) {
               width = Math.max(width, mc.font.width(iTextComponent));
            }

            int x = width1 / 3 - width / 2;
            int y = height1 - height - 24;
            guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, list, x, y);
            poseStack.popPose();
         }
      }
   }

   public static enum Mode {
      IDLE,
      ACTIVE,
      BIND;
   }
}
