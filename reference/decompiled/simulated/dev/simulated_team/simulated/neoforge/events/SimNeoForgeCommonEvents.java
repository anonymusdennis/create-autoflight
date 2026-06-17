package dev.simulated_team.simulated.neoforge.events;

import dev.simulated_team.simulated.command.SimCommand;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysicsData;
import dev.simulated_team.simulated.data.advancements.SimAdvancementTriggers;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.data.neoforge.SimProcessingRecipeGen;
import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.events.SimulatedCommonEvents;
import dev.simulated_team.simulated.index.SimArmInteractions;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.index.neoforge.NeoForgeSimStats;
import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.multiloader.energy.SingleBatteryWrapper;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.neoforge.ContainerWrapper;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import dev.simulated_team.simulated.multiloader.tanks.neoforge.SingleTankWrapper;
import dev.simulated_team.simulated.neoforge.service.NeoForgeSimConfigService;
import dev.simulated_team.simulated.neoforge.service.NeoForgeSimInventoryService;
import dev.simulated_team.simulated.util.hold_interaction.HoldInteractionManager;
import java.util.concurrent.CompletableFuture;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent.UsePhase;
import net.neoforged.neoforge.event.level.ChunkEvent.Load;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(
   modid = "simulated"
)
public class SimNeoForgeCommonEvents {
   @SubscribeEvent
   public static void loadChunk(Load event) {
      SimulatedCommonEvents.onChunkLoad(event.getLevel(), event.getChunk(), event.isNewChunk());
   }

   @SubscribeEvent
   public static void playerLoggedIn(PlayerLoggedInEvent event) {
      Player player = event.getEntity();
      SimulatedCommonEvents.onPlayerLoggedIn(player);
   }

   @SubscribeEvent
   public static void registerCommands(RegisterCommandsEvent event) {
      SimCommand.register(event.getDispatcher(), event.getBuildContext());
   }

   @SubscribeEvent
   public static void serverStopped(ServerStoppedEvent event) {
      SimulatedCommonEvents.onServerStopped(event.getServer());
   }

   @SubscribeEvent
   public static void postServerTick(Post event) {
      MinecraftServer server = event.getServer();

      for (ServerLevel level : server.getAllLevels()) {
         SimulatedCommonEvents.onServerTickEnd(level);
      }
   }

   @SubscribeEvent
   public static void syncDataPack(OnDatapackSyncEvent event) {
      EndSeaPhysicsData.syncDataPacket(packet -> event.getRelevantPlayers().forEach(player -> player.connection.send(packet)));
   }

   @SubscribeEvent
   public static void addReloadListeners(AddReloadListenerEvent event) {
      event.addListener(EndSeaPhysicsData.ReloadListener.INSTANCE);
   }

   @SubscribeEvent
   public static void keyInput(InteractionKeyMappingTriggered event) {
      if (event.isUseItem() && SimulatedCommonClientEvents.useItemMappingTriggered()) {
         event.setCanceled(true);
         event.setSwingHand(false);
      }
   }

   @SubscribeEvent
   public static void useItemOnBlock(UseItemOnBlockEvent event) {
      if (event.getLevel().isClientSide()) {
         if (event.getPlayer() != null
            && event.getUsePhase() == UsePhase.ITEM_AFTER_BLOCK
            && SimulatedCommonClientEvents.useItemOnBlockEvent(event.getLevel(), event.getPlayer(), event.getItemStack(), event.getHand())) {
            event.cancelWithResult(ItemInteractionResult.CONSUME);
         }

         useItemOnBlockClient(event);
      }
   }

