package dev.eriksonn.aeronautics.neoforge.events;

import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.compat.jei.category.MysteriousItemConversionCategory;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.data.AeroAdvancementTriggers;
import dev.eriksonn.aeronautics.events.AeronauticsCommonEvents;
import dev.eriksonn.aeronautics.index.AeroAdvancements;
import dev.eriksonn.aeronautics.index.AeroArmInteractionPoints;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.eriksonn.aeronautics.index.AeroItems;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.eriksonn.aeronautics.neoforge.data.recipe.AeroProcessingRecipeGen;
import dev.eriksonn.aeronautics.neoforge.index.AeroFluidsNeoForge;
import dev.eriksonn.aeronautics.neoforge.service.NeoForgeAeroConfigService;
import dev.simulated_team.simulated.service.SimPlatformService;
import java.util.concurrent.CompletableFuture;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(
   modid = "aeronautics"
)
public class AeroNeoForgeCommonEvents {
   @SubscribeEvent
   public static void serverStop(ServerStoppedEvent event) {
      AeronauticsCommonEvents.onServerStopped(event.getServer());
   }

   @SubscribeEvent
   public static void postServerTick(Post event) {
      MinecraftServer server = event.getServer();

      for (ServerLevel level : server.getAllLevels()) {
         AeronauticsCommonEvents.onServerTickEnd(level);
      }
   }

   @EventBusSubscriber(
      modid = "aeronautics"
   )
   public static class ModBusEvents {
      @SubscribeEvent
      public static void registerEvent(RegisterEvent event) {
         AeroArmInteractionPoints.init();
         if (event.getRegistry() == BuiltInRegistries.TRIGGER_TYPES) {
            AeroAdvancements.init();
            AeroAdvancementTriggers.register();
            if (SimPlatformService.INSTANCE.isLoaded("jei")) {
               jeiCompat();
            }
         }
      }

      private static void jeiCompat() {
         MysteriousItemConversionCategory.RECIPES
            .add(
               ConversionRecipe.create(
                  ((Item)AeroFluidsNeoForge.LEVITITE_BLEND.getBucket().get()).getDefaultInstance(), AeroBlocks.LEVITITE.asItem().getDefaultInstance()
               )
            );
         MysteriousItemConversionCategory.RECIPES
            .add(
               ConversionRecipe.create(
                  ((Item)AeroFluidsNeoForge.LEVITITE_BLEND.getBucket().get()).getDefaultInstance(),
                  AeroBlocks.PEARLESCENT_LEVITITE.asItem().getDefaultInstance()
               )
            );
         ResourceLocation recipeId = Aeronautics.path("conversion_music_disc_cloud_skipper");
         ConversionRecipe recipe = (ConversionRecipe)((Builder)((Builder)new Builder(ConversionRecipe::new, recipeId)
                  .withItemIngredients(new Ingredient[]{Ingredient.of(AeroTags.ItemTags.CONVERTS_TO_CLOUD_SKIPPER)}))
               .withSingleItemOutput(AeroItems.MUSIC_DISC_CLOUD_SKIPPER.asStack()))
            .build();
         MysteriousItemConversionCategory.RECIPES.add(new RecipeHolder(recipeId, recipe));
      }

      @SubscribeEvent(
         priority = EventPriority.HIGH
      )
      public static void gatherDataHighPriority(GatherDataEvent event) {
         if (event.getMods().contains("aeronautics")) {
            AeroTags.addGenerators();
         }
      }

      @SubscribeEvent
      public static void gatherData(GatherDataEvent event) {
         DataGenerator generator = event.getGenerator();
         PackOutput output = generator.getPackOutput();
         CompletableFuture<Provider> lookupProvider = event.getLookupProvider();
         generator.addProvider(event.includeServer(), new AeroAdvancements(output, lookupProvider));
         generator.addProvider(event.includeServer(), AeroProcessingRecipeGen.registerAll(output, lookupProvider));
         event.addProvider(AeroSoundEvents.REGISTRY.getProvider(output));
      }

      @SubscribeEvent
      public static void commonSetup(FMLCommonSetupEvent event) {
         AeroFluidsNeoForge.registerFluidInteractions();
      }

      @SubscribeEvent
      public static void loadConfig(Loading event) {
         for (ConfigBase config : NeoForgeAeroConfigService.CONFIGS.values()) {
            if (config.specification == event.getConfig().getSpec()) {
               config.onLoad();
            }
         }
      }

      @SubscribeEvent
      public static void reloadConfig(Reloading event) {
         for (ConfigBase config : NeoForgeAeroConfigService.CONFIGS.values()) {
            if (config.specification == event.getConfig().getSpec()) {
               config.onReload();
            }
         }
      }
   }
}
