package com.simibubi.create.content.equipment.toolbox;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllKeys;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import java.util.Comparator;
import java.util.List;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;

public class ToolboxHandlerClient {
   public static final Layer OVERLAY = ToolboxHandlerClient::renderOverlay;
   static int COOLDOWN = 0;

   public static void clientTick() {
      if (COOLDOWN > 0 && !AllKeys.TOOLBELT.isPressed()) {
         COOLDOWN--;
      }
   }

   public static boolean onPickItem() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null) {
         return false;
      } else {
         Level level = player.level();
         HitResult hitResult = mc.hitResult;
         if (hitResult != null && hitResult.getType() != Type.MISS) {
            if (player.isCreative()) {
               return false;
            } else {
               ItemStack result = ItemStack.EMPTY;
               List<ToolboxBlockEntity> toolboxes = ToolboxHandler.getNearest(player.level(), player, 8);
               if (toolboxes.isEmpty()) {
                  return false;
               } else {
                  if (hitResult.getType() == Type.BLOCK) {
                     BlockPos pos = ((BlockHitResult)hitResult).getBlockPos();
                     BlockState state = level.getBlockState(pos);
                     if (state.isAir()) {
                        return false;
                     }

                     result = state.getCloneItemStack(hitResult, level, pos, player);
                  } else if (hitResult.getType() == Type.ENTITY) {
                     Entity entity = ((EntityHitResult)hitResult).getEntity();
                     result = entity.getPickedResult(hitResult);
                  }

                  if (result.isEmpty()) {
                     return false;
                  } else {
                     for (ToolboxBlockEntity toolboxBlockEntity : toolboxes) {
                        ToolboxInventory inventory = toolboxBlockEntity.inventory;

                        for (int comp = 0; comp < 8; comp++) {
                           ItemStack inSlot = inventory.takeFromCompartment(1, comp, true);
                           if (!inSlot.isEmpty() && inSlot.getItem() == result.getItem() && ItemStack.matches(inSlot, result)) {
                              CatnipServices.NETWORK
                                 .sendToServer(new ToolboxEquipPacket(toolboxBlockEntity.getBlockPos(), comp, player.getInventory().selected));
                              return true;
                           }
                        }
                     }

                     return false;
                  }
               }
            }
         } else {
            return false;
         }
      }
   }

   public static void onKeyInput(int key, boolean pressed) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.gameMode != null && mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
         if (AllKeys.TOOLBELT.doesModifierAndCodeMatch(key)) {
            if (COOLDOWN <= 0) {
               LocalPlayer player = mc.player;
               if (player != null) {
                  Level level = player.level();
                  List<ToolboxBlockEntity> toolboxes = ToolboxHandler.getNearest(player.level(), player, 8);
                  toolboxes.sort(Comparator.comparing(ToolboxBlockEntity::getUniqueId));
                  CompoundTag compound = player.getPersistentData().getCompound("CreateToolboxData");
                  String slotKey = String.valueOf(player.getInventory().selected);
                  boolean equipped = compound.contains(slotKey);
                  if (equipped) {
                     BlockPos pos = NBTHelper.readBlockPos(compound.getCompound(slotKey), "Pos");
                     double max = ToolboxHandler.getMaxRange(player);
                     boolean canReachToolbox = ToolboxHandler.distance(player.position(), pos) < max * max;
                     if (canReachToolbox) {
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof ToolboxBlockEntity) {
                           RadialToolboxMenu screen = new RadialToolboxMenu(
                              toolboxes, RadialToolboxMenu.State.SELECT_ITEM_UNEQUIP, (ToolboxBlockEntity)blockEntity
                           );
                           screen.prevSlot(compound.getCompound(slotKey).getInt("Slot"));
                           ScreenOpener.open(screen);
                           return;
                        }
                     }

                     ScreenOpener.open(new RadialToolboxMenu(ImmutableList.of(), RadialToolboxMenu.State.DETACH, null));
                  } else if (!toolboxes.isEmpty()) {
                     if (toolboxes.size() == 1) {
                        ScreenOpener.open(new RadialToolboxMenu(toolboxes, RadialToolboxMenu.State.SELECT_ITEM, toolboxes.get(0)));
                     } else {
                        ScreenOpener.open(new RadialToolboxMenu(toolboxes, RadialToolboxMenu.State.SELECT_BOX, null));
                     }
                  }
               }
            }
         }
      }
   }

   public static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
      int width = guiGraphics.guiWidth();
      int height = guiGraphics.guiHeight();
      Minecraft mc = Minecraft.getInstance();
      if (!mc.options.hideGui && mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
         int x = width / 2 - 90;
         int y = height - 23;
         RenderSystem.enableDepthTest();
         Player player = mc.player;
         CompoundTag persistentData = player.getPersistentData();
         if (persistentData.contains("CreateToolboxData")) {
            CompoundTag compound = player.getPersistentData().getCompound("CreateToolboxData");
            if (!compound.isEmpty()) {
               PoseStack poseStack = guiGraphics.pose();
               poseStack.pushPose();

               for (int slot = 0; slot < 9; slot++) {
                  String key = String.valueOf(slot);
                  if (compound.contains(key)) {
                     BlockPos pos = NBTHelper.readBlockPos(compound.getCompound(key), "Pos");
                     double max = ToolboxHandler.getMaxRange(player);
                     boolean selected = player.getInventory().selected == slot;
                     int offset = selected ? 1 : 0;
                     AllGuiTextures texture = ToolboxHandler.distance(player.position(), pos) < max * max
                        ? (selected ? AllGuiTextures.TOOLBELT_SELECTED_ON : AllGuiTextures.TOOLBELT_HOTBAR_ON)
                        : (selected ? AllGuiTextures.TOOLBELT_SELECTED_OFF : AllGuiTextures.TOOLBELT_HOTBAR_OFF);
                     texture.render(guiGraphics, x + 20 * slot - offset, y + offset);
                  }
               }

               poseStack.popPose();
            }
         }
      }
   }
}