   @SubscribeEvent
   public static void rightClickBlock(RightClickBlock event) {
      InteractionResult result = SimulatedCommonEvents.rightClickBlock(event.getLevel(), event.getPos(), event.getEntity(), event.getItemStack());
      if (result != null) {
         event.setCancellationResult(result);
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onLivingEntityUseItem(RightClickItem event) {
      LivingEntity entity = event.getEntity();
      if (entity instanceof Player player && player.isLocalPlayer()) {
         SimulatedCommonClientEvents.useItemOnAirEvent(entity.level(), player, event.getItemStack(), event.getHand());
      }
   }

   private static void useItemOnBlockClient(UseItemOnBlockEvent event) {
      if (event.getPlayer().isLocalPlayer() && HoldInteractionManager.isActive()) {
         event.setCanceled(true);
      }
   }

   @EventBusSubscriber(
      modid = "simulated"
   )
   public static class ModBusEvents {
      @SubscribeEvent
      public static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
         SimulatedCommonEvents.modifyDefaultComponents(event::modify);
      }

      @SubscribeEvent
      public static void register(RegisterEvent event) {
         SimArmInteractions.init();
         if (event.getRegistry() == BuiltInRegistries.TRIGGER_TYPES) {
            SimAdvancements.register();
            SimAdvancementTriggers.register();
         }
      }

      @SubscribeEvent(
         priority = EventPriority.HIGHEST
      )
      public static void gatherDataHighPriority(GatherDataEvent event) {
         if (event.getMods().contains("simulated")) {
            SimTags.addGenerators();
         }
      }

      @SubscribeEvent
      public static void gatherData(GatherDataEvent event) {
         DataGenerator generator = event.getGenerator();
         PackOutput output = generator.getPackOutput();
         CompletableFuture<Provider> lookupProvider = event.getLookupProvider();
         if (event.includeClient()) {
            event.addProvider(SimSoundEvents.REGISTRY.getProvider(output));
         }

         generator.addProvider(event.includeServer(), new SimAdvancements(output, lookupProvider));
         generator.addProvider(event.includeServer(), SimProcessingRecipeGen.registerAll(output, lookupProvider));
      }

      @SubscribeEvent
      public static void registerCapabilities(RegisterCapabilitiesEvent event) {
         for (NeoForgeSimInventoryService.InventoryGetterHolder<? extends BlockEntity> getter : NeoForgeSimInventoryService.inventoryGetters) {
            event.registerBlockEntity(ItemHandler.BLOCK, getter.type(), (be, dir) -> {
               AbstractContainer container = getter.castBlockEntityAndGetInv(be, dir);
               return container == null ? null : new ContainerWrapper<AbstractContainer>(container);
            });
         }

         for (NeoForgeSimInventoryService.TankGetterHolder<? extends BlockEntity> getter : NeoForgeSimInventoryService.fluidTankGetters) {
            event.registerBlockEntity(FluidHandler.BLOCK, getter.type(), (be, dir) -> {
               SingleTank container = getter.castBlockEntityAndGetInv(be, dir);
               return container == null ? null : new SingleTankWrapper(container);
            });
         }

         for (NeoForgeSimInventoryService.EnergyGetterHolder<? extends BlockEntity> getter : NeoForgeSimInventoryService.energyGetters) {
            event.registerBlockEntity(EnergyStorage.BLOCK, getter.type(), (be, dir) -> {
               SingleBattery battery = getter.castBlockEntityAndGetInv(be, dir);
               return battery == null ? null : new SingleBatteryWrapper(battery);
            });
         }
      }

      @SubscribeEvent
      public static void loadConfig(Loading event) {
         for (ConfigBase config : NeoForgeSimConfigService.CONFIGS.values()) {
            if (config.specification == event.getConfig().getSpec()) {
               config.onLoad();
            }
         }
      }

      @SubscribeEvent
      public static void reloadConfig(Reloading event) {
         for (ConfigBase config : NeoForgeSimConfigService.CONFIGS.values()) {
            if (config.specification == event.getConfig().getSpec()) {
               config.onReload();
            }
         }
      }

      @SubscribeEvent
      public static void postRegister(FMLLoadCompleteEvent event) {
         NeoForgeSimStats.bootstrap();
      }
   }
}
