package dev.eriksonn.aeronautics.index;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.ponder.scenes.AirPressureScenes;
import dev.eriksonn.aeronautics.content.ponder.scenes.HotAirScenes;
import dev.eriksonn.aeronautics.content.ponder.scenes.LevititeScenes;
import dev.eriksonn.aeronautics.content.ponder.scenes.MountedPotatoCannonScenes;
import dev.eriksonn.aeronautics.content.ponder.scenes.PropellerScenes;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

public class AeroPonderScenes {
   public static void register(PonderSceneRegistrationHelper<ResourceLocation> registry) {
      PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registry.withKeyFunction(DeferredHolder::getId);
      helper.forComponents(new ItemProviderEntry[]{aeroItemProvider("levitite_blend_bucket")})
         .addStoryBoard("levitite/blend", LevititeScenes::levititeBlend)
         .addStoryBoard("levitite/levitite", (builder, util) -> LevititeScenes.levitite(builder, util, false));
      helper.forComponents(new ItemProviderEntry[]{AeroBlocks.LEVITITE})
         .addStoryBoard("levitite/levitite", (builder, util) -> LevititeScenes.levitite(builder, util, false))
         .addStoryBoard("levitite/blend", LevititeScenes::levititeBlend);
      helper.forComponents(new ItemProviderEntry[]{AeroBlocks.PEARLESCENT_LEVITITE})
         .addStoryBoard("levitite/levitite", (builder, util) -> LevititeScenes.levitite(builder, util, true))
         .addStoryBoard("levitite/blend", LevititeScenes::levititeBlend);
      helper.forComponents(new ItemProviderEntry[]{AeroBlocks.HOT_AIR_BURNER})
         .addStoryBoard("hot_air/adjustable_burner", (scene, util) -> HotAirScenes.hotAirBurner(scene, util, false))
         .addStoryBoard("airpressure/burner", (builder, util) -> AirPressureScenes.airPressure(builder, util, new AirPressureScenes.Burner()));
      helper.forComponents(new ItemProviderEntry[]{AeroBlocks.STEAM_VENT})
         .addStoryBoard("hot_air/steam_vent", (scene, util) -> HotAirScenes.hotAirBurner(scene, util, true))
         .addStoryBoard("airpressure/vent", (builder, util) -> AirPressureScenes.airPressure(builder, util, new AirPressureScenes.Vent()));
      helper.forComponents(AeroBlocks.DYED_ENVELOPE_BLOCKS).addStoryBoard("hot_air/envelope", HotAirScenes::envelope);
      helper.forComponents(new ItemProviderEntry[]{AeroBlocks.PROPELLER_BEARING})
         .addStoryBoard("propeller_bearing/thrust", (builder, util) -> PropellerScenes.propellerBearingThrust(builder, util, false))
         .addStoryBoard("propeller_bearing/size", PropellerScenes::propellerBearingSize)
         .addStoryBoard("airpressure/propeller", (builder, util) -> AirPressureScenes.airPressure(builder, util, new AirPressureScenes.PropellerBearing()));
      helper.forComponents(new ItemProviderEntry[]{AeroBlocks.GYROSCOPIC_PROPELLER_BEARING})
         .addStoryBoard("gyroscopic_propeller_bearing/helicopter", PropellerScenes::gyroBearingStabilize)
         .addStoryBoard("gyroscopic_propeller_bearing/island", PropellerScenes::gyroBearingIsland)
         .addStoryBoard("gyroscopic_propeller_bearing/redstone", PropellerScenes::gyroBearingRedstone)
         .addStoryBoard("airpressure/gyro", (builder, util) -> AirPressureScenes.airPressure(builder, util, new AirPressureScenes.GyroBearing()));
      helper.forComponents(new ItemProviderEntry[]{AeroBlocks.MOUNTED_POTATO_CANNON})
         .addStoryBoard("mounted_potato_cannon/intro", MountedPotatoCannonScenes::mountedPotatoCannonIntro);
      helper.forComponents(new ItemProviderEntry[]{AeroBlocks.ANDESITE_PROPELLER})
         .addStoryBoard("propeller_bearing/thrust", (builder, util) -> PropellerScenes.propellerBearingThrust(builder, util, true))
         .addStoryBoard("airpressure/miniprop", (builder, util) -> AirPressureScenes.airPressure(builder, util, new AirPressureScenes.Miniprop()));
      helper.forComponents(new ItemProviderEntry[]{AeroBlocks.WOODEN_PROPELLER})
         .addStoryBoard("propeller_bearing/thrust", (builder, util) -> PropellerScenes.propellerBearingThrust(builder, util, true))
         .addStoryBoard("airpressure/miniprop", (builder, util) -> AirPressureScenes.airPressure(builder, util, new AirPressureScenes.Miniprop()));
   }

   private static ItemProviderEntry<Item, Item> aeroItemProvider(String id) {
      return new ItemProviderEntry(Aeronautics.getRegistrate(), DeferredHolder.create(ResourceKey.create(Registries.ITEM, Aeronautics.path(id))));
   }
}
