package com.simibubi.create.infrastructure.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.infrastructure.ponder.scenes.ArmScenes;
import com.simibubi.create.infrastructure.ponder.scenes.BearingScenes;
import com.simibubi.create.infrastructure.ponder.scenes.BeltScenes;
import com.simibubi.create.infrastructure.ponder.scenes.CartAssemblerScenes;
import com.simibubi.create.infrastructure.ponder.scenes.ChainDriveScenes;
import com.simibubi.create.infrastructure.ponder.scenes.ChassisScenes;
import com.simibubi.create.infrastructure.ponder.scenes.ChuteScenes;
import com.simibubi.create.infrastructure.ponder.scenes.CrafterScenes;
import com.simibubi.create.infrastructure.ponder.scenes.DeployerScenes;
import com.simibubi.create.infrastructure.ponder.scenes.DetectorScenes;
import com.simibubi.create.infrastructure.ponder.scenes.DisplayScenes;
import com.simibubi.create.infrastructure.ponder.scenes.EjectorScenes;
import com.simibubi.create.infrastructure.ponder.scenes.ElevatorScenes;
import com.simibubi.create.infrastructure.ponder.scenes.FanScenes;
import com.simibubi.create.infrastructure.ponder.scenes.FunnelScenes;
import com.simibubi.create.infrastructure.ponder.scenes.GantryScenes;
import com.simibubi.create.infrastructure.ponder.scenes.ItemVaultScenes;
import com.simibubi.create.infrastructure.ponder.scenes.KineticsScenes;
import com.simibubi.create.infrastructure.ponder.scenes.MechanicalDrillScenes;
import com.simibubi.create.infrastructure.ponder.scenes.MechanicalSawScenes;
import com.simibubi.create.infrastructure.ponder.scenes.MovementActorScenes;
import com.simibubi.create.infrastructure.ponder.scenes.PistonScenes;
import com.simibubi.create.infrastructure.ponder.scenes.ProcessingScenes;
import com.simibubi.create.infrastructure.ponder.scenes.PulleyScenes;
import com.simibubi.create.infrastructure.ponder.scenes.RedstoneScenes;
import com.simibubi.create.infrastructure.ponder.scenes.RedstoneScenes2;
import com.simibubi.create.infrastructure.ponder.scenes.RollerScenes;
import com.simibubi.create.infrastructure.ponder.scenes.SteamScenes;
import com.simibubi.create.infrastructure.ponder.scenes.TunnelScenes;
import com.simibubi.create.infrastructure.ponder.scenes.fluid.DrainScenes;
import com.simibubi.create.infrastructure.ponder.scenes.fluid.FluidMovementActorScenes;
import com.simibubi.create.infrastructure.ponder.scenes.fluid.FluidTankScenes;
import com.simibubi.create.infrastructure.ponder.scenes.fluid.HosePulleyScenes;
import com.simibubi.create.infrastructure.ponder.scenes.fluid.PipeScenes;
import com.simibubi.create.infrastructure.ponder.scenes.fluid.PumpScenes;
import com.simibubi.create.infrastructure.ponder.scenes.fluid.SpoutScenes;
import com.simibubi.create.infrastructure.ponder.scenes.highLogistics.FactoryGaugeScenes;
import com.simibubi.create.infrastructure.ponder.scenes.highLogistics.FrogAndConveyorScenes;
import com.simibubi.create.infrastructure.ponder.scenes.highLogistics.PackagerScenes;
import com.simibubi.create.infrastructure.ponder.scenes.highLogistics.PostboxScenes;
import com.simibubi.create.infrastructure.ponder.scenes.highLogistics.RepackagerScenes;
import com.simibubi.create.infrastructure.ponder.scenes.highLogistics.RequesterAndShopScenes;
import com.simibubi.create.infrastructure.ponder.scenes.highLogistics.StockLinkScenes;
import com.simibubi.create.infrastructure.ponder.scenes.highLogistics.StockTickerScenes;
import com.simibubi.create.infrastructure.ponder.scenes.highLogistics.TableClothScenes;
import com.simibubi.create.infrastructure.ponder.scenes.trains.TrackObserverScenes;
import com.simibubi.create.infrastructure.ponder.scenes.trains.TrackScenes;
import com.simibubi.create.infrastructure.ponder.scenes.trains.TrainScenes;
import com.simibubi.create.infrastructure.ponder.scenes.trains.TrainSignalScenes;
import com.simibubi.create.infrastructure.ponder.scenes.trains.TrainStationScenes;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

