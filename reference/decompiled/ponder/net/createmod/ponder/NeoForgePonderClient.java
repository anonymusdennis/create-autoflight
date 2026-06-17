package net.createmod.ponder;

import java.util.Optional;
import java.util.function.Supplier;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.levelWrappers.WrappedClientLevel;
import net.createmod.catnip.placement.PlacementClient;
import net.createmod.catnip.render.StitchedSprite;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.enums.PonderConfig;
import net.createmod.ponder.enums.PonderKeybinds;
import net.createmod.ponder.foundation.PonderTooltipHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Post;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Load;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;

@Mod(
   value = "ponder",
   dist = {Dist.CLIENT}
)
public class NeoForgePonderClient {
   public NeoForgePonderClient(IEventBus modEventBus) {
      modEventBus.addListener(NeoForgePonderClient::init);
   }

   public static void init(FMLClientSetupEvent event) {
      PonderClient.init();
   }

   @EventBusSubscriber({Dist.CLIENT})
   public static class ClientEvents {
      @SubscribeEvent
      public static void onTickPre(Pre event) {
         PonderClient.onTick();
         PonderTooltipHandler.tick();
      }

      @SubscribeEvent
      public static void onRenderWorld(RenderLevelStageEvent event) {
         if (event.getStage() == Stage.AFTER_PARTICLES) {
            PonderClient.onRenderWorld(event.getPoseStack());
         }
      }

      @SubscribeEvent
      public static void onLoadWorld(Load event) {
         LevelAccessor level = event.getLevel();
         if (level.isClientSide()) {
            if (level instanceof ClientLevel && !(level instanceof WrappedClientLevel)) {
               PonderClient.invalidateRenderers();
               AnimationTickHolder.reset();
            }
         }
      }

      @SubscribeEvent
      public static void onUnloadWorld(Unload event) {
         if (event.getLevel().isClientSide()) {
            PonderClient.invalidateRenderers();
            AnimationTickHolder.reset();
         }
      }

      @SubscribeEvent
      public static void afterRenderOverlayLayer(Post event) {
         if (event.getName() == VanillaGuiLayers.CROSSHAIR) {
            PlacementClient.onRenderCrosshairOverlay(event.getGuiGraphics(), AnimationTickHolder.getPartialTicksUI());
         }
      }

      @SubscribeEvent
      public static void onRenderTooltipColor(net.neoforged.neoforge.client.event.RenderTooltipEvent.Color event) {
         Optional<Couple<Color>> colors = PonderTooltipHandler.handleTooltipColor(event.getItemStack());
         if (!colors.isEmpty()) {
            event.setBorderStart(colors.get().getFirst().getRGB());
            event.setBorderEnd(colors.get().getSecond().getRGB());
         }
      }

      @SubscribeEvent
      public static void onItemTooltip(ItemTooltipEvent event) {
         PonderTooltipHandler.addToTooltip(event.getToolTip(), event.getItemStack());
      }
   }

   @EventBusSubscriber(
      value = {Dist.CLIENT},
      bus = Bus.MOD
   )
   public static class ModBusClientEvents {
      @SubscribeEvent
      public static void loadCompleted(FMLLoadCompleteEvent event) {
         PonderClient.modLoadCompleted();
         ModContainer modContainer = (ModContainer)ModList.get()
            .getModContainerById("ponder")
            .orElseThrow(() -> new IllegalStateException("Ponder Mod Container missing after loadCompleted"));
         Supplier<IConfigScreenFactory> configScreen = () -> (mc, previousScreen) -> new BaseConfigScreen(previousScreen, "ponder");
         modContainer.registerExtensionPoint(IConfigScreenFactory.class, configScreen);
         BaseConfigScreen.setDefaultActionFor(
            "ponder", base -> base.withButtonLabels("Client Settings", null, null).withSpecs(PonderConfig.client().specification, null, null)
         );
      }

      @SubscribeEvent
      public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
         event.registerReloadListener(PonderClient.RESOURCE_RELOAD_LISTENER);
      }

      @SubscribeEvent
      public static void onTextureStitchPost(TextureAtlasStitchedEvent event) {
         StitchedSprite.onTextureStitchPost(event.getAtlas());
      }

      @SubscribeEvent
      public static void register(RegisterKeyMappingsEvent event) {
         PonderKeybinds.register(event::register);
      }
   }
}
