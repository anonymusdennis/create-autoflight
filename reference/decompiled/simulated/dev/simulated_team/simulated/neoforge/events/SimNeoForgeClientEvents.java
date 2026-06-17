package dev.simulated_team.simulated.neoforge.events;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterItemBindHandler;
import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.index.SimClickInteractions;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimKeys;
import dev.simulated_team.simulated.neoforge.service.SimpleResourceManagerRegistryService;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.client.event.InputEvent.Key;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

@EventBusSubscriber(
   modid = "simulated",
   value = {Dist.CLIENT}
)
public class SimNeoForgeClientEvents {
   @SubscribeEvent
   public static void preClientTick(Pre event) {
      SimulatedCommonClientEvents.preClientTick(Minecraft.getInstance());
   }

   @SubscribeEvent
   public static void postClientTick(Post event) {
      SimulatedCommonClientEvents.postClientTick(Minecraft.getInstance());
   }

   @SubscribeEvent
   public static void postRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
      SimulatedCommonClientEvents.renderOverlays(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
   }

   @SubscribeEvent
   public static void keyInput(Key event) {
      SimulatedCommonClientEvents.onAfterKeyPress(event.getKey(), event.getScanCode(), event.getAction(), event.getModifiers());
   }

   @SubscribeEvent
   public static void postMouseButtonInput(net.neoforged.neoforge.client.event.InputEvent.MouseButton.Post event) {
      SimulatedCommonClientEvents.onAfterMouseInput(event.getButton(), event.getModifiers(), event.getAction());
   }

   @SubscribeEvent
   public static void playerInteractRightClickBlock(RightClickBlock event) {
      if (event.getEntity().isLocalPlayer()) {
         InteractionResult res = SimulatedCommonClientEvents.onRightClickBlock(event.getEntity(), event.getHand(), event.getPos(), event.getHitVec());
         if (res != null) {
            event.setCancellationResult(res);
            event.setCanceled(true);
            return;
         }
      }

      if (event.getItemStack().is(SimItems.HONEY_GLUE)) {
         event.setUseBlock(TriState.FALSE);
         if (event.getLevel().isClientSide) {
            SimClickInteractions.HONEY_GLUE_MANAGER.selectPos(event.getPos(), event.getEntity(), event.getItemStack());
         }

         event.setCancellationResult(InteractionResult.SUCCESS);
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void itemTooltip(ItemTooltipEvent event) {
      SimulatedCommonClientEvents.appendTooltip(event.getItemStack(), event.getFlags(), event.getEntity(), event.getToolTip());
   }

   @EventBusSubscriber(
      modid = "simulated",
      value = {Dist.CLIENT},
      bus = Bus.MOD
   )
   public static class ModBusEvents {
      @SubscribeEvent
      public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
         SimKeys.registerTo(event::register);
      }

      @SubscribeEvent
      public static void registerGuiLayers(RegisterGuiLayersEvent event) {
         event.registerAbove(VanillaGuiLayers.HOTBAR, Simulated.path("linked_typewriter_binding"), LinkedTypewriterItemBindHandler.OVERLAY);
      }

      @SubscribeEvent
      public static void addReloadListener(RegisterClientReloadListenersEvent event) {
         for (PreparableReloadListener listener : SimpleResourceManagerRegistryService.LISTENERS) {
            event.registerReloadListener(listener);
         }
      }
   }
}
