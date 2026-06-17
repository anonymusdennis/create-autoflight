package dev.simulated_team.simulated.index;

import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.ponder.new_ponder_tooltip.NewPonderTooltipManager;
import dev.simulated_team.simulated.ponder.scenes.AugerShaftScenes;
import dev.simulated_team.simulated.ponder.scenes.DockingConnectorScenes;
import dev.simulated_team.simulated.ponder.scenes.HoneyGlueScenes;
import dev.simulated_team.simulated.ponder.scenes.KineticScenes;
import dev.simulated_team.simulated.ponder.scenes.PhysicsAssemblerScenes;
import dev.simulated_team.simulated.ponder.scenes.RedstoneScenes;
import dev.simulated_team.simulated.ponder.scenes.RopeScenes;
import dev.simulated_team.simulated.ponder.scenes.SensorScenes;
import dev.simulated_team.simulated.ponder.scenes.SwivelBearingScenes;
import dev.simulated_team.simulated.ponder.scenes.SymmetricSailScenes;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SimPonderScenes {
   public static void register(PonderSceneRegistrationHelper<ResourceLocation> registry) {
      PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registry.withKeyFunction(DeferredHolder::getId);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.PHYSICS_ASSEMBLER})
         .addStoryBoard("physics_assembler/intro", PhysicsAssemblerScenes::physicsAssemblerIntro)
         .addStoryBoard("physics_assembler/simulated_contraptions", PhysicsAssemblerScenes::physicsAssemblerSimulatedContraptions)
         .addStoryBoard("physics_assembler/block_properties", PhysicsAssemblerScenes::physicsAssemblerBlockProperties)
         .addStoryBoard("physics_assembler/sub_level_splitting", PhysicsAssemblerScenes::physicsAssemblerSubLevelSplitting);
      helper.forComponents(new ItemProviderEntry[]{vanillaItemProvider("slime_ball")})
         .addStoryBoard("physics_assembler/sub_level_splitting", PhysicsAssemblerScenes::physicsAssemblerSubLevelSplitting);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.SWIVEL_BEARING})
         .addStoryBoard("swivel_bearing/intro", SwivelBearingScenes::swivelBearingIntro)
         .addStoryBoard("swivel_bearing/unlocking", SwivelBearingScenes::swivelBearingUnlocking)
         .addStoryBoard("swivel_bearing/passthrough", SwivelBearingScenes::swivelBearingPassthrough);
      helper.forComponents(new ItemProviderEntry[]{AllBlocks.SAIL}).addStoryBoard("symmetric_sail/main", SymmetricSailScenes::symmetricSailMain);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.WHITE_SYMMETRIC_SAIL})
         .addStoryBoard("symmetric_sail/main", SymmetricSailScenes::symmetricSailMain)
         .addStoryBoard("symmetric_sail/windmill", SymmetricSailScenes::symmetricSailWindmill);
      NewPonderTooltipManager.forItems(AllBlocks.SAIL.asItem()).addScenes(Simulated.path("symmetric_sail"));
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.ROPE_CONNECTOR, SimBlocks.ROPE_WINCH, SimItems.ROPE_COUPLING})
         .addStoryBoard("rope", RopeScenes::ropeIntro)
         .addStoryBoard("rope", RopeScenes::ropeConnections);
      helper.forComponents(new ItemProviderEntry[]{AllBlocks.NOZZLE, AllBlocks.ENCASED_FAN}).addStoryBoard("nozzle", KineticScenes::nozzle);
      NewPonderTooltipManager.forItems(AllBlocks.NOZZLE.asItem(), AllBlocks.ENCASED_FAN.asItem()).addScenes(Simulated.path("nozzle"));
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.DOCKING_CONNECTOR}).addStoryBoard("docking_connector", DockingConnectorScenes::DockingConnector);
      helper.forComponents(SimBlocks.PORTABLE_ENGINES).addStoryBoard("portable_engine", KineticScenes::portableEngine);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.DIRECTIONAL_GEARSHIFT})
         .addStoryBoard("directional_gearshift", KineticScenes::directionalGearshift);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.ANALOG_TRANSMISSION}).addStoryBoard("analog_transmission", KineticScenes::analogTransmission);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.STEERING_WHEEL})
         .addStoryBoard("steering_wheel/intro", KineticScenes::steeringWheelIntro)
         .addStoryBoard("steering_wheel/comparator", KineticScenes::steeringWheelComparator);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.AUGER_SHAFT, SimBlocks.AUGER_COG})
         .addStoryBoard("auger_shaft/intro", AugerShaftScenes::augerShaftIntro)
         .addStoryBoard("auger_shaft/extracting", AugerShaftScenes::augerShaftExtracting);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.TORSION_SPRING}).addStoryBoard("torsion_spring", KineticScenes::torsionSpring);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.MODULATING_LINKED_RECEIVER})
         .addStoryBoard("redstone/modulating_receiver", RedstoneScenes::modulatingReceiver);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.DIRECTIONAL_LINKED_RECEIVER})
         .addStoryBoard("redstone/directional_receiver", RedstoneScenes::directionalReceiver);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.REDSTONE_ACCUMULATOR})
         .addStoryBoard("redstone/redstone_accumulator", RedstoneScenes::redstoneAccumulator);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.REDSTONE_INDUCTOR}).addStoryBoard("redstone/redstone_inductor", RedstoneScenes::redstoneInductor);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.REDSTONE_MAGNET}).addStoryBoard("redstone/redstone_magnet", RedstoneScenes::redstoneMagnet);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.THROTTLE_LEVER}).addStoryBoard("redstone/throttle_lever", RedstoneScenes::throttleLever);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.ALTITUDE_SENSOR}).addStoryBoard("sensor/altitude_sensor", SensorScenes::altitudeSensorIntro);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.OPTICAL_SENSOR}).addStoryBoard("sensor/lasers/optical_sensor", SensorScenes::opticalSensor);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.LASER_POINTER, SimBlocks.LASER_SENSOR})
         .addStoryBoard("sensor/lasers/laser_pointer", SensorScenes::lasers);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.GIMBAL_SENSOR}).addStoryBoard("sensor/gimbal_sensor", SensorScenes::gimbalSensor);
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.NAVIGATION_TABLE})
         .addStoryBoard("sensor/navigation_table", SensorScenes::navigationTable, new ResourceLocation[]{SimPonderTags.NAVIGATION_ITEMS});
      helper.forComponents(new ItemProviderEntry[]{SimBlocks.VELOCITY_SENSOR}).addStoryBoard("sensor/velocity_sensor", SensorScenes::velocitySensor);
      helper.forComponents(new ItemProviderEntry[]{SimItems.HONEY_GLUE})
         .addStoryBoard("honey_glue/intro", HoneyGlueScenes::honeyGlueIntro)
         .addStoryBoard("honey_glue/super_glue", HoneyGlueScenes::honeyGlueSuperGlue);
   }

   private static ItemProviderEntry<Item, Item> vanillaItemProvider(String id) {
      return new ItemProviderEntry(
         Simulated.getRegistrate(), DeferredHolder.create(ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace(id)))
      );
   }
}