public class AllCreatePonderScenes {
   public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
      PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER = helper.withKeyFunction(DeferredHolder::getId);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.SHAFT})
         .addStoryBoard("shaft/relay", KineticsScenes::shaftAsRelay, new ResourceLocation[]{AllCreatePonderTags.KINETIC_RELAYS});
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.SHAFT, AllBlocks.ANDESITE_ENCASED_SHAFT, AllBlocks.BRASS_ENCASED_SHAFT})
         .addStoryBoard("shaft/encasing", KineticsScenes::shaftsCanBeEncased);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.COGWHEEL})
         .addStoryBoard("cog/small", KineticsScenes::cogAsRelay, new ResourceLocation[]{AllCreatePonderTags.KINETIC_RELAYS})
         .addStoryBoard("cog/speedup", KineticsScenes::cogsSpeedUp)
         .addStoryBoard("cog/encasing", KineticsScenes::cogwheelsCanBeEncased);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.LARGE_COGWHEEL})
         .addStoryBoard("cog/speedup", KineticsScenes::cogsSpeedUp)
         .addStoryBoard("cog/large", KineticsScenes::largeCogAsRelay, new ResourceLocation[]{AllCreatePonderTags.KINETIC_RELAYS})
         .addStoryBoard("cog/encasing", KineticsScenes::cogwheelsCanBeEncased);
      HELPER.forComponents(new ItemProviderEntry[]{AllItems.BELT_CONNECTOR})
         .addStoryBoard("belt/connect", BeltScenes::beltConnector, new ResourceLocation[]{AllCreatePonderTags.KINETIC_RELAYS})
         .addStoryBoard("belt/directions", BeltScenes::directions)
         .addStoryBoard("belt/transport", BeltScenes::transport, new ResourceLocation[]{AllCreatePonderTags.LOGISTICS})
         .addStoryBoard("belt/encasing", BeltScenes::beltsCanBeEncased);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.ANDESITE_CASING, AllBlocks.BRASS_CASING})
         .addStoryBoard("shaft/encasing", KineticsScenes::shaftsCanBeEncased)
         .addStoryBoard("belt/encasing", BeltScenes::beltsCanBeEncased);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.GEARBOX, AllItems.VERTICAL_GEARBOX})
         .addStoryBoard("gearbox", KineticsScenes::gearbox, new ResourceLocation[]{AllCreatePonderTags.KINETIC_RELAYS});
      HELPER.addStoryBoard(AllBlocks.CLUTCH, "clutch", KineticsScenes::clutch, new ResourceLocation[]{AllCreatePonderTags.KINETIC_RELAYS});
      HELPER.addStoryBoard(AllBlocks.GEARSHIFT, "gearshift", KineticsScenes::gearshift, new ResourceLocation[]{AllCreatePonderTags.KINETIC_RELAYS});
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.SEQUENCED_GEARSHIFT}).addStoryBoard("sequenced_gearshift", KineticsScenes::sequencedGearshift);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.ENCASED_FAN})
         .addStoryBoard("fan/direction", FanScenes::direction, new ResourceLocation[]{AllCreatePonderTags.KINETIC_APPLIANCES})
         .addStoryBoard("fan/processing", FanScenes::processing);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.CREATIVE_MOTOR})
         .addStoryBoard("creative_motor", KineticsScenes::creativeMotor, new ResourceLocation[]{AllCreatePonderTags.KINETIC_SOURCES})
         .addStoryBoard("creative_motor_mojang", KineticsScenes::creativeMotorMojang);
      HELPER.addStoryBoard(AllBlocks.WATER_WHEEL, "water_wheel", KineticsScenes::waterWheel, new ResourceLocation[]{AllCreatePonderTags.KINETIC_SOURCES});
      HELPER.addStoryBoard(
         AllBlocks.LARGE_WATER_WHEEL, "large_water_wheel", KineticsScenes::largeWaterWheel, new ResourceLocation[]{AllCreatePonderTags.KINETIC_SOURCES}
      );
      HELPER.addStoryBoard(AllBlocks.HAND_CRANK, "hand_crank", KineticsScenes::handCrank, new ResourceLocation[]{AllCreatePonderTags.KINETIC_SOURCES});
      HELPER.addStoryBoard(
         AllBlocks.COPPER_VALVE_HANDLE, "valve_handle", KineticsScenes::valveHandle, new ResourceLocation[]{AllCreatePonderTags.KINETIC_SOURCES}
      );
      HELPER.forComponents(AllBlocks.DYED_VALVE_HANDLES.toArray()).addStoryBoard("valve_handle", KineticsScenes::valveHandle);
      HELPER.addStoryBoard(
         AllBlocks.ENCASED_CHAIN_DRIVE, "chain_drive/relay", ChainDriveScenes::chainDriveAsRelay, new ResourceLocation[]{AllCreatePonderTags.KINETIC_RELAYS}
      );
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.ENCASED_CHAIN_DRIVE, AllBlocks.ADJUSTABLE_CHAIN_GEARSHIFT})
         .addStoryBoard("chain_drive/gearshift", ChainDriveScenes::adjustableChainGearshift);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.ROTATION_SPEED_CONTROLLER}).addStoryBoard("speed_controller", KineticsScenes::speedController);
      HELPER.addStoryBoard(AllBlocks.SPEEDOMETER, "gauges", KineticsScenes::speedometer, new ResourceLocation[0]);
      HELPER.addStoryBoard(AllBlocks.STRESSOMETER, "gauges", KineticsScenes::stressometer, new ResourceLocation[0]);
      HELPER.addStoryBoard(AllBlocks.MILLSTONE, "millstone", ProcessingScenes::millstone, new ResourceLocation[0]);
      HELPER.addStoryBoard(AllBlocks.CRUSHING_WHEEL, "crushing_wheel", ProcessingScenes::crushingWheels, new ResourceLocation[0]);
      HELPER.addStoryBoard(AllBlocks.MECHANICAL_MIXER, "mechanical_mixer/mixing", ProcessingScenes::mixing, new ResourceLocation[0]);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_PRESS})
         .addStoryBoard("mechanical_press/pressing", ProcessingScenes::pressing)
         .addStoryBoard("mechanical_press/compacting", ProcessingScenes::compacting);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.BASIN})
         .addStoryBoard("basin", ProcessingScenes::basin)
         .addStoryBoard("mechanical_mixer/mixing", ProcessingScenes::mixing)
         .addStoryBoard("mechanical_press/compacting", ProcessingScenes::compacting);
      HELPER.addStoryBoard(AllItems.EMPTY_BLAZE_BURNER, "empty_blaze_burner", ProcessingScenes::emptyBlazeBurner, new ResourceLocation[0]);
      HELPER.addStoryBoard(AllBlocks.BLAZE_BURNER, "blaze_burner", ProcessingScenes::blazeBurner, new ResourceLocation[0]);
      HELPER.addStoryBoard(AllBlocks.DEPOT, "depot", BeltScenes::depot, new ResourceLocation[0]);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.WEIGHTED_EJECTOR})
         .addStoryBoard("weighted_ejector/eject", EjectorScenes::ejector)
         .addStoryBoard("weighted_ejector/split", EjectorScenes::splitY)
         .addStoryBoard("weighted_ejector/redstone", EjectorScenes::redstone);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_CRAFTER})
         .addStoryBoard("mechanical_crafter/setup", CrafterScenes::setup)
         .addStoryBoard("mechanical_crafter/connect", CrafterScenes::connect);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_CRAFTER, AllItems.CRAFTER_SLOT_COVER})
         .addStoryBoard("mechanical_crafter/covers", CrafterScenes::covers);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.ITEM_VAULT})
         .addStoryBoard("item_vault/storage", ItemVaultScenes::storage, new ResourceLocation[]{AllCreatePonderTags.LOGISTICS})
         .addStoryBoard("item_vault/sizes", ItemVaultScenes::sizes);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.CHUTE})
         .addStoryBoard("chute/downward", ChuteScenes::downward, new ResourceLocation[]{AllCreatePonderTags.LOGISTICS})
         .addStoryBoard("chute/upward", ChuteScenes::upward);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.CHUTE, AllBlocks.SMART_CHUTE}).addStoryBoard("chute/smart", ChuteScenes::smart);
      HELPER.addStoryBoard(AllBlocks.BRASS_FUNNEL, "funnels/brass", FunnelScenes::brass, new ResourceLocation[0]);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.ANDESITE_FUNNEL, AllBlocks.BRASS_FUNNEL})
         .addStoryBoard("funnels/intro", FunnelScenes::intro, new ResourceLocation[]{AllCreatePonderTags.LOGISTICS})
         .addStoryBoard("funnels/direction", FunnelScenes::directionality)
         .addStoryBoard("funnels/compat", FunnelScenes::compat)
         .addStoryBoard("funnels/redstone", FunnelScenes::redstone)
         .addStoryBoard("funnels/transposer", FunnelScenes::transposer);
      HELPER.addStoryBoard(AllBlocks.ANDESITE_FUNNEL, "funnels/brass", FunnelScenes::brass, new ResourceLocation[0]);
      HELPER.addStoryBoard(AllBlocks.ANDESITE_TUNNEL, "tunnels/andesite", TunnelScenes::andesite, new ResourceLocation[0]);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.BRASS_TUNNEL})
         .addStoryBoard("tunnels/brass", TunnelScenes::brass)
         .addStoryBoard("tunnels/brass_modes", TunnelScenes::brassModes);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.LINEAR_CHASSIS, AllBlocks.SECONDARY_LINEAR_CHASSIS})
         .addStoryBoard("chassis/linear_group", ChassisScenes::linearGroup, new ResourceLocation[]{AllCreatePonderTags.CONTRAPTION_ASSEMBLY})
         .addStoryBoard("chassis/linear_attachment", ChassisScenes::linearAttachement);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.RADIAL_CHASSIS})
         .addStoryBoard("chassis/radial", ChassisScenes::radial, new ResourceLocation[]{AllCreatePonderTags.CONTRAPTION_ASSEMBLY});
      HELPER.forComponents(new ItemProviderEntry[]{AllItems.SUPER_GLUE})
         .addStoryBoard("super_glue", ChassisScenes::superGlue, new ResourceLocation[]{AllCreatePonderTags.CONTRAPTION_ASSEMBLY});
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.STICKER})
         .addStoryBoard("sticker", RedstoneScenes::sticker, new ResourceLocation[]{AllCreatePonderTags.CONTRAPTION_ASSEMBLY});
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_ARM})
         .addStoryBoard("mechanical_arm/setup", ArmScenes::setup, new ResourceLocation[]{AllCreatePonderTags.ARM_TARGETS})
         .addStoryBoard("mechanical_arm/filter", ArmScenes::filtering)
         .addStoryBoard("mechanical_arm/modes", ArmScenes::modes)
         .addStoryBoard("mechanical_arm/redstone", ArmScenes::redstone);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_PISTON, AllBlocks.STICKY_MECHANICAL_PISTON})
         .addStoryBoard(
            "mechanical_piston/anchor",
            PistonScenes::movement,
            new ResourceLocation[]{AllCreatePonderTags.KINETIC_APPLIANCES, AllCreatePonderTags.MOVEMENT_ANCHOR}
         );
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_PISTON, AllBlocks.STICKY_MECHANICAL_PISTON, AllBlocks.PISTON_EXTENSION_POLE})
         .addStoryBoard("mechanical_piston/piston_pole", PistonScenes::poles);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_PISTON, AllBlocks.STICKY_MECHANICAL_PISTON})
         .addStoryBoard("mechanical_piston/modes", PistonScenes::movementModes);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.ROPE_PULLEY})
         .addStoryBoard(
            "rope_pulley/anchor", PulleyScenes::movement, new ResourceLocation[]{AllCreatePonderTags.KINETIC_APPLIANCES, AllCreatePonderTags.MOVEMENT_ANCHOR}
         )
         .addStoryBoard("rope_pulley/modes", PulleyScenes::movementModes)
         .addStoryBoard("rope_pulley/multi_rope", PulleyScenes::multiRope)
         .addStoryBoard("rope_pulley/attachment", PulleyScenes::attachment);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.ELEVATOR_PULLEY})
         .addStoryBoard("elevator_pulley/elevator", ElevatorScenes::elevator)
         .addStoryBoard("elevator_pulley/multi_rope", ElevatorScenes::multiRope);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.WINDMILL_BEARING})
         .addStoryBoard("windmill_bearing/source", BearingScenes::windmillsAsSource, new ResourceLocation[]{AllCreatePonderTags.KINETIC_SOURCES})
         .addStoryBoard("windmill_bearing/structure", BearingScenes::windmillsAnyStructure, new ResourceLocation[]{AllCreatePonderTags.MOVEMENT_ANCHOR});
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.SAIL}).addStoryBoard("sail", BearingScenes::sail);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.SAIL_FRAME}).addStoryBoard("sail", BearingScenes::sailFrame);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_BEARING})
         .addStoryBoard(
            "mechanical_bearing/anchor",
            BearingScenes::mechanicalBearing,
            new ResourceLocation[]{AllCreatePonderTags.KINETIC_APPLIANCES, AllCreatePonderTags.MOVEMENT_ANCHOR}
         )
         .addStoryBoard("mechanical_bearing/modes", BearingScenes::bearingModes)
         .addStoryBoard("mechanical_bearing/stabilized", BearingScenes::stabilizedBearings, new ResourceLocation[]{AllCreatePonderTags.CONTRAPTION_ACTOR});
      HELPER.addStoryBoard(
         AllBlocks.CLOCKWORK_BEARING,
         "clockwork_bearing",
         BearingScenes::clockwork,
         new ResourceLocation[]{AllCreatePonderTags.KINETIC_APPLIANCES, AllCreatePonderTags.MOVEMENT_ANCHOR}
      );
      HELPER.addStoryBoard(
         AllBlocks.GANTRY_SHAFT,
         "gantry/intro",
         GantryScenes::introForShaft,
         new ResourceLocation[]{AllCreatePonderTags.KINETIC_APPLIANCES, AllCreatePonderTags.MOVEMENT_ANCHOR}
      );
      HELPER.addStoryBoard(
         AllBlocks.GANTRY_CARRIAGE,
         "gantry/intro",
         GantryScenes::introForPinion,
         new ResourceLocation[]{AllCreatePonderTags.KINETIC_APPLIANCES, AllCreatePonderTags.MOVEMENT_ANCHOR}
      );
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.GANTRY_SHAFT, AllBlocks.GANTRY_CARRIAGE})
         .addStoryBoard("gantry/redstone", GantryScenes::redstone)
         .addStoryBoard("gantry/direction", GantryScenes::direction)
         .addStoryBoard("gantry/subgantry", GantryScenes::subgantry);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.CART_ASSEMBLER})
         .addStoryBoard("cart_assembler/anchor", CartAssemblerScenes::anchor, new ResourceLocation[]{AllCreatePonderTags.MOVEMENT_ANCHOR})
         .addStoryBoard("cart_assembler/modes", CartAssemblerScenes::modes)
         .addStoryBoard("cart_assembler/dual", CartAssemblerScenes::dual)
         .addStoryBoard("cart_assembler/rails", CartAssemblerScenes::rails);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.PORTABLE_STORAGE_INTERFACE})
         .addStoryBoard("portable_interface/transfer", MovementActorScenes::psiTransfer, new ResourceLocation[]{AllCreatePonderTags.CONTRAPTION_ACTOR})
         .addStoryBoard("portable_interface/redstone", MovementActorScenes::psiRedstone);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.REDSTONE_CONTACT}).addStoryBoard("redstone_contact", RedstoneScenes::contact);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_SAW})
         .addStoryBoard("mechanical_saw/processing", MechanicalSawScenes::processing, new ResourceLocation[]{AllCreatePonderTags.KINETIC_APPLIANCES})
         .addStoryBoard("mechanical_saw/breaker", MechanicalSawScenes::treeCutting)
         .addStoryBoard("mechanical_saw/contraption", MechanicalSawScenes::contraption, new ResourceLocation[]{AllCreatePonderTags.CONTRAPTION_ACTOR});
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_DRILL})
         .addStoryBoard("mechanical_drill/breaker", MechanicalDrillScenes::breaker, new ResourceLocation[]{AllCreatePonderTags.KINETIC_APPLIANCES})
         .addStoryBoard("mechanical_drill/contraption", MechanicalDrillScenes::contraption, new ResourceLocation[]{AllCreatePonderTags.CONTRAPTION_ACTOR});
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.DEPLOYER})
         .addStoryBoard("deployer/filter", DeployerScenes::filter, new ResourceLocation[]{AllCreatePonderTags.KINETIC_APPLIANCES})
         .addStoryBoard("deployer/modes", DeployerScenes::modes)
         .addStoryBoard("deployer/processing", DeployerScenes::processing)
         .addStoryBoard("deployer/redstone", DeployerScenes::redstone)
         .addStoryBoard("deployer/contraption", DeployerScenes::contraption, new ResourceLocation[]{AllCreatePonderTags.CONTRAPTION_ACTOR});
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_HARVESTER}).addStoryBoard("harvester", MovementActorScenes::harvester);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_PLOUGH}).addStoryBoard("plough", MovementActorScenes::plough);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.CONTRAPTION_CONTROLS})
         .addStoryBoard("contraption_controls", MovementActorScenes::contraptionControls);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_ROLLER})
         .addStoryBoard("mechanical_roller/clear_and_pave", RollerScenes::clearAndPave)
         .addStoryBoard("mechanical_roller/fill", RollerScenes::fill);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.FLUID_PIPE})
         .addStoryBoard("fluid_pipe/flow", PipeScenes::flow, new ResourceLocation[]{AllCreatePonderTags.FLUIDS})
         .addStoryBoard("fluid_pipe/interaction", PipeScenes::interaction)
         .addStoryBoard("fluid_pipe/encasing", PipeScenes::encasing);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.COPPER_CASING}).addStoryBoard("fluid_pipe/encasing", PipeScenes::encasing);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.MECHANICAL_PUMP})
         .addStoryBoard("mechanical_pump/flow", PumpScenes::flow, new ResourceLocation[]{AllCreatePonderTags.FLUIDS, AllCreatePonderTags.KINETIC_APPLIANCES})
         .addStoryBoard("mechanical_pump/speed", PumpScenes::speed);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.FLUID_VALVE})
         .addStoryBoard("fluid_valve", PipeScenes::valve, new ResourceLocation[]{AllCreatePonderTags.FLUIDS, AllCreatePonderTags.KINETIC_APPLIANCES});
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.SMART_FLUID_PIPE})
         .addStoryBoard("smart_pipe", PipeScenes::smart, new ResourceLocation[]{AllCreatePonderTags.FLUIDS});
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.FLUID_TANK})
         .addStoryBoard("fluid_tank/storage", FluidTankScenes::storage, new ResourceLocation[]{AllCreatePonderTags.FLUIDS})
         .addStoryBoard("fluid_tank/sizes", FluidTankScenes::sizes);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.CREATIVE_FLUID_TANK})
         .addStoryBoard(
            "fluid_tank/storage_creative", FluidTankScenes::creative, new ResourceLocation[]{AllCreatePonderTags.FLUIDS, AllCreatePonderTags.CREATIVE}
         )
         .addStoryBoard("fluid_tank/sizes_creative", FluidTankScenes::sizes);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.HOSE_PULLEY})
         .addStoryBoard(
            "hose_pulley/intro", HosePulleyScenes::intro, new ResourceLocation[]{AllCreatePonderTags.FLUIDS, AllCreatePonderTags.KINETIC_APPLIANCES}
         )
         .addStoryBoard("hose_pulley/level", HosePulleyScenes::level)
         .addStoryBoard("hose_pulley/infinite", HosePulleyScenes::infinite);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.SPOUT})
         .addStoryBoard("spout", SpoutScenes::filling, new ResourceLocation[]{AllCreatePonderTags.FLUIDS});
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.ITEM_DRAIN})
         .addStoryBoard("item_drain", DrainScenes::emptying, new ResourceLocation[]{AllCreatePonderTags.FLUIDS});
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.PORTABLE_FLUID_INTERFACE})
         .addStoryBoard(
            "portable_interface/transfer_fluid",
            FluidMovementActorScenes::transfer,
            new ResourceLocation[]{AllCreatePonderTags.FLUIDS, AllCreatePonderTags.CONTRAPTION_ACTOR}
         )
         .addStoryBoard("portable_interface/redstone_fluid", MovementActorScenes::psiRedstone);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.PULSE_EXTENDER}).addStoryBoard("pulse_extender", RedstoneScenes::pulseExtender);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.PULSE_REPEATER}).addStoryBoard("pulse_repeater", RedstoneScenes::pulseRepeater);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.POWERED_LATCH}).addStoryBoard("powered_latch", RedstoneScenes::poweredLatch);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.POWERED_TOGGLE_LATCH}).addStoryBoard("powered_toggle_latch", RedstoneScenes::poweredToggleLatch);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.ANALOG_LEVER}).addStoryBoard("analog_lever", RedstoneScenes::analogLever);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.ORANGE_NIXIE_TUBE}).addStoryBoard("nixie_tube", RedstoneScenes::nixieTube);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.REDSTONE_LINK}).addStoryBoard("redstone_link", RedstoneScenes::redstoneLink);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.ROSE_QUARTZ_LAMP}).addStoryBoard("rose_quartz_lamp", RedstoneScenes2::roseQuartzLamp);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.PULSE_TIMER}).addStoryBoard("pulse_timer", RedstoneScenes2::pulseTimer);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.SMART_OBSERVER}).addStoryBoard("smart_observer", DetectorScenes::smartObserver);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.THRESHOLD_SWITCH}).addStoryBoard("threshold_switch", DetectorScenes::thresholdSwitch);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.CHAIN_CONVEYOR}).addStoryBoard("high_logistics/chain_conveyor", FrogAndConveyorScenes::conveyor);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.PACKAGE_FROGPORT})
         .addStoryBoard("high_logistics/package_frogport", FrogAndConveyorScenes::frogPort);
      HELPER.forComponents(AllBlocks.PACKAGE_POSTBOXES.toArray()).addStoryBoard("high_logistics/package_postbox", PostboxScenes::postbox);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.PACKAGER})
         .addStoryBoard("high_logistics/packager", PackagerScenes::packager)
         .addStoryBoard("high_logistics/packager_address", PackagerScenes::packagerAddress);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.STOCK_LINK}).addStoryBoard("high_logistics/stock_link", StockLinkScenes::stockLink);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.STOCK_TICKER})
         .addStoryBoard("high_logistics/stock_ticker", StockTickerScenes::stockTicker)
         .addStoryBoard("high_logistics/stock_ticker_address", StockTickerScenes::stockTickerAddress);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.REDSTONE_REQUESTER})
         .addStoryBoard("high_logistics/redstone_requester", RequesterAndShopScenes::requester);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.REPACKAGER}).addStoryBoard("high_logistics/repackager", RepackagerScenes::repackager);
      HELPER.forComponents(AllBlocks.TABLE_CLOTHS.toArray()).addStoryBoard("high_logistics/table_cloth", TableClothScenes::tableCloth);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.ANDESITE_TABLE_CLOTH, AllBlocks.BRASS_TABLE_CLOTH, AllBlocks.COPPER_TABLE_CLOTH})
         .addStoryBoard("high_logistics/table_cloth", TableClothScenes::tableCloth);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.FACTORY_GAUGE})
         .addStoryBoard("high_logistics/factory_gauge_restocker", FactoryGaugeScenes::restocker)
         .addStoryBoard("high_logistics/factory_gauge_recipe", FactoryGaugeScenes::recipe)
         .addStoryBoard("high_logistics/factory_gauge_crafting", FactoryGaugeScenes::crafting)
         .addStoryBoard("high_logistics/factory_gauge_links", FactoryGaugeScenes::links);
      HELPER.forComponents(
            (ItemProviderEntry[])TrackMaterial.allBlocks()
               .stream()
               .map(
                  trackSupplier -> new BlockEntry(
                        Create.registrate(), DeferredHolder.create(Registries.BLOCK, BuiltInRegistries.BLOCK.getKey((Block)trackSupplier.get()))
                     )
               )
               .toArray(BlockEntry[]::new)
         )
         .addStoryBoard("train_track/placement", TrackScenes::placement)
         .addStoryBoard("train_track/portal", TrackScenes::portal)
         .addStoryBoard("train_track/chunks", TrackScenes::chunks);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.TRACK_STATION})
         .addStoryBoard("train_station/assembly", TrainStationScenes::assembly)
         .addStoryBoard("train_station/schedule", TrainStationScenes::autoSchedule);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.TRACK_SIGNAL})
         .addStoryBoard("train_signal/placement", TrainSignalScenes::placement)
         .addStoryBoard("train_signal/signaling", TrainSignalScenes::signaling)
         .addStoryBoard("train_signal/redstone", TrainSignalScenes::redstone);
      HELPER.forComponents(new ItemProviderEntry[]{AllItems.SCHEDULE}).addStoryBoard("train_schedule", TrainScenes::schedule);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.TRAIN_CONTROLS}).addStoryBoard("train_controls", TrainScenes::controls);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.TRACK_OBSERVER}).addStoryBoard("train_observer", TrackObserverScenes::observe);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.DISPLAY_LINK})
         .addStoryBoard("display_link", DisplayScenes::link)
         .addStoryBoard("display_link_redstone", DisplayScenes::redstone);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.DISPLAY_BOARD}).addStoryBoard("display_board", DisplayScenes::board);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.STEAM_WHISTLE}).addStoryBoard("steam_whistle", SteamScenes::whistle);
      HELPER.forComponents(new ItemProviderEntry[]{AllBlocks.STEAM_ENGINE}).addStoryBoard("steam_engine", SteamScenes::engine);
   }
}
