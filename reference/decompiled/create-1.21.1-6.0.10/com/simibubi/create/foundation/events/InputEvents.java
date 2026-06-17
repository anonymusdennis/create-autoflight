package com.simibubi.create.foundation.events;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.elevator.ElevatorControlsHandler;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchHandler;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorConnectionHandler;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorInteractionHandler;
import com.simibubi.create.content.kinetics.chainConveyor.ChainPackageInteractionHandler;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnectionHandler;
import com.simibubi.create.content.logistics.packagePort.PackagePortTargetSelectionHandler;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler;
import com.simibubi.create.content.trains.TrainHUD;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.content.trains.track.CurvedTrackInteraction;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.client.event.InputEvent.Key;
import net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent;
import net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre;
import net.neoforged.neoforge.common.Tags.Items;

@EventBusSubscriber({Dist.CLIENT})
public class InputEvents {
   @SubscribeEvent
   public static void onKeyInput(Key event) {
      if (Minecraft.getInstance().screen == null) {
         int key = event.getKey();
         boolean pressed = event.getAction() != 0;
         CreateClient.SCHEMATIC_HANDLER.onKeyInput(key, pressed);
         ToolboxHandlerClient.onKeyInput(key, pressed);
         RadialWrenchHandler.onKeyInput(key, pressed);
      }
   }

   @SubscribeEvent
   public static void onMouseScrolled(MouseScrollingEvent event) {
      if (Minecraft.getInstance().screen == null) {
         double delta = event.getScrollDeltaY();
         boolean cancelled = CreateClient.SCHEMATIC_HANDLER.mouseScrolled(delta)
            || CreateClient.SCHEMATIC_AND_QUILL_HANDLER.mouseScrolled(delta)
            || TrainHUD.onScroll(delta)
            || ElevatorControlsHandler.onScroll(delta);
         event.setCanceled(cancelled);
      }
   }

   @SubscribeEvent
   public static void onMouseInput(Pre event) {
      if (Minecraft.getInstance().screen == null) {
         int button = event.getButton();
         boolean pressed = event.getAction() != 0;
         RadialWrenchHandler.onKeyInput(button, pressed);
         if (CreateClient.SCHEMATIC_HANDLER.onMouseInput(button, pressed)) {
            event.setCanceled(true);
         } else if (CreateClient.SCHEMATIC_AND_QUILL_HANDLER.onMouseInput(button, pressed)) {
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public static void onClickInput(InteractionKeyMappingTriggered event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.screen == null) {
         if (CurvedTrackInteraction.onClickInput(event)) {
            event.setCanceled(true);
         } else {
            KeyMapping key = event.getKeyMapping();
            if ((key == mc.options.keyUse || key == mc.options.keyAttack) && CreateClient.GLUE_HANDLER.onMouseInput(key == mc.options.keyAttack)) {
               event.setCanceled(true);
            }

            if (key != mc.options.keyUse || !FactoryPanelConnectionHandler.onRightClick() && !ChainConveyorConnectionHandler.onRightClick()) {
               if (key == mc.options.keyPickItem) {
                  if (ToolboxHandlerClient.onPickItem()) {
                     event.setCanceled(true);
                  }
               } else if (event.isUseItem()) {
                  LinkedControllerClientHandler.deactivateInLectern();
                  TrainRelocator.onClicked(event);
                  if (ChainConveyorInteractionHandler.onUse()) {
                     event.setCanceled(true);
                  } else if (PackagePortTargetSelectionHandler.onUse()) {
                     event.setCanceled(true);
                  } else {
                     if (mc.player != null) {
                        ItemStack itemInHand = mc.player.getItemInHand(event.getHand());
                        if (itemInHand.is(Items.TOOLS_WRENCH)) {
                           return;
                        }

                        if (itemInHand.is(net.minecraft.world.item.Items.CHAIN) || AllBlocks.PACKAGE_FROGPORT.isIn(itemInHand)) {
                           return;
                        }
                     }

                     CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> {
                           if (ChainPackageInteractionHandler.onUse()) {
                              event.setCanceled(true);
                           }
                        });
                  }
               }
            } else {
               event.setCanceled(true);
            }
         }
      }
   }
}
