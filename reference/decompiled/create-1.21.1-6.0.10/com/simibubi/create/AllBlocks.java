package com.simibubi.create;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.behaviour.interaction.ConductorBlockInteractionBehavior;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlock;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovingInteraction;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterBlock;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.simibubi.create.content.contraptions.actors.plough.PloughBlock;
import com.simibubi.create.content.contraptions.actors.plough.PloughMovementBehaviour;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlock;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlock;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlockItem;
import com.simibubi.create.content.contraptions.actors.roller.RollerMovementBehaviour;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatInteractionBehaviour;
import com.simibubi.create.content.contraptions.actors.seat.SeatMovementBehaviour;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsInteractionBehaviour;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsMovementBehaviour;
import com.simibubi.create.content.contraptions.bearing.BlankSailBlockItem;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlock;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlock;
import com.simibubi.create.content.contraptions.bearing.SailBlock;
import com.simibubi.create.content.contraptions.bearing.StabilizedBearingMovementBehaviour;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlock;
import com.simibubi.create.content.contraptions.behaviour.BellMovementBehaviour;
import com.simibubi.create.content.contraptions.chassis.LinearChassisBlock;
import com.simibubi.create.content.contraptions.chassis.RadialChassisBlock;
import com.simibubi.create.content.contraptions.chassis.StickerBlock;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlock;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlock;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockItem;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonHeadBlock;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.contraptions.pulley.PulleyBlock;
import com.simibubi.create.content.decoration.CardboardBlock;
import com.simibubi.create.content.decoration.MetalLadderBlock;
import com.simibubi.create.content.decoration.MetalScaffoldingBlock;
import com.simibubi.create.content.decoration.RoofBlockCTBehaviour;
import com.simibubi.create.content.decoration.TrainTrapdoorBlock;
import com.simibubi.create.content.decoration.TrapdoorCTBehaviour;
import com.simibubi.create.content.decoration.bracket.BracketBlock;
import com.simibubi.create.content.decoration.bracket.BracketBlockItem;
import com.simibubi.create.content.decoration.bracket.BracketGenerator;
import com.simibubi.create.content.decoration.copycat.CopycatBarsModel;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import com.simibubi.create.content.decoration.copycat.CopycatPanelModel;
import com.simibubi.create.content.decoration.copycat.CopycatStepBlock;
import com.simibubi.create.content.decoration.copycat.CopycatStepModel;
import com.simibubi.create.content.decoration.copycat.SpecialCopycatPanelBlockState;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.decoration.girder.ConnectedGirderModel;
import com.simibubi.create.content.decoration.girder.GirderBlock;
import com.simibubi.create.content.decoration.girder.GirderBlockStateGenerator;
import com.simibubi.create.content.decoration.girder.GirderEncasedShaftBlock;
import com.simibubi.create.content.decoration.placard.PlacardBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleExtenderBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleGenerator;
import com.simibubi.create.content.equipment.armor.BacktankBlock;
import com.simibubi.create.content.equipment.bell.HauntedBellBlock;
import com.simibubi.create.content.equipment.bell.HauntedBellMovementBehaviour;
import com.simibubi.create.content.equipment.bell.PeculiarBellBlock;
import com.simibubi.create.content.equipment.clipboard.ClipboardBlock;
import com.simibubi.create.content.equipment.clipboard.ClipboardBlockItem;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlock;
import com.simibubi.create.content.fluids.PipeAttachmentModel;
import com.simibubi.create.content.fluids.drain.ItemDrainBlock;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyBlock;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeGenerator;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.spout.SpoutBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankGenerator;
import com.simibubi.create.content.fluids.tank.FluidTankItem;
import com.simibubi.create.content.fluids.tank.FluidTankModel;
import com.simibubi.create.content.fluids.tank.FluidTankMovementBehavior;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltGenerator;
import com.simibubi.create.content.kinetics.belt.BeltModel;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlock;
import com.simibubi.create.content.kinetics.chainDrive.ChainDriveBlock;
import com.simibubi.create.content.kinetics.chainDrive.ChainDriveGenerator;
import com.simibubi.create.content.kinetics.chainDrive.ChainGearshiftBlock;
import com.simibubi.create.content.kinetics.clock.CuckooClockBlock;
import com.simibubi.create.content.kinetics.crafter.CrafterCTBehaviour;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerMovementBehaviour;
import com.simibubi.create.content.kinetics.deployer.DeployerMovingInteraction;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.drill.DrillMovementBehaviour;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.NozzleBlock;
import com.simibubi.create.content.kinetics.flywheel.FlywheelBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import com.simibubi.create.content.kinetics.gauge.GaugeGenerator;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmItem;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlock;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorGenerator;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlock;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawGenerator;
import com.simibubi.create.content.kinetics.saw.SawMovementBehaviour;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockModel;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogCTBehaviour;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.transmission.ClutchBlock;
import com.simibubi.create.content.kinetics.transmission.GearshiftBlock;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlock;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftGenerator;
import com.simibubi.create.content.kinetics.turntable.TurntableBlock;
import com.simibubi.create.content.kinetics.waterwheel.LargeWaterWheelBlock;
import com.simibubi.create.content.kinetics.waterwheel.LargeWaterWheelBlockItem;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlock;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelStructuralBlock;
import com.simibubi.create.content.logistics.chute.ChuteBlock;
import com.simibubi.create.content.logistics.chute.ChuteGenerator;
import com.simibubi.create.content.logistics.chute.ChuteItem;
import com.simibubi.create.content.logistics.chute.SmartChuteBlock;
import com.simibubi.create.content.logistics.crate.CreativeCrateBlock;
import com.simibubi.create.content.logistics.depot.DepotBlock;
import com.simibubi.create.content.logistics.depot.EjectorBlock;
import com.simibubi.create.content.logistics.depot.EjectorItem;
import com.simibubi.create.content.logistics.depot.MountedDepotInteractionBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockItem;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelModel;
import com.simibubi.create.content.logistics.funnel.AndesiteFunnelBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelGenerator;
import com.simibubi.create.content.logistics.funnel.BrassFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelGenerator;
import com.simibubi.create.content.logistics.funnel.FunnelItem;
import com.simibubi.create.content.logistics.funnel.FunnelMovementBehaviour;
import com.simibubi.create.content.logistics.itemHatch.ItemHatchBlock;
import com.simibubi.create.content.logistics.packagePort.PackagePortItem;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlock;
import com.simibubi.create.content.logistics.packagePort.postbox.PostboxBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlock;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlock;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkGenerator;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlock;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockItem;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlock;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlock;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelCTBehaviour;
import com.simibubi.create.content.logistics.vault.ItemVaultBlock;
import com.simibubi.create.content.logistics.vault.ItemVaultCTBehaviour;
import com.simibubi.create.content.logistics.vault.ItemVaultItem;
import com.simibubi.create.content.materials.ExperienceBlock;
import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.basin.BasinGenerator;
import com.simibubi.create.content.processing.basin.BasinMovementBehaviour;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockItem;
import com.simibubi.create.content.processing.burner.BlazeBurnerMovementBehaviour;
import com.simibubi.create.content.processing.burner.LitBlazeBurnerBlock;
import com.simibubi.create.content.redstone.RoseQuartzLampBlock;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlock;
import com.simibubi.create.content.redstone.contact.ContactMovementBehaviour;
import com.simibubi.create.content.redstone.contact.RedstoneContactBlock;
import com.simibubi.create.content.redstone.contact.RedstoneContactItem;
import com.simibubi.create.content.redstone.deskBell.DeskBellBlock;
import com.simibubi.create.content.redstone.diodes.AbstractDiodeGenerator;
import com.simibubi.create.content.redstone.diodes.BrassDiodeBlock;
import com.simibubi.create.content.redstone.diodes.BrassDiodeGenerator;
import com.simibubi.create.content.redstone.diodes.PoweredLatchBlock;
import com.simibubi.create.content.redstone.diodes.PoweredLatchGenerator;
import com.simibubi.create.content.redstone.diodes.ToggleLatchBlock;
import com.simibubi.create.content.redstone.diodes.ToggleLatchGenerator;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockItem;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkGenerator;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlock;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlock;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeGenerator;
import com.simibubi.create.content.redstone.rail.ControllerRailBlock;
import com.simibubi.create.content.redstone.rail.ControllerRailGenerator;
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlock;
import com.simibubi.create.content.redstone.smartObserver.SmartObserverGenerator;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlock;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchGenerator;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlock;
import com.simibubi.create.content.schematics.table.SchematicTableBlock;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlock;
import com.simibubi.create.content.trains.display.FlapDisplayBlock;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.observer.TrackObserverBlock;
import com.simibubi.create.content.trains.signal.SignalBlock;
import com.simibubi.create.content.trains.station.StationBlock;
import com.simibubi.create.content.trains.track.FakeTrackBlock;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockItem;
import com.simibubi.create.content.trains.track.TrackBlockStateGenerator;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackModel;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem;
import com.simibubi.create.foundation.block.CopperBlockSet;
import com.simibubi.create.foundation.block.DyedBlockList;
import com.simibubi.create.foundation.block.ItemUseOverrides;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.block.render.ReducedDestroyEffects;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.MetalBarsGen;
import com.simibubi.create.foundation.data.ModelGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.simibubi.create.foundation.data.recipe.CommonMetal;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.UncontainableBlockItem;
import com.simibubi.create.foundation.mixin.accessor.BlockLootSubProviderAccessor;
import com.simibubi.create.foundation.utility.DyeHelper;
import com.simibubi.create.infrastructure.config.CStress;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import java.util.Map;
import net.createmod.catnip.data.Couple;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTable.Builder;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction.NameSource;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.util.DeferredSoundType;

public class AllBlocks {
   private static final CreateRegistrate REGISTRATE = Create.registrate();
   public static final BlockEntry<SchematicannonBlock> SCHEMATICANNON = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "schematicannon", SchematicannonBlock::new
            )
            .initialProperties(() -> Blocks.DISPENSER)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY))
            .transform(TagGen.pickaxeOnly()))
         .blockstate((ctx, prov) -> prov.simpleBlock((Block)ctx.getEntry(), AssetLookup.partialBaseModel(ctx, prov)))
         .loot(
            (lt, block) -> {
               Builder builder = LootTable.lootTable();
               net.minecraft.world.level.storage.loot.predicates.LootItemCondition.Builder survivesExplosion = ExplosionCondition.survivesExplosion();
               lt.add(
                  block,
                  builder.withPool(
                     LootPool.lootPool()
                        .when(survivesExplosion)
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(
                           LootItem.lootTableItem(((SchematicannonBlock)SCHEMATICANNON.get()).asItem())
                              .apply(CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY).include(AllDataComponents.SCHEMATICANNON_OPTIONS))
                        )
                  )
               );
            }
         )
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<SchematicTableBlock> SCHEMATIC_TABLE = ((BlockBuilder)REGISTRATE.block("schematic_table", SchematicTableBlock::new)
         .initialProperties(() -> Blocks.LECTERN)
         .properties(p -> p.mapColor(MapColor.PODZOL).forceSolidOn())
         .transform(TagGen.axeOrPickaxe()))
      .blockstate((ctx, prov) -> prov.horizontalBlock((Block)ctx.getEntry(), prov.models().getExistingFile(ctx.getId()), 0))
      .simpleItem()
      .register();
   public static final BlockEntry<ShaftBlock> SHAFT = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block("shaft", ShaftBlock::new)
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.METAL).forceSolidOff())
               .transform(CStress.setNoImpact()))
            .transform(TagGen.pickaxeOnly()))
         .blockstate(BlockStateGen.axisBlockProvider(false))
         .onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new)))
      .simpleItem()
      .register();
   public static final BlockEntry<CogWheelBlock> COGWHEEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "cogwheel", CogWheelBlock::small
                  )
                  .initialProperties(SharedProperties::stone)
                  .properties(p -> p.sound(SoundType.WOOD).mapColor(MapColor.DIRT))
                  .transform(CStress.setNoImpact()))
               .transform(TagGen.axeOrPickaxe()))
            .blockstate(BlockStateGen.axisBlockProvider(false))
            .onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new)))
         .item(CogwheelBlockItem::new)
         .build())
      .register();
   public static final BlockEntry<CogWheelBlock> LARGE_COGWHEEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "large_cogwheel", CogWheelBlock::large
                  )
                  .initialProperties(SharedProperties::stone)
                  .properties(p -> p.sound(SoundType.WOOD).mapColor(MapColor.DIRT))
                  .transform(TagGen.axeOrPickaxe()))
               .transform(CStress.setNoImpact()))
            .blockstate(BlockStateGen.axisBlockProvider(false))
            .onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new)))
         .item(CogwheelBlockItem::new)
         .build())
      .register();
   public static final BlockEntry<EncasedShaftBlock> ANDESITE_ENCASED_SHAFT = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "andesite_encased_shaft", p -> new EncasedShaftBlock(p, AllBlocks.ANDESITE_CASING::get)
               )
               .properties(p -> p.mapColor(MapColor.PODZOL))
               .transform(BuilderTransformers.encasedShaft("andesite", () -> AllSpriteShifts.ANDESITE_CASING)))
            .transform(EncasingRegistry.addVariantTo(SHAFT)))
         .transform(TagGen.axeOrPickaxe()))
      .register();
   public static final BlockEntry<EncasedShaftBlock> BRASS_ENCASED_SHAFT = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "brass_encased_shaft", p -> new EncasedShaftBlock(p, AllBlocks.BRASS_CASING::get)
               )
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN))
               .transform(BuilderTransformers.encasedShaft("brass", () -> AllSpriteShifts.BRASS_CASING)))
            .transform(EncasingRegistry.addVariantTo(SHAFT)))
         .transform(TagGen.axeOrPickaxe()))
      .register();
   public static final BlockEntry<EncasedCogwheelBlock> ANDESITE_ENCASED_COGWHEEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "andesite_encased_cogwheel", p -> new EncasedCogwheelBlock(p, false, AllBlocks.ANDESITE_CASING::get)
                  )
                  .properties(p -> p.mapColor(MapColor.PODZOL))
                  .transform(BuilderTransformers.encasedCogwheel("andesite", () -> AllSpriteShifts.ANDESITE_CASING)))
               .transform(EncasingRegistry.addVariantTo(COGWHEEL)))
            .onRegister(
               CreateRegistrate.connectedTextures(
                  () -> new EncasedCogCTBehaviour(
                        AllSpriteShifts.ANDESITE_CASING,
                        Couple.create(AllSpriteShifts.ANDESITE_ENCASED_COGWHEEL_SIDE, AllSpriteShifts.ANDESITE_ENCASED_COGWHEEL_OTHERSIDE)
                     )
               )
            ))
         .transform(TagGen.axeOrPickaxe()))
      .register();
   public static final BlockEntry<EncasedCogwheelBlock> BRASS_ENCASED_COGWHEEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "brass_encased_cogwheel", p -> new EncasedCogwheelBlock(p, false, AllBlocks.BRASS_CASING::get)
                  )
                  .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN))
                  .transform(BuilderTransformers.encasedCogwheel("brass", () -> AllSpriteShifts.BRASS_CASING)))
               .transform(EncasingRegistry.addVariantTo(COGWHEEL)))
            .onRegister(
               CreateRegistrate.connectedTextures(
                  () -> new EncasedCogCTBehaviour(
                        AllSpriteShifts.BRASS_CASING,
                        Couple.create(AllSpriteShifts.BRASS_ENCASED_COGWHEEL_SIDE, AllSpriteShifts.BRASS_ENCASED_COGWHEEL_OTHERSIDE)
                     )
               )
            ))
         .transform(TagGen.axeOrPickaxe()))
      .register();
   public static final BlockEntry<EncasedCogwheelBlock> ANDESITE_ENCASED_LARGE_COGWHEEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "andesite_encased_large_cogwheel", p -> new EncasedCogwheelBlock(p, true, AllBlocks.ANDESITE_CASING::get)
               )
               .properties(p -> p.mapColor(MapColor.PODZOL))
               .transform(BuilderTransformers.encasedLargeCogwheel("andesite", () -> AllSpriteShifts.ANDESITE_CASING)))
            .transform(EncasingRegistry.addVariantTo(LARGE_COGWHEEL)))
         .transform(TagGen.axeOrPickaxe()))
      .register();
   public static final BlockEntry<EncasedCogwheelBlock> BRASS_ENCASED_LARGE_COGWHEEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "brass_encased_large_cogwheel", p -> new EncasedCogwheelBlock(p, true, AllBlocks.BRASS_CASING::get)
               )
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN))
               .transform(BuilderTransformers.encasedLargeCogwheel("brass", () -> AllSpriteShifts.BRASS_CASING)))
            .transform(EncasingRegistry.addVariantTo(LARGE_COGWHEEL)))
         .transform(TagGen.axeOrPickaxe()))
      .register();
   public static final BlockEntry<GearboxBlock> GEARBOX = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                        "gearbox", GearboxBlock::new
                     )
                     .initialProperties(SharedProperties::stone)
                     .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL))
                     .transform(CStress.setNoImpact()))
                  .transform(TagGen.axeOrPickaxe()))
               .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(AllSpriteShifts.ANDESITE_CASING))))
            .onRegister(
               CreateRegistrate.casingConnectivity(
                  (block, cc) -> cc.make(block, AllSpriteShifts.ANDESITE_CASING, (s, f) -> f.getAxis() == s.getValue(GearboxBlock.AXIS))
               )
            ))
         .blockstate((c, p) -> BlockStateGen.axisBlock(c, p, $ -> AssetLookup.partialBaseModel(c, p), true))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<ClutchBlock> CLUTCH = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block("clutch", ClutchBlock::new)
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL))
               .addLayer(() -> RenderType::cutoutMipped)
               .transform(CStress.setNoImpact()))
            .transform(TagGen.axeOrPickaxe()))
         .blockstate((c, p) -> BlockStateGen.axisBlock(c, p, AssetLookup.forPowered(c, p)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<GearshiftBlock> GEARSHIFT = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block("gearshift", GearshiftBlock::new)
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL))
               .addLayer(() -> RenderType::cutoutMipped)
               .transform(CStress.setNoImpact()))
            .transform(TagGen.axeOrPickaxe()))
         .blockstate((c, p) -> BlockStateGen.axisBlock(c, p, AssetLookup.forPowered(c, p)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<ChainDriveBlock> ENCASED_CHAIN_DRIVE = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "encased_chain_drive", ChainDriveBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL))
               .transform(CStress.setNoImpact()))
            .transform(TagGen.axeOrPickaxe()))
         .blockstate(
            (c, p) -> new ChainDriveGenerator((state, suffix) -> p.models().getExistingFile(p.modLoc("block/" + c.getName() + "/" + suffix))).generate(c, p)
         )
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<ChainGearshiftBlock> ADJUSTABLE_CHAIN_GEARSHIFT = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "adjustable_chain_gearshift", ChainGearshiftBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.noOcclusion().mapColor(MapColor.NETHER))
               .transform(CStress.setNoImpact()))
            .transform(TagGen.axeOrPickaxe()))
         .blockstate(
            (c, p) -> new ChainDriveGenerator(
                     (state, suffix) -> {
                        String powered = state.getValue(ChainGearshiftBlock.POWERED) ? "_powered" : "";
                        return ((BlockModelBuilder)p.models()
                              .withExistingParent(c.getName() + "_" + suffix + powered, p.modLoc("block/encased_chain_drive/" + suffix)))
                           .texture("side", p.modLoc("block/" + c.getName() + powered));
                     }
                  )
                  .generate(c, p)
         )
         .item()
         .model(
            (c, p) -> ((ItemModelBuilder)p.withExistingParent(c.getName(), p.modLoc("block/encased_chain_drive/item")))
                  .texture("side", p.modLoc("block/" + c.getName()))
         )
         .build())
      .register();
   public static final BlockEntry<BeltBlock> BELT = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block("belt", BeltBlock::new)
                  .properties(p -> p.sound(SoundType.WOOL).strength(0.8F).mapColor(MapColor.COLOR_GRAY))
                  .addLayer(() -> RenderType::cutoutMipped)
                  .transform(TagGen.axeOrPickaxe()))
               .blockstate(new BeltGenerator()::generate)
               .transform(CStress.setNoImpact()))
            .transform(DisplaySource.displaySource(AllDisplaySources.ITEM_NAMES)))
         .onRegister(CreateRegistrate.blockModel(() -> BeltModel::new)))
      .clientExtension(() -> () -> new BeltBlock.RenderProperties())
      .register();
   public static final BlockEntry<ChainConveyorBlock> CHAIN_CONVEYOR = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "chain_conveyor", ChainConveyorBlock::new
                  )
                  .initialProperties(SharedProperties::stone)
                  .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL))
                  .transform(TagGen.axeOrPickaxe()))
               .transform(CStress.setImpact(1.0)))
            .transform(CStress.setImpact(1.0)))
         .blockstate((c, p) -> p.simpleBlock((Block)c.getEntry(), AssetLookup.partialBaseModel(c, p)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<CreativeMotorBlock> CREATIVE_MOTOR = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "creative_motor", CreativeMotorBlock::new
                  )
                  .initialProperties(SharedProperties::stone)
                  .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).forceSolidOn())
                  .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
                  .transform(TagGen.pickaxeOnly()))
               .blockstate(new CreativeMotorGenerator()::generate)
               .transform(CStress.setCapacity(16384.0)))
            .onRegister(BlockStressValues.setGeneratorSpeed(256, true)))
         .item()
         .properties(p -> p.rarity(Rarity.EPIC))
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<WaterWheelBlock> WATER_WHEEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "water_wheel", WaterWheelBlock::new
                  )
                  .initialProperties(SharedProperties::wooden)
                  .properties(p -> p.noOcclusion().mapColor(MapColor.DIRT))
                  .transform(TagGen.axeOrPickaxe()))
               .blockstate((c, p) -> BlockStateGen.directionalBlockIgnoresWaterlogged(c, p, s -> AssetLookup.partialBaseModel(c, p)))
               .addLayer(() -> RenderType::cutoutMipped)
               .transform(CStress.setCapacity(32.0)))
            .onRegister(BlockStressValues.setGeneratorSpeed(8)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<LargeWaterWheelBlock> LARGE_WATER_WHEEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "large_water_wheel", LargeWaterWheelBlock::new
                  )
                  .initialProperties(SharedProperties::wooden)
                  .properties(p -> p.noOcclusion().mapColor(MapColor.DIRT))
                  .transform(TagGen.axeOrPickaxe()))
               .blockstate(
                  (c, p) -> BlockStateGen.axisBlock(
                        c,
                        p,
                        s -> s.getValue(LargeWaterWheelBlock.EXTENSION) ? AssetLookup.partialBaseModel(c, p, "extension") : AssetLookup.partialBaseModel(c, p)
                     )
               )
               .transform(CStress.setCapacity(128.0)))
            .onRegister(BlockStressValues.setGeneratorSpeed(4)))
         .item(LargeWaterWheelBlockItem::new)
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<WaterWheelStructuralBlock> WATER_WHEEL_STRUCTURAL = ((BlockBuilder)REGISTRATE.block(
            "water_wheel_structure", WaterWheelStructuralBlock::new
         )
         .initialProperties(SharedProperties::wooden)
         .clientExtension(() -> () -> new WaterWheelStructuralBlock.RenderProperties())
         .blockstate(
            (c, p) -> p.getVariantBuilder((Block)c.get()).forAllStatesExcept(BlockStateGen.mapToAir(p), new Property[]{WaterWheelStructuralBlock.FACING})
         )
         .properties(p -> p.noOcclusion().mapColor(MapColor.DIRT))
         .transform(TagGen.axeOrPickaxe()))
      .lang("Large Water Wheel")
      .register();
   public static final BlockEntry<EncasedFanBlock> ENCASED_FAN = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "encased_fan", EncasedFanBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.PODZOL))
               .blockstate(BlockStateGen.directionalBlockProvider(true))
               .addLayer(() -> RenderType::cutoutMipped)
               .transform(TagGen.axeOrPickaxe()))
            .transform(CStress.setImpact(2.0)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<NozzleBlock> NOZZLE = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("nozzle", NozzleBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY))
            .tag(new TagKey[]{AllTags.AllBlockTags.BRITTLE.tag})
            .transform(TagGen.axeOrPickaxe()))
         .blockstate(BlockStateGen.directionalBlockProvider(true))
         .addLayer(() -> RenderType::cutoutMipped)
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<TurntableBlock> TURNTABLE = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("turntable", TurntableBlock::new)
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.PODZOL))
            .transform(TagGen.axeOrPickaxe()))
         .blockstate((c, p) -> p.simpleBlock((Block)c.getEntry(), AssetLookup.standardModel(c, p)))
         .transform(CStress.setImpact(4.0)))
      .simpleItem()
      .register();
   public static final BlockEntry<HandCrankBlock> HAND_CRANK = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                        "hand_crank", HandCrankBlock::new
                     )
                     .initialProperties(SharedProperties::wooden)
                     .properties(p -> p.mapColor(MapColor.PODZOL))
                     .transform(TagGen.axeOrPickaxe()))
                  .blockstate(BlockStateGen.directionalBlockProvider(true))
                  .transform(CStress.setCapacity(8.0)))
               .onRegister(BlockStressValues.setGeneratorSpeed(32)))
            .tag(new TagKey[]{AllTags.AllBlockTags.BRITTLE.tag})
            .onRegister(ItemUseOverrides::addBlock))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<CuckooClockBlock> CUCKOO_CLOCK = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "cuckoo_clock", CuckooClockBlock::regular
                  )
                  .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
                  .transform(TagGen.axeOrPickaxe()))
               .transform(BuilderTransformers.cuckooClock()))
            .transform(DisplaySource.displaySource(AllDisplaySources.TIME_OF_DAY)))
         .transform(DisplaySource.displaySource(AllDisplaySources.STOPWATCH)))
      .register();
   public static final BlockEntry<CuckooClockBlock> MYSTERIOUS_CUCKOO_CLOCK = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "mysterious_cuckoo_clock", CuckooClockBlock::mysterious
               )
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
               .transform(TagGen.axeOrPickaxe()))
            .transform(BuilderTransformers.cuckooClock()))
         .lang("Cuckoo Clock")
         .onRegisterAfter(Registries.ITEM, c -> ItemDescription.referKey(c, CUCKOO_CLOCK)))
      .register();
   public static final BlockEntry<MillstoneBlock> MILLSTONE = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block("millstone", MillstoneBlock::new)
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.METAL))
               .transform(TagGen.pickaxeOnly()))
            .blockstate((c, p) -> p.simpleBlock((Block)c.getEntry(), AssetLookup.partialBaseModel(c, p)))
            .transform(CStress.setImpact(4.0)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<CrushingWheelBlock> CRUSHING_WHEEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "crushing_wheel", CrushingWheelBlock::new
               )
               .properties(p -> p.mapColor(MapColor.METAL))
               .initialProperties(SharedProperties::stone)
               .properties(Properties::noOcclusion)
               .transform(TagGen.pickaxeOnly()))
            .blockstate((c, p) -> BlockStateGen.axisBlock(c, p, s -> AssetLookup.partialBaseModel(c, p)))
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(CStress.setImpact(8.0)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<CrushingWheelControllerBlock> CRUSHING_WHEEL_CONTROLLER = REGISTRATE.block(
         "crushing_wheel_controller", CrushingWheelControllerBlock::new
      )
      .properties(p -> p.mapColor(MapColor.STONE).noOcclusion().noLootTable().air().noCollission().pushReaction(PushReaction.BLOCK))
      .blockstate(
         (c, p) -> p.getVariantBuilder((Block)c.get()).forAllStatesExcept(BlockStateGen.mapToAir(p), new Property[]{CrushingWheelControllerBlock.FACING})
      )
      .register();
   public static final BlockEntry<MechanicalPressBlock> MECHANICAL_PRESS = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "mechanical_press", MechanicalPressBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL))
               .transform(TagGen.axeOrPickaxe()))
            .blockstate(BlockStateGen.horizontalBlockProvider(true))
            .transform(CStress.setImpact(8.0)))
         .item(AssemblyOperatorBlockItem::new)
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<MechanicalMixerBlock> MECHANICAL_MIXER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "mechanical_mixer", MechanicalMixerBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.noOcclusion().mapColor(MapColor.STONE))
               .transform(TagGen.axeOrPickaxe()))
            .blockstate((c, p) -> p.simpleBlock((Block)c.getEntry(), AssetLookup.partialBaseModel(c, p)))
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(CStress.setImpact(4.0)))
         .item(AssemblyOperatorBlockItem::new)
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<BasinBlock> BASIN = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block("basin", BasinBlock::new)
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.NETHERITE_BLOCK))
               .transform(TagGen.pickaxeOnly()))
            .blockstate(new BasinGenerator()::generate)
            .addLayer(() -> RenderType::cutoutMipped)
            .onRegister(MovementBehaviour.movementBehaviour(new BasinMovementBehaviour())))
         .item()
         .transform(ModelGen.customItemModel("_", "block")))
      .register();
   public static final BlockEntry<BlazeBurnerBlock> BLAZE_BURNER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "blaze_burner", BlazeBurnerBlock::new
                  )
                  .initialProperties(SharedProperties::softMetal)
                  .properties(p -> p.mapColor(MapColor.COLOR_GRAY).lightLevel(BlazeBurnerBlock::getLight))
                  .transform(TagGen.pickaxeOnly()))
               .addLayer(() -> RenderType::cutoutMipped)
               .tag(
                  new TagKey[]{
                     AllTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_BLASTING.tag,
                     AllTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_SMOKING.tag,
                     AllTags.AllBlockTags.FAN_TRANSPARENT.tag,
                     AllTags.AllBlockTags.PASSIVE_BOILER_HEATERS.tag
                  }
               )
               .loot((lt, block) -> lt.add(block, BlazeBurnerBlock.buildLootTable()))
               .blockstate((c, p) -> p.simpleBlock((Block)c.getEntry(), AssetLookup.partialBaseModel(c, p)))
               .onRegister(MovementBehaviour.movementBehaviour(new BlazeBurnerMovementBehaviour())))
            .onRegister(MovingInteractionBehaviour.interactionBehaviour(new ConductorBlockInteractionBehavior.BlazeBurner())))
         .item(BlazeBurnerBlockItem::withBlaze)
         .model(AssetLookup.customBlockItemModel("blaze_burner", "block_with_blaze"))
         .build())
      .register();
   public static final BlockEntry<LitBlazeBurnerBlock> LIT_BLAZE_BURNER = ((BlockBuilder)REGISTRATE.block("lit_blaze_burner", LitBlazeBurnerBlock::new)
         .initialProperties(SharedProperties::softMetal)
         .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).lightLevel(LitBlazeBurnerBlock::getLight))
         .transform(TagGen.pickaxeOnly()))
      .addLayer(() -> RenderType::cutoutMipped)
      .tag(
         new TagKey[]{
            AllTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_HAUNTING.tag,
            AllTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_SMOKING.tag,
            AllTags.AllBlockTags.FAN_TRANSPARENT.tag,
            AllTags.AllBlockTags.PASSIVE_BOILER_HEATERS.tag
         }
      )
      .loot((lt, block) -> lt.dropOther(block, (ItemLike)AllItems.EMPTY_BLAZE_BURNER.get()))
      .blockstate(
         (c, p) -> p.getVariantBuilder((Block)c.get())
               .forAllStates(
                  state -> ConfiguredModel.builder()
                        .modelFile(
                           p.models()
                              .getExistingFile(
                                 p.modLoc(
                                    "block/blaze_burner/"
                                       + (
                                          state.getValue(LitBlazeBurnerBlock.FLAME_TYPE) == LitBlazeBurnerBlock.FlameType.SOUL
                                             ? "block_with_soul_fire"
                                             : "block_with_fire"
                                       )
                                 )
                              )
                        )
                        .build()
               )
      )
      .register();
   public static final BlockEntry<DepotBlock> DEPOT = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                        "depot", DepotBlock::new
                     )
                     .initialProperties(SharedProperties::stone)
                     .properties(p -> p.mapColor(MapColor.COLOR_GRAY))
                     .transform(TagGen.axeOrPickaxe()))
                  .blockstate((c, p) -> p.simpleBlock((Block)c.getEntry(), AssetLookup.partialBaseModel(c, p)))
                  .transform(DisplaySource.displaySource(AllDisplaySources.ITEM_NAMES)))
               .onRegister(MovingInteractionBehaviour.interactionBehaviour(new MountedDepotInteractionBehaviour())))
            .transform(MountedItemStorageType.mountedItemStorage(AllMountedStorageTypes.DEPOT)))
         .item()
         .transform(ModelGen.customItemModel("_", "block")))
      .register();
   public static final BlockEntry<EjectorBlock> WEIGHTED_EJECTOR = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "weighted_ejector", EjectorBlock::new
                  )
                  .initialProperties(SharedProperties::stone)
                  .properties(p -> p.noOcclusion().mapColor(MapColor.COLOR_GRAY))
                  .transform(TagGen.axeOrPickaxe()))
               .blockstate((c, p) -> p.horizontalBlock((Block)c.getEntry(), AssetLookup.partialBaseModel(c, p), 180))
               .transform(CStress.setImpact(2.0)))
            .transform(DisplaySource.displaySource(AllDisplaySources.ITEM_NAMES)))
         .item(EjectorItem::new)
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<ChuteBlock> CHUTE = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("chute", ChuteBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.NETHERITE_BLOCK).noOcclusion().isSuffocating((state, level, pos) -> false))
            .transform(TagGen.pickaxeOnly()))
         .addLayer(() -> RenderType::cutoutMipped)
         .clientExtension(() -> () -> new ReducedDestroyEffects())
         .blockstate(new ChuteGenerator()::generate)
         .item(ChuteItem::new)
         .transform(ModelGen.customItemModel("_", "block")))
      .register();
   public static final BlockEntry<SmartChuteBlock> SMART_CHUTE = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("smart_chute", SmartChuteBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(
               p -> p.mapColor(MapColor.COLOR_GRAY)
                     .sound(SoundType.NETHERITE_BLOCK)
                     .noOcclusion()
                     .isSuffocating((state, level, pos) -> false)
                     .isRedstoneConductor((state, level, pos) -> false)
            )
            .addLayer(() -> RenderType::cutoutMipped)
            .clientExtension(() -> () -> new ReducedDestroyEffects())
            .transform(TagGen.pickaxeOnly()))
         .blockstate((c, p) -> BlockStateGen.simpleBlock(c, p, AssetLookup.forPowered(c, p)))
         .item()
         .transform(ModelGen.customItemModel("_", "block")))
      .register();
   public static final BlockEntry<GaugeBlock> SPEEDOMETER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "speedometer", GaugeBlock::speed
                  )
                  .initialProperties(SharedProperties::wooden)
                  .properties(p -> p.mapColor(MapColor.PODZOL))
                  .transform(TagGen.axeOrPickaxe()))
               .transform(CStress.setNoImpact()))
            .blockstate(new GaugeGenerator()::generate)
            .transform(DisplaySource.displaySource(AllDisplaySources.KINETIC_SPEED)))
         .item()
         .transform(ModelGen.customItemModel("gauge", "_", "item")))
      .register();
   public static final BlockEntry<GaugeBlock> STRESSOMETER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "stressometer", GaugeBlock::stress
                  )
                  .initialProperties(SharedProperties::wooden)
                  .properties(p -> p.mapColor(MapColor.PODZOL))
                  .transform(TagGen.axeOrPickaxe()))
               .transform(CStress.setNoImpact()))
            .blockstate(new GaugeGenerator()::generate)
            .transform(DisplaySource.displaySource(AllDisplaySources.KINETIC_STRESS)))
         .item()
         .transform(ModelGen.customItemModel("gauge", "_", "item")))
      .register();
   public static final BlockEntry<BracketBlock> WOODEN_BRACKET = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("wooden_bracket", BracketBlock::new)
            .blockstate(new BracketGenerator("wooden")::generate)
            .properties(p -> p.sound(SoundType.SCAFFOLDING))
            .transform(TagGen.axeOrPickaxe()))
         .item(BracketBlockItem::new)
         .tag(new TagKey[]{AllTags.AllItemTags.INVALID_FOR_TRACK_PAVING.tag})
         .transform(BracketGenerator.itemModel("wooden")))
      .register();
   public static final BlockEntry<BracketBlock> METAL_BRACKET = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("metal_bracket", BracketBlock::new)
            .blockstate(new BracketGenerator("metal")::generate)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
            .transform(TagGen.pickaxeOnly()))
         .item(BracketBlockItem::new)
         .tag(new TagKey[]{AllTags.AllItemTags.INVALID_FOR_TRACK_PAVING.tag})
         .transform(BracketGenerator.itemModel("metal")))
      .register();
   public static final BlockEntry<FluidPipeBlock> FLUID_PIPE = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block("fluid_pipe", FluidPipeBlock::new)
               .initialProperties(SharedProperties::copperMetal)
               .properties(p -> p.forceSolidOff())
               .transform(TagGen.pickaxeOnly()))
            .blockstate(BlockStateGen.pipe())
            .onRegister(CreateRegistrate.blockModel(() -> PipeAttachmentModel::withAO)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<EncasedPipeBlock> ENCASED_FLUID_PIPE = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                        "encased_fluid_pipe", p -> new EncasedPipeBlock(p, AllBlocks.COPPER_CASING::get)
                     )
                     .initialProperties(SharedProperties::copperMetal)
                     .properties(p -> p.noOcclusion().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY))
                     .transform(TagGen.axeOrPickaxe()))
                  .blockstate(BlockStateGen.encasedPipe())
                  .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(AllSpriteShifts.COPPER_CASING))))
               .onRegister(
                  CreateRegistrate.casingConnectivity(
                     (block, cc) -> cc.make(
                           block, AllSpriteShifts.COPPER_CASING, (s, f) -> !(Boolean)s.getValue((Property)EncasedPipeBlock.FACING_TO_PROPERTY_MAP.get(f))
                        )
                  )
               ))
            .onRegister(CreateRegistrate.blockModel(() -> PipeAttachmentModel::withAO)))
         .loot((p, b) -> p.dropOther(b, (ItemLike)FLUID_PIPE.get()))
         .transform(EncasingRegistry.addVariantTo(FLUID_PIPE)))
      .register();
   public static final BlockEntry<GlassFluidPipeBlock> GLASS_FLUID_PIPE = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "glass_fluid_pipe", GlassFluidPipeBlock::new
            )
            .initialProperties(SharedProperties::copperMetal)
            .properties(p -> p.noOcclusion())
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(TagGen.pickaxeOnly()))
         .blockstate(
            (c, p) -> p.getVariantBuilder((Block)c.getEntry())
                  .forAllStatesExcept(
                     state -> {
                        Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
                        return ConfiguredModel.builder()
                           .modelFile(p.models().getExistingFile(p.modLoc("block/fluid_pipe/window")))
                           .uvLock(false)
                           .rotationX(axis == Axis.Y ? 0 : 90)
                           .rotationY(axis == Axis.X ? 90 : 0)
                           .build();
                     },
                     new Property[]{BlockStateProperties.WATERLOGGED}
                  )
         )
         .onRegister(CreateRegistrate.blockModel(() -> PipeAttachmentModel::withAO)))
      .loot((p, b) -> p.dropOther(b, (ItemLike)FLUID_PIPE.get()))
      .register();
   public static final BlockEntry<PumpBlock> MECHANICAL_PUMP = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "mechanical_pump", PumpBlock::new
                  )
                  .initialProperties(SharedProperties::copperMetal)
                  .properties(p -> p.mapColor(MapColor.STONE))
                  .transform(TagGen.pickaxeOnly()))
               .blockstate(BlockStateGen.directionalBlockProviderIgnoresWaterlogged(true))
               .onRegister(CreateRegistrate.blockModel(() -> PipeAttachmentModel::withAO)))
            .transform(CStress.setImpact(4.0)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<SmartFluidPipeBlock> SMART_FLUID_PIPE = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "smart_fluid_pipe", SmartFluidPipeBlock::new
               )
               .initialProperties(SharedProperties::copperMetal)
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
               .transform(TagGen.pickaxeOnly()))
            .blockstate(new SmartFluidPipeGenerator()::generate)
            .onRegister(CreateRegistrate.blockModel(() -> PipeAttachmentModel::withAO)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<FluidValveBlock> FLUID_VALVE = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "fluid_valve", FluidValveBlock::new
               )
               .initialProperties(SharedProperties::copperMetal)
               .transform(TagGen.pickaxeOnly()))
            .addLayer(() -> RenderType::cutoutMipped)
            .blockstate(
               (c, p) -> BlockStateGen.directionalAxisBlock(
                     c,
                     p,
                     (state, vertical) -> AssetLookup.partialBaseModel(
                           c, p, vertical ? "vertical" : "horizontal", state.getValue(FluidValveBlock.ENABLED) ? "open" : "closed"
                        )
                  )
            )
            .onRegister(CreateRegistrate.blockModel(() -> PipeAttachmentModel::withAO)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<ValveHandleBlock> COPPER_VALVE_HANDLE = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "copper_valve_handle", ValveHandleBlock::copper
               )
               .transform(TagGen.pickaxeOnly()))
            .transform(BuilderTransformers.valveHandle(null)))
         .transform(CStress.setCapacity(8.0)))
      .register();
   public static final DyedBlockList<ValveHandleBlock> DYED_VALVE_HANDLES = new DyedBlockList<>(
      colour -> {
         String colourName = colour.getSerializedName();
         return ((BlockBuilder)((BlockBuilder)REGISTRATE.block(colourName + "_valve_handle", p -> ValveHandleBlock.dyed(p, colour))
                  .properties(p -> p.mapColor(colour.getMapColor()))
                  .transform(TagGen.pickaxeOnly()))
               .transform(BuilderTransformers.valveHandle(colour)))
            .recipe(
               (c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, (ItemLike)c.get())
                     .requires(colour.getTag())
                     .requires(AllTags.AllItemTags.VALVE_HANDLES.tag)
                     .unlockedBy("has_valve", RegistrateRecipeProvider.has(AllTags.AllItemTags.VALVE_HANDLES.tag))
                     .save(p, Create.asResource("crafting/kinetics/" + c.getName() + "_from_other_valve_handle"))
            )
            .register();
      }
   );
   public static final BlockEntry<FluidTankBlock> FLUID_TANK = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                           "fluid_tank", FluidTankBlock::regular
                        )
                        .initialProperties(SharedProperties::copperMetal)
                        .properties(p -> p.noOcclusion().isRedstoneConductor((p1, p2, p3) -> true))
                        .transform(TagGen.pickaxeOnly()))
                     .blockstate(new FluidTankGenerator()::generate)
                     .onRegister(CreateRegistrate.blockModel(() -> FluidTankModel::standard)))
                  .transform(DisplaySource.displaySource(AllDisplaySources.BOILER)))
               .transform(MountedFluidStorageType.mountedFluidStorage(AllMountedStorageTypes.FLUID_TANK)))
            .onRegister(MovementBehaviour.movementBehaviour(new FluidTankMovementBehavior())))
         .addLayer(() -> RenderType::cutoutMipped)
         .item(FluidTankItem::new)
         .model(AssetLookup.customBlockItemModel("_", "block_single_window"))
         .build())
      .register();
   public static final BlockEntry<FluidTankBlock> CREATIVE_FLUID_TANK = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "creative_fluid_tank", FluidTankBlock::creative
                  )
                  .initialProperties(SharedProperties::copperMetal)
                  .properties(p -> p.noOcclusion().mapColor(MapColor.COLOR_PURPLE))
                  .transform(TagGen.pickaxeOnly()))
               .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
               .blockstate(new FluidTankGenerator("creative_")::generate)
               .onRegister(CreateRegistrate.blockModel(() -> FluidTankModel::creative)))
            .transform(MountedFluidStorageType.mountedFluidStorage(AllMountedStorageTypes.CREATIVE_FLUID_TANK)))
         .addLayer(() -> RenderType::cutoutMipped)
         .item(FluidTankItem::new)
         .properties(p -> p.rarity(Rarity.EPIC))
         .model(
            (c, p) -> ((ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)p.withExistingParent(
                                 c.getName(), p.modLoc("block/fluid_tank/block_single_window")
                              ))
                              .texture("5", p.modLoc("block/creative_fluid_tank_window_single")))
                           .texture("1", p.modLoc("block/creative_fluid_tank")))
                        .texture("particle", p.modLoc("block/creative_fluid_tank")))
                     .texture("4", p.modLoc("block/creative_casing")))
                  .texture("0", p.modLoc("block/creative_casing"))
         )
         .build())
      .register();
   public static final BlockEntry<HosePulleyBlock> HOSE_PULLEY = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "hose_pulley", HosePulleyBlock::new
               )
               .initialProperties(SharedProperties::copperMetal)
               .properties(Properties::noOcclusion)
               .addLayer(() -> RenderType::cutoutMipped)
               .transform(TagGen.pickaxeOnly()))
            .blockstate(BlockStateGen.horizontalBlockProvider(true))
            .transform(CStress.setImpact(4.0)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<ItemDrainBlock> ITEM_DRAIN = ((BlockBuilder)REGISTRATE.block("item_drain", ItemDrainBlock::new)
         .initialProperties(SharedProperties::copperMetal)
         .transform(TagGen.pickaxeOnly()))
      .addLayer(() -> RenderType::cutoutMipped)
      .blockstate((c, p) -> p.simpleBlock((Block)c.get(), AssetLookup.standardModel(c, p)))
      .simpleItem()
      .register();
   public static final BlockEntry<SpoutBlock> SPOUT = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("spout", SpoutBlock::new)
            .initialProperties(SharedProperties::copperMetal)
            .transform(TagGen.pickaxeOnly()))
         .blockstate((ctx, prov) -> prov.simpleBlock((Block)ctx.getEntry(), AssetLookup.partialBaseModel(ctx, prov)))
         .addLayer(() -> RenderType::cutoutMipped)
         .item(AssemblyOperatorBlockItem::new)
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<PortableStorageInterfaceBlock> PORTABLE_FLUID_INTERFACE = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "portable_fluid_interface", PortableStorageInterfaceBlock::forFluids
               )
               .initialProperties(SharedProperties::copperMetal)
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_LIGHT_GRAY))
               .transform(TagGen.axeOrPickaxe()))
            .blockstate((c, p) -> p.directionalBlock((Block)c.get(), AssetLookup.partialBaseModel(c, p)))
            .onRegister(MovementBehaviour.movementBehaviour(new PortableStorageInterfaceMovement())))
         .item()
         .tag(new TagKey[]{AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag})
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<SteamEngineBlock> STEAM_ENGINE = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "steam_engine", SteamEngineBlock::new
                  )
                  .initialProperties(SharedProperties::copperMetal)
                  .transform(TagGen.pickaxeOnly()))
               .blockstate((c, p) -> p.horizontalFaceBlock((Block)c.get(), AssetLookup.partialBaseModel(c, p)))
               .transform(CStress.setCapacity(1024.0)))
            .onRegister(BlockStressValues.setGeneratorSpeed(64, true)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<WhistleBlock> STEAM_WHISTLE = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("steam_whistle", WhistleBlock::new)
            .initialProperties(SharedProperties::copperMetal)
            .properties(p -> p.mapColor(MapColor.GOLD))
            .transform(TagGen.pickaxeOnly()))
         .blockstate(new WhistleGenerator()::generate)
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<WhistleExtenderBlock> STEAM_WHISTLE_EXTENSION = ((BlockBuilder)REGISTRATE.block(
            "steam_whistle_extension", WhistleExtenderBlock::new
         )
         .initialProperties(SharedProperties::copperMetal)
         .properties(p -> p.mapColor(MapColor.GOLD).forceSolidOn())
         .transform(TagGen.pickaxeOnly()))
      .blockstate(BlockStateGen.whistleExtender())
      .register();
   public static final BlockEntry<PoweredShaftBlock> POWERED_SHAFT = ((BlockBuilder)REGISTRATE.block("powered_shaft", PoweredShaftBlock::new)
         .initialProperties(SharedProperties::stone)
         .properties(p -> p.mapColor(MapColor.METAL).forceSolidOn())
         .transform(TagGen.pickaxeOnly()))
      .blockstate(BlockStateGen.axisBlockProvider(false))
      .loot((lt, block) -> lt.dropOther(block, (ItemLike)SHAFT.get()))
      .register();
   public static final BlockEntry<MechanicalPistonBlock> MECHANICAL_PISTON = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "mechanical_piston", MechanicalPistonBlock::normal
            )
            .properties(p -> p.mapColor(MapColor.PODZOL))
            .transform(TagGen.axeOrPickaxe()))
         .transform(BuilderTransformers.mechanicalPiston(PistonType.DEFAULT)))
      .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
      .register();
   public static final BlockEntry<MechanicalPistonBlock> STICKY_MECHANICAL_PISTON = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "sticky_mechanical_piston", MechanicalPistonBlock::sticky
            )
            .properties(p -> p.mapColor(MapColor.PODZOL))
            .transform(TagGen.axeOrPickaxe()))
         .transform(BuilderTransformers.mechanicalPiston(PistonType.STICKY)))
      .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
      .register();
   public static final BlockEntry<PistonExtensionPoleBlock> PISTON_EXTENSION_POLE = ((BlockBuilder)REGISTRATE.block(
            "piston_extension_pole", PistonExtensionPoleBlock::new
         )
         .initialProperties(() -> Blocks.PISTON_HEAD)
         .properties(p -> p.sound(SoundType.SCAFFOLDING).mapColor(MapColor.DIRT).forceSolidOn())
         .transform(TagGen.axeOrPickaxe()))
      .blockstate(BlockStateGen.directionalBlockProviderIgnoresWaterlogged(false))
      .simpleItem()
      .register();
   public static final BlockEntry<MechanicalPistonHeadBlock> MECHANICAL_PISTON_HEAD = ((BlockBuilder)REGISTRATE.block(
            "mechanical_piston_head", MechanicalPistonHeadBlock::new
         )
         .initialProperties(() -> Blocks.PISTON_HEAD)
         .properties(p -> p.mapColor(MapColor.DIRT))
         .transform(TagGen.axeOrPickaxe()))
      .loot((p, b) -> p.dropOther(b, (ItemLike)PISTON_EXTENSION_POLE.get()))
      .blockstate(
         (c, p) -> BlockStateGen.directionalBlockIgnoresWaterlogged(
               c,
               p,
               state -> p.models()
                     .getExistingFile(
                        p.modLoc("block/mechanical_piston/" + ((PistonType)state.getValue(MechanicalPistonHeadBlock.TYPE)).getSerializedName() + "/head")
                     )
            )
      )
      .register();
   public static final BlockEntry<GantryCarriageBlock> GANTRY_CARRIAGE = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "gantry_carriage", GantryCarriageBlock::new
            )
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL))
            .transform(TagGen.axeOrPickaxe()))
         .blockstate(BlockStateGen.directionalAxisBlockProvider())
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<GantryShaftBlock> GANTRY_SHAFT = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "gantry_shaft", GantryShaftBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.NETHER).forceSolidOn())
               .transform(TagGen.axeOrPickaxe()))
            .blockstate(
               (c, p) -> p.directionalBlock(
                     (Block)c.get(),
                     s -> {
                        boolean isPowered = (Boolean)s.getValue(GantryShaftBlock.POWERED);
                        boolean isFlipped = ((Direction)s.getValue(GantryShaftBlock.FACING)).getAxisDirection() == AxisDirection.NEGATIVE;
                        String partName = ((GantryShaftBlock.Part)s.getValue(GantryShaftBlock.PART)).getSerializedName();
                        String flipped = isFlipped ? "_flipped" : "";
                        String powered = isPowered ? "_powered" : "";
                        ModelFile existing = AssetLookup.partialBaseModel(c, p, partName);
                        return (ModelFile)(!isPowered && !isFlipped
                           ? existing
                           : ((BlockModelBuilder)p.models()
                                 .withExistingParent("block/" + c.getName() + "_" + partName + powered + flipped, existing.getLocation()))
                              .texture("2", p.modLoc("block/" + c.getName() + powered + flipped)));
                     }
                  )
            )
            .transform(CStress.setNoImpact()))
         .item()
         .transform(ModelGen.customItemModel("_", "block_single")))
      .register();
   public static final BlockEntry<WindmillBearingBlock> WINDMILL_BEARING = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "windmill_bearing", WindmillBearingBlock::new
                  )
                  .transform(TagGen.axeOrPickaxe()))
               .properties(p -> p.mapColor(MapColor.PODZOL))
               .transform(BuilderTransformers.bearing("windmill", "gearbox")))
            .transform(CStress.setCapacity(512.0)))
         .onRegister(BlockStressValues.setGeneratorSpeed(16, true)))
      .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
      .register();
   public static final BlockEntry<MechanicalBearingBlock> MECHANICAL_BEARING = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "mechanical_bearing", MechanicalBearingBlock::new
                  )
                  .properties(p -> p.mapColor(MapColor.PODZOL))
                  .transform(TagGen.axeOrPickaxe()))
               .transform(BuilderTransformers.bearing("mechanical", "gearbox")))
            .transform(CStress.setImpact(4.0)))
         .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
         .onRegister(MovementBehaviour.movementBehaviour(new StabilizedBearingMovementBehaviour())))
      .register();
   public static final BlockEntry<ClockworkBearingBlock> CLOCKWORK_BEARING = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "clockwork_bearing", ClockworkBearingBlock::new
               )
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN))
               .transform(TagGen.axeOrPickaxe()))
            .transform(BuilderTransformers.bearing("clockwork", "brass_gearbox")))
         .transform(CStress.setImpact(4.0)))
      .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
      .register();
   public static final BlockEntry<PulleyBlock> ROPE_PULLEY = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block("rope_pulley", PulleyBlock::new)
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.PODZOL))
               .properties(p -> p.noOcclusion())
               .addLayer(() -> RenderType::cutoutMipped)
               .transform(TagGen.axeOrPickaxe()))
            .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
            .blockstate(BlockStateGen.horizontalAxisBlockProvider(true))
            .transform(CStress.setImpact(4.0)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<PulleyBlock.RopeBlock> ROPE = REGISTRATE.block("rope", PulleyBlock.RopeBlock::new)
      .properties(p -> p.sound(SoundType.WOOL).mapColor(MapColor.COLOR_BROWN))
      .tag(new TagKey[]{AllTags.AllBlockTags.BRITTLE.tag})
      .tag(new TagKey[]{BlockTags.CLIMBABLE})
      .blockstate((c, p) -> p.simpleBlock((Block)c.get(), p.models().getExistingFile(p.modLoc("block/rope_pulley/" + c.getName()))))
      .register();
   public static final BlockEntry<PulleyBlock.MagnetBlock> PULLEY_MAGNET = REGISTRATE.block("pulley_magnet", PulleyBlock.MagnetBlock::new)
      .initialProperties(SharedProperties::stone)
      .tag(new TagKey[]{AllTags.AllBlockTags.BRITTLE.tag})
      .tag(new TagKey[]{BlockTags.CLIMBABLE})
      .blockstate((c, p) -> p.simpleBlock((Block)c.get(), p.models().getExistingFile(p.modLoc("block/rope_pulley/" + c.getName()))))
      .register();
   public static final BlockEntry<ElevatorPulleyBlock> ELEVATOR_PULLEY = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "elevator_pulley", ElevatorPulleyBlock::new
               )
               .initialProperties(SharedProperties::softMetal)
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN))
               .transform(TagGen.axeOrPickaxe()))
            .blockstate(BlockStateGen.horizontalBlockProvider(true))
            .transform(CStress.setImpact(4.0)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<CartAssemblerBlock> CART_ASSEMBLER = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("cart_assembler", CartAssemblerBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion().mapColor(MapColor.COLOR_GRAY))
            .transform(TagGen.axeOrPickaxe()))
         .blockstate(BlockStateGen.cartAssembler())
         .addLayer(() -> RenderType::cutoutMipped)
         .tag(new TagKey[]{BlockTags.RAILS, AllTags.AllBlockTags.SAFE_NBT.tag})
         .item(CartAssemblerBlockItem::new)
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<ControllerRailBlock> CONTROLLER_RAIL = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "controller_rail", ControllerRailBlock::new
            )
            .initialProperties(() -> Blocks.POWERED_RAIL)
            .transform(TagGen.pickaxeOnly()))
         .blockstate(new ControllerRailGenerator()::generate)
         .addLayer(() -> RenderType::cutoutMipped)
         .color(
            () -> () -> (state, world, pos, layer) -> RedStoneWireBlock.getColorForPower(
                        pos != null && world != null ? (Integer)state.getValue(BlockStateProperties.POWER) : 0
                     )
         )
         .tag(new TagKey[]{BlockTags.RAILS})
         .item()
         .model((c, p) -> p.generated(c, new ResourceLocation[]{Create.asResource("block/" + c.getName())}))
         .build())
      .register();
   public static final BlockEntry<CartAssemblerBlock.MinecartAnchorBlock> MINECART_ANCHOR = REGISTRATE.block(
         "minecart_anchor", CartAssemblerBlock.MinecartAnchorBlock::new
      )
      .initialProperties(SharedProperties::stone)
      .blockstate((c, p) -> p.simpleBlock((Block)c.get(), p.models().getExistingFile(p.modLoc("block/cart_assembler/" + c.getName()))))
      .register();
   public static final BlockEntry<LinearChassisBlock> LINEAR_CHASSIS = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("linear_chassis", LinearChassisBlock::new)
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN))
            .transform(TagGen.axeOrPickaxe()))
         .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
         .blockstate(BlockStateGen.linearChassis())
         .onRegister(CreateRegistrate.connectedTextures(LinearChassisBlock.ChassisCTBehaviour::new)))
      .lang("Linear Chassis")
      .simpleItem()
      .register();
   public static final BlockEntry<LinearChassisBlock> SECONDARY_LINEAR_CHASSIS = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "secondary_linear_chassis", LinearChassisBlock::new
            )
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.PODZOL))
            .transform(TagGen.axeOrPickaxe()))
         .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
         .blockstate(BlockStateGen.linearChassis())
         .onRegister(CreateRegistrate.connectedTextures(LinearChassisBlock.ChassisCTBehaviour::new)))
      .simpleItem()
      .register();
   public static final BlockEntry<RadialChassisBlock> RADIAL_CHASSIS = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("radial_chassis", RadialChassisBlock::new)
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.DIRT))
            .transform(TagGen.axeOrPickaxe()))
         .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
         .blockstate(BlockStateGen.radialChassis())
         .item()
         .model((c, p) -> {
            String path = "block/" + c.getName();
            p.cubeColumn(c.getName(), p.modLoc(path + "_side"), p.modLoc(path + "_end"));
         })
         .build())
      .register();
   public static final BlockEntry<StickerBlock> STICKER = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("sticker", StickerBlock::new)
            .initialProperties(SharedProperties::stone)
            .transform(TagGen.pickaxeOnly()))
         .properties(Properties::noOcclusion)
         .addLayer(() -> RenderType::cutoutMipped)
         .blockstate((c, p) -> p.directionalBlock((Block)c.get(), AssetLookup.forPowered(c, p)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<ContraptionControlsBlock> CONTRAPTION_CONTROLS = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "contraption_controls", ContraptionControlsBlock::new
                  )
                  .initialProperties(SharedProperties::stone)
                  .properties(p -> p.mapColor(MapColor.PODZOL))
                  .addLayer(() -> RenderType::cutoutMipped)
                  .transform(TagGen.axeOrPickaxe()))
               .blockstate((c, p) -> p.horizontalBlock((Block)c.get(), s -> AssetLookup.partialBaseModel(c, p)))
               .onRegister(MovementBehaviour.movementBehaviour(new ContraptionControlsMovement())))
            .onRegister(MovingInteractionBehaviour.interactionBehaviour(new ContraptionControlsMovingInteraction())))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<DrillBlock> MECHANICAL_DRILL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "mechanical_drill", DrillBlock::new
                  )
                  .initialProperties(SharedProperties::stone)
                  .properties(p -> p.mapColor(MapColor.PODZOL))
                  .transform(TagGen.axeOrPickaxe()))
               .blockstate(BlockStateGen.directionalBlockProvider(true))
               .transform(CStress.setImpact(4.0)))
            .onRegister(MovementBehaviour.movementBehaviour(new DrillMovementBehaviour())))
         .item()
         .tag(new TagKey[]{AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag})
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<SawBlock> MECHANICAL_SAW = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "mechanical_saw", SawBlock::new
                  )
                  .initialProperties(SharedProperties::stone)
                  .addLayer(() -> RenderType::cutoutMipped)
                  .properties(p -> p.mapColor(MapColor.PODZOL))
                  .transform(TagGen.axeOrPickaxe()))
               .blockstate(new SawGenerator()::generate)
               .transform(CStress.setImpact(4.0)))
            .onRegister(MovementBehaviour.movementBehaviour(new SawMovementBehaviour())))
         .addLayer(() -> RenderType::cutoutMipped)
         .item()
         .tag(new TagKey[]{AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag})
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<DeployerBlock> DEPLOYER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                        "deployer", DeployerBlock::new
                     )
                     .initialProperties(SharedProperties::stone)
                     .properties(p -> p.mapColor(MapColor.PODZOL))
                     .transform(TagGen.axeOrPickaxe()))
                  .blockstate(BlockStateGen.directionalAxisBlockProvider())
                  .transform(CStress.setImpact(4.0)))
               .onRegister(MovementBehaviour.movementBehaviour(new DeployerMovementBehaviour())))
            .onRegister(MovingInteractionBehaviour.interactionBehaviour(new DeployerMovingInteraction())))
         .item(AssemblyOperatorBlockItem::new)
         .tag(new TagKey[]{AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag})
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<PortableStorageInterfaceBlock> PORTABLE_STORAGE_INTERFACE = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "portable_storage_interface", PortableStorageInterfaceBlock::forItems
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.PODZOL))
               .transform(TagGen.axeOrPickaxe()))
            .blockstate((c, p) -> p.directionalBlock((Block)c.get(), AssetLookup.partialBaseModel(c, p)))
            .onRegister(MovementBehaviour.movementBehaviour(new PortableStorageInterfaceMovement())))
         .item()
         .tag(new TagKey[]{AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag})
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<RedstoneContactBlock> REDSTONE_CONTACT = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "redstone_contact", RedstoneContactBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.COLOR_GRAY))
               .transform(TagGen.axeOrPickaxe()))
            .onRegister(MovementBehaviour.movementBehaviour(new ContactMovementBehaviour())))
         .blockstate((c, p) -> p.directionalBlock((Block)c.get(), AssetLookup.forPowered(c, p)))
         .item(RedstoneContactItem::new)
         .tag(new TagKey[]{AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag})
         .transform(ModelGen.customItemModel("_", "block")))
      .register();
   public static final BlockEntry<ElevatorContactBlock> ELEVATOR_CONTACT = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "elevator_contact", ElevatorContactBlock::new
               )
               .initialProperties(SharedProperties::softMetal)
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW).lightLevel(ElevatorContactBlock::getLight))
               .transform(TagGen.axeOrPickaxe()))
            .blockstate(
               (c, p) -> p.directionalBlock(
                     (Block)c.get(),
                     state -> {
                        Boolean calling = (Boolean)state.getValue(ElevatorContactBlock.CALLING);
                        Boolean powering = (Boolean)state.getValue(ElevatorContactBlock.POWERING);
                        return powering
                           ? AssetLookup.partialBaseModel(c, p, "powered")
                           : (calling ? AssetLookup.partialBaseModel(c, p, "dim") : AssetLookup.partialBaseModel(c, p));
                     }
                  )
            )
            .loot((p, b) -> p.dropOther(b, (ItemLike)REDSTONE_CONTACT.get()))
            .transform(DisplaySource.displaySource(AllDisplaySources.CURRENT_FLOOR)))
         .item()
         .transform(ModelGen.customItemModel("_", "block")))
      .register();
   public static final BlockEntry<HarvesterBlock> MECHANICAL_HARVESTER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "mechanical_harvester", HarvesterBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.METAL).forceSolidOn())
               .transform(TagGen.axeOrPickaxe()))
            .onRegister(MovementBehaviour.movementBehaviour(new HarvesterMovementBehaviour())))
         .blockstate(BlockStateGen.horizontalBlockProvider(true))
         .addLayer(() -> RenderType::cutoutMipped)
         .item()
         .tag(new TagKey[]{AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag})
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<PloughBlock> MECHANICAL_PLOUGH = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "mechanical_plough", PloughBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.COLOR_GRAY).forceSolidOn())
               .transform(TagGen.axeOrPickaxe()))
            .onRegister(MovementBehaviour.movementBehaviour(new PloughMovementBehaviour())))
         .blockstate(BlockStateGen.horizontalBlockProvider(false))
         .item()
         .tag(new TagKey[]{AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag})
         .build())
      .register();
   public static final BlockEntry<RollerBlock> MECHANICAL_ROLLER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "mechanical_roller", RollerBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.COLOR_GRAY).noOcclusion())
               .transform(TagGen.axeOrPickaxe()))
            .onRegister(MovementBehaviour.movementBehaviour(new RollerMovementBehaviour())))
         .blockstate(BlockStateGen.horizontalBlockProvider(true))
         .addLayer(() -> RenderType::cutoutMipped)
         .item(RollerBlockItem::new)
         .tag(new TagKey[]{AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag})
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<SailBlock> SAIL_FRAME = ((BlockBuilder)REGISTRATE.block("sail_frame", p -> SailBlock.frame(p))
         .initialProperties(SharedProperties::wooden)
         .properties(p -> p.mapColor(MapColor.DIRT).sound(SoundType.SCAFFOLDING).noOcclusion())
         .transform(TagGen.axeOnly()))
      .blockstate(BlockStateGen.directionalBlockProvider(false))
      .lang("Windmill Sail Frame")
      .tag(new TagKey[]{AllTags.AllBlockTags.WINDMILL_SAILS.tag})
      .tag(new TagKey[]{AllTags.AllBlockTags.FAN_TRANSPARENT.tag})
      .simpleItem()
      .register();
   public static final BlockEntry<SailBlock> SAIL = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("white_sail", p -> SailBlock.withCanvas(p, DyeColor.WHITE))
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.SNOW).sound(SoundType.SCAFFOLDING).noOcclusion())
            .transform(TagGen.axeOnly()))
         .blockstate(BlockStateGen.directionalBlockProvider(false))
         .lang("Windmill Sail")
         .tag(new TagKey[]{AllTags.AllBlockTags.WINDMILL_SAILS.tag})
         .item(BlankSailBlockItem::new)
         .build())
      .register();
   public static final DyedBlockList<SailBlock> DYED_SAILS = new DyedBlockList(
      colour -> {
         if (colour == DyeColor.WHITE) {
            return SAIL;
         } else {
            String colourName = colour.getSerializedName();
            return ((BlockBuilder)REGISTRATE.block(colourName + "_sail", p -> SailBlock.withCanvas(p, colour))
                  .initialProperties(SharedProperties::wooden)
                  .properties(p -> p.mapColor(colour.getMapColor()).sound(SoundType.SCAFFOLDING).noOcclusion())
                  .transform(TagGen.axeOnly()))
               .blockstate(
                  (c, p) -> p.directionalBlock(
                        (Block)c.get(),
                        ((BlockModelBuilder)p.models().withExistingParent(colourName + "_sail", p.modLoc("block/white_sail")))
                           .texture("0", p.modLoc("block/sail/canvas_" + colourName))
                     )
               )
               .tag(new TagKey[]{AllTags.AllBlockTags.WINDMILL_SAILS.tag})
               .loot((p, b) -> p.dropOther(b, (ItemLike)SAIL.get()))
               .register();
         }
      }
   );
   public static final BlockEntry<CasingBlock> ANDESITE_CASING = ((BlockBuilder)REGISTRATE.block("andesite_casing", CasingBlock::new)
         .properties(p -> p.mapColor(MapColor.PODZOL))
         .transform(BuilderTransformers.casing(() -> AllSpriteShifts.ANDESITE_CASING)))
      .register();
   public static final BlockEntry<CasingBlock> BRASS_CASING = ((BlockBuilder)REGISTRATE.block("brass_casing", CasingBlock::new)
         .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN))
         .transform(BuilderTransformers.casing(() -> AllSpriteShifts.BRASS_CASING)))
      .register();
   public static final BlockEntry<CasingBlock> COPPER_CASING = ((BlockBuilder)REGISTRATE.block("copper_casing", CasingBlock::new)
         .properties(p -> p.mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).sound(SoundType.COPPER))
         .transform(BuilderTransformers.casing(() -> AllSpriteShifts.COPPER_CASING)))
      .register();
   public static final BlockEntry<CasingBlock> SHADOW_STEEL_CASING = ((BlockBuilder)REGISTRATE.block("shadow_steel_casing", CasingBlock::new)
         .properties(p -> p.mapColor(MapColor.COLOR_BLACK))
         .transform(BuilderTransformers.casing(() -> AllSpriteShifts.SHADOW_STEEL_CASING)))
      .lang("Shadow Casing")
      .register();
   public static final BlockEntry<CasingBlock> REFINED_RADIANCE_CASING = ((BlockBuilder)REGISTRATE.block("refined_radiance_casing", CasingBlock::new)
         .properties(p -> p.mapColor(MapColor.SNOW))
         .transform(BuilderTransformers.casing(() -> AllSpriteShifts.REFINED_RADIANCE_CASING)))
      .properties(p -> p.lightLevel($ -> 12))
      .lang("Radiant Casing")
      .register();
   public static final BlockEntry<MechanicalCrafterBlock> MECHANICAL_CRAFTER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "mechanical_crafter", MechanicalCrafterBlock::new
                  )
                  .initialProperties(SharedProperties::softMetal)
                  .properties(p -> p.noOcclusion().mapColor(MapColor.TERRACOTTA_YELLOW))
                  .transform(TagGen.axeOrPickaxe()))
               .blockstate(BlockStateGen.horizontalBlockProvider(true))
               .transform(CStress.setImpact(2.0)))
            .onRegister(CreateRegistrate.connectedTextures(CrafterCTBehaviour::new)))
         .addLayer(() -> RenderType::cutoutMipped)
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<SequencedGearshiftBlock> SEQUENCED_GEARSHIFT = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "sequenced_gearshift", SequencedGearshiftBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN))
               .transform(TagGen.axeOrPickaxe()))
            .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
            .properties(Properties::noOcclusion)
            .transform(CStress.setNoImpact()))
         .blockstate(new SequencedGearshiftGenerator()::generate)
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<FlywheelBlock> FLYWHEEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block("flywheel", FlywheelBlock::new)
               .initialProperties(SharedProperties::softMetal)
               .properties(p -> p.noOcclusion().mapColor(MapColor.TERRACOTTA_YELLOW))
               .transform(TagGen.axeOrPickaxe()))
            .transform(CStress.setNoImpact()))
         .blockstate(BlockStateGen.axisBlockProvider(true))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<SpeedControllerBlock> ROTATION_SPEED_CONTROLLER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "rotation_speed_controller", SpeedControllerBlock::new
               )
               .initialProperties(SharedProperties::softMetal)
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
               .transform(TagGen.axeOrPickaxe()))
            .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
            .transform(CStress.setNoImpact()))
         .blockstate(BlockStateGen.horizontalAxisBlockProvider(true))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<ArmBlock> MECHANICAL_ARM = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block("mechanical_arm", ArmBlock::new)
               .initialProperties(SharedProperties::softMetal)
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
               .transform(TagGen.axeOrPickaxe()))
            .blockstate(
               (c, p) -> p.getVariantBuilder((Block)c.get())
                     .forAllStates(
                        s -> ConfiguredModel.builder().modelFile(AssetLookup.partialBaseModel(c, p)).rotationX(s.getValue(ArmBlock.CEILING) ? 180 : 0).build()
                     )
            )
            .transform(CStress.setImpact(2.0)))
         .item(ArmItem::new)
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<TrackBlock> TRACK = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "track", TrackMaterial.ANDESITE::createBlock
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.METAL).noOcclusion().forceSolidOn())
               .addLayer(() -> RenderType::cutoutMipped)
               .transform(TagGen.pickaxeOnly()))
            .clientExtension(() -> () -> new TrackBlock.RenderProperties())
            .onRegister(CreateRegistrate.blockModel(() -> TrackModel::new)))
         .blockstate(new TrackBlockStateGenerator()::generate)
         .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Blocks.RELOCATION_NOT_SUPPORTED})
         .tag(new TagKey[]{AllTags.AllBlockTags.TRACKS.tag})
         .tag(new TagKey[]{AllTags.AllBlockTags.GIRDABLE_TRACKS.tag})
         .lang("Train Track")
         .item(TrackBlockItem::new)
         .tag(new TagKey[]{AllTags.AllItemTags.TRACKS.tag})
         .model((c, p) -> p.generated(c, new ResourceLocation[]{Create.asResource("item/" + c.getName())}))
         .build())
      .register();
   public static final BlockEntry<FakeTrackBlock> FAKE_TRACK = REGISTRATE.block("fake_track", FakeTrackBlock::new)
      .properties(p -> p.mapColor(MapColor.METAL).noCollission().noOcclusion().replaceable())
      .blockstate((c, p) -> p.simpleBlock((Block)c.get(), p.models().withExistingParent(c.getName(), p.mcLoc("block/air"))))
      .lang("Track Marker for Maps")
      .register();
   public static final BlockEntry<CasingBlock> RAILWAY_CASING = ((BlockBuilder)REGISTRATE.block("railway_casing", CasingBlock::new)
         .transform(BuilderTransformers.layeredCasing(() -> AllSpriteShifts.RAILWAY_CASING_SIDE, () -> AllSpriteShifts.RAILWAY_CASING)))
      .properties(p -> p.mapColor(MapColor.TERRACOTTA_CYAN).sound(SoundType.NETHERITE_BLOCK))
      .lang("Train Casing")
      .register();
   public static final BlockEntry<StationBlock> TRACK_STATION = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "track_station", StationBlock::new
                  )
                  .initialProperties(SharedProperties::softMetal)
                  .properties(p -> p.mapColor(MapColor.PODZOL).sound(SoundType.NETHERITE_BLOCK))
                  .transform(TagGen.pickaxeOnly()))
               .blockstate((c, p) -> p.simpleBlock((Block)c.get(), AssetLookup.partialBaseModel(c, p)))
               .transform(DisplaySource.displaySource(AllDisplaySources.STATION_SUMMARY)))
            .transform(DisplaySource.displaySource(AllDisplaySources.TRAIN_STATUS)))
         .lang("Train Station")
         .item(TrackTargetingBlockItem.ofType(EdgePointType.STATION))
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<SignalBlock> TRACK_SIGNAL = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("track_signal", SignalBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.PODZOL).noOcclusion().sound(SoundType.NETHERITE_BLOCK))
            .transform(TagGen.pickaxeOnly()))
         .blockstate(
            (c, p) -> p.getVariantBuilder((Block)c.get())
                  .forAllStates(
                     state -> ConfiguredModel.builder()
                           .modelFile(AssetLookup.partialBaseModel(c, p, ((SignalBlock.SignalType)state.getValue(SignalBlock.TYPE)).getSerializedName()))
                           .build()
                  )
         )
         .lang("Train Signal")
         .item(TrackTargetingBlockItem.ofType(EdgePointType.SIGNAL))
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<TrackObserverBlock> TRACK_OBSERVER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "track_observer", TrackObserverBlock::new
               )
               .initialProperties(SharedProperties::softMetal)
               .properties(p -> p.mapColor(MapColor.PODZOL).noOcclusion().sound(SoundType.NETHERITE_BLOCK))
               .blockstate((c, p) -> BlockStateGen.simpleBlock(c, p, AssetLookup.forPowered(c, p)))
               .transform(TagGen.pickaxeOnly()))
            .transform(DisplaySource.displaySource(AllDisplaySources.OBSERVED_TRAIN_NAME)))
         .lang("Train Observer")
         .item(TrackTargetingBlockItem.ofType(EdgePointType.OBSERVER))
         .transform(ModelGen.customItemModel("_", "block")))
      .register();
   public static final BlockEntry<StandardBogeyBlock> SMALL_BOGEY = ((BlockBuilder)REGISTRATE.block(
            "small_bogey", p -> new StandardBogeyBlock(p, BogeySizes.SMALL)
         )
         .properties(p -> p.mapColor(MapColor.PODZOL))
         .transform(BuilderTransformers.bogey()))
      .register();
   public static final BlockEntry<StandardBogeyBlock> LARGE_BOGEY = ((BlockBuilder)REGISTRATE.block(
            "large_bogey", p -> new StandardBogeyBlock(p, BogeySizes.LARGE)
         )
         .properties(p -> p.mapColor(MapColor.PODZOL))
         .transform(BuilderTransformers.bogey()))
      .register();
   public static final BlockEntry<ControlsBlock> TRAIN_CONTROLS = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "controls", ControlsBlock::new
                  )
                  .initialProperties(SharedProperties::softMetal)
                  .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN).sound(SoundType.NETHERITE_BLOCK))
                  .addLayer(() -> RenderType::cutoutMipped)
                  .transform(TagGen.pickaxeOnly()))
               .blockstate(
                  (c, p) -> p.horizontalBlock(
                        (Block)c.get(),
                        s -> AssetLookup.partialBaseModel(
                              c, p, s.getValue(ControlsBlock.VIRTUAL) ? "virtual" : (s.getValue(ControlsBlock.OPEN) ? "open" : "closed")
                           )
                     )
               )
               .onRegister(MovementBehaviour.movementBehaviour(new ControlsMovementBehaviour())))
            .onRegister(MovingInteractionBehaviour.interactionBehaviour(new ControlsInteractionBehaviour())))
         .lang("Train Controls")
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<AndesiteFunnelBlock> ANDESITE_FUNNEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "andesite_funnel", AndesiteFunnelBlock::new
               )
               .addLayer(() -> RenderType::cutoutMipped)
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.STONE))
               .transform(TagGen.pickaxeOnly()))
            .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
            .clientExtension(() -> () -> new ReducedDestroyEffects())
            .onRegister(MovementBehaviour.movementBehaviour(FunnelMovementBehaviour.andesite())))
         .blockstate(new FunnelGenerator("andesite", false)::generate)
         .item(FunnelItem::new)
         .tag(new TagKey[]{AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag})
         .model(FunnelGenerator.itemModel("andesite"))
         .build())
      .register();
   public static final BlockEntry<BeltFunnelBlock> ANDESITE_BELT_FUNNEL = ((BlockBuilder)REGISTRATE.block(
            "andesite_belt_funnel", p -> new BeltFunnelBlock(ANDESITE_FUNNEL, p)
         )
         .addLayer(() -> RenderType::cutoutMipped)
         .initialProperties(SharedProperties::stone)
         .properties(p -> p.mapColor(MapColor.STONE))
         .transform(TagGen.pickaxeOnly()))
      .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
      .clientExtension(() -> () -> new ReducedDestroyEffects())
      .blockstate(new BeltFunnelGenerator("andesite")::generate)
      .loot((p, b) -> p.dropOther(b, (ItemLike)ANDESITE_FUNNEL.get()))
      .register();
   public static final BlockEntry<BrassFunnelBlock> BRASS_FUNNEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "brass_funnel", BrassFunnelBlock::new
               )
               .addLayer(() -> RenderType::cutoutMipped)
               .initialProperties(SharedProperties::softMetal)
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
               .transform(TagGen.pickaxeOnly()))
            .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
            .clientExtension(() -> () -> new ReducedDestroyEffects())
            .onRegister(MovementBehaviour.movementBehaviour(FunnelMovementBehaviour.brass())))
         .blockstate(new FunnelGenerator("brass", true)::generate)
         .item(FunnelItem::new)
         .tag(new TagKey[]{AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag})
         .model(FunnelGenerator.itemModel("brass"))
         .build())
      .register();
   public static final BlockEntry<BeltFunnelBlock> BRASS_BELT_FUNNEL = ((BlockBuilder)REGISTRATE.block(
            "brass_belt_funnel", p -> new BeltFunnelBlock(BRASS_FUNNEL, p)
         )
         .addLayer(() -> RenderType::cutoutMipped)
         .initialProperties(SharedProperties::softMetal)
         .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
         .transform(TagGen.pickaxeOnly()))
      .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
      .clientExtension(() -> () -> new ReducedDestroyEffects())
      .blockstate(new BeltFunnelGenerator("brass")::generate)
      .loot((p, b) -> p.dropOther(b, (ItemLike)BRASS_FUNNEL.get()))
      .register();
   public static final BlockEntry<BeltTunnelBlock> ANDESITE_TUNNEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "andesite_tunnel", BeltTunnelBlock::new
               )
               .properties(p -> p.mapColor(MapColor.STONE))
               .transform(BuilderTransformers.beltTunnel("andesite", ResourceLocation.withDefaultNamespace("block/polished_andesite"))))
            .transform(DisplaySource.displaySource(AllDisplaySources.ACCUMULATE_ITEMS)))
         .transform(DisplaySource.displaySource(AllDisplaySources.ITEM_THROUGHPUT)))
      .register();
   public static final BlockEntry<BrassTunnelBlock> BRASS_TUNNEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "brass_tunnel", BrassTunnelBlock::new
                  )
                  .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
                  .transform(BuilderTransformers.beltTunnel("brass", Create.asResource("block/brass_block"))))
               .transform(DisplaySource.displaySource(AllDisplaySources.ACCUMULATE_ITEMS)))
            .transform(DisplaySource.displaySource(AllDisplaySources.ITEM_THROUGHPUT)))
         .onRegister(CreateRegistrate.connectedTextures(BrassTunnelCTBehaviour::new)))
      .register();
   public static final BlockEntry<SmartObserverBlock> SMART_OBSERVER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                              "content_observer", SmartObserverBlock::new
                           )
                           .initialProperties(SharedProperties::stone)
                           .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN).noOcclusion())
                           .properties(p -> p.isRedstoneConductor(($1, $2, $3) -> false))
                           .transform(TagGen.axeOrPickaxe()))
                        .blockstate(new SmartObserverGenerator()::generate)
                        .transform(DisplaySource.displaySource(AllDisplaySources.COUNT_ITEMS)))
                     .transform(DisplaySource.displaySource(AllDisplaySources.LIST_ITEMS)))
                  .transform(DisplaySource.displaySource(AllDisplaySources.COUNT_FLUIDS)))
               .transform(DisplaySource.displaySource(AllDisplaySources.LIST_FLUIDS)))
            .transform(DisplaySource.displaySource(AllDisplaySources.READ_PACKAGE_ADDRESS)))
         .lang("Smart Observer")
         .item()
         .transform(ModelGen.customItemModel("_", "block")))
      .register();
   public static final BlockEntry<ThresholdSwitchBlock> THRESHOLD_SWITCH = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "stockpile_switch", ThresholdSwitchBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN).noOcclusion())
               .properties(p -> p.isRedstoneConductor(($1, $2, $3) -> false))
               .transform(TagGen.axeOrPickaxe()))
            .blockstate(new ThresholdSwitchGenerator()::generate)
            .transform(DisplaySource.displaySource(AllDisplaySources.FILL_LEVEL)))
         .lang("Threshold Switch")
         .item()
         .transform(ModelGen.customItemModel("threshold_switch", "block_wall")))
      .register();
   public static final BlockEntry<CreativeCrateBlock> CREATIVE_CRATE = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("creative_crate", CreativeCrateBlock::new)
            .transform(BuilderTransformers.crate("creative")))
         .properties(p -> p.mapColor(MapColor.COLOR_PURPLE))
         .transform(MountedItemStorageType.mountedItemStorage(AllMountedStorageTypes.CREATIVE_CRATE)))
      .register();
   public static final BlockEntry<ItemVaultBlock> ITEM_VAULT = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "item_vault", ItemVaultBlock::new
                  )
                  .initialProperties(SharedProperties::softMetal)
                  .properties(p -> p.mapColor(MapColor.TERRACOTTA_BLUE).sound(SoundType.NETHERITE_BLOCK).explosionResistance(1200.0F))
                  .transform(TagGen.pickaxeOnly()))
               .blockstate(
                  (c, p) -> p.getVariantBuilder((Block)c.get())
                        .forAllStates(
                           s -> ConfiguredModel.builder()
                                 .modelFile(AssetLookup.standardModel(c, p))
                                 .rotationY(s.getValue(ItemVaultBlock.HORIZONTAL_AXIS) == Axis.X ? 90 : 0)
                                 .build()
                        )
               )
               .onRegister(CreateRegistrate.connectedTextures(ItemVaultCTBehaviour::new)))
            .transform(MountedItemStorageType.mountedItemStorage(AllMountedStorageTypes.VAULT)))
         .item(ItemVaultItem::new)
         .build())
      .register();
   public static final BlockEntry<ItemHatchBlock> ITEM_HATCH = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("item_hatch", ItemHatchBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_BLUE).sound(SoundType.NETHERITE_BLOCK))
            .transform(TagGen.pickaxeOnly()))
         .addLayer(() -> RenderType::cutoutMipped)
         .blockstate((c, p) -> p.horizontalBlock((Block)c.get(), s -> AssetLookup.partialBaseModel(c, p, s.getValue(ItemHatchBlock.OPEN) ? "open" : "closed")))
         .item()
         .transform(ModelGen.customItemModel("_", "block_closed")))
      .register();
   public static final BlockEntry<PackagerBlock> PACKAGER = ((BlockBuilder)REGISTRATE.block("packager", PackagerBlock::new)
         .transform(BuilderTransformers.packager()))
      .register();
   public static final BlockEntry<RepackagerBlock> REPACKAGER = ((BlockBuilder)REGISTRATE.block("repackager", RepackagerBlock::new)
         .transform(BuilderTransformers.packager()))
      .lang("Re-Packager")
      .register();
   public static final BlockEntry<FrogportBlock> PACKAGE_FROGPORT = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("package_frogport", FrogportBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.noOcclusion())
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_BLUE).sound(SoundType.NETHERITE_BLOCK))
            .transform(TagGen.pickaxeOnly()))
         .addLayer(() -> RenderType::cutoutMipped)
         .blockstate((c, p) -> p.simpleBlock((Block)c.getEntry(), AssetLookup.partialBaseModel(c, p)))
         .item(PackagePortItem::new)
         .model(AssetLookup::customItemModel)
         .build())
      .register();
   public static final DyedBlockList<PostboxBlock> PACKAGE_POSTBOXES = new DyedBlockList(
      colour -> {
         String colourName = colour.getSerializedName();
         return ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(colourName + "_postbox", p -> new PostboxBlock(p, colour))
                     .initialProperties(SharedProperties::wooden)
                     .properties(p -> p.mapColor(colour))
                     .transform(TagGen.axeOnly()))
                  .blockstate(
                     (c, p) -> p.horizontalBlock(
                           (Block)c.get(),
                           s -> {
                              String suffix = s.getValue(PostboxBlock.OPEN) ? "open" : "closed";
                              return ((BlockModelBuilder)((BlockModelBuilder)p.models()
                                       .withExistingParent(colourName + "_postbox_" + suffix, p.modLoc("block/package_postbox/block_" + suffix)))
                                    .texture("0", p.modLoc("block/post_box/post_box_" + colourName)))
                                 .texture("1", p.modLoc("block/post_box/post_box_" + colourName + "_" + suffix));
                           }
                        )
                  )
                  .tag(new TagKey[]{AllTags.AllBlockTags.POSTBOXES.tag})
                  .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block.create.package_postbox")))
               .item(PackagePortItem::new)
               .recipe(
                  (c, p) -> {
                     ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, (ItemLike)c.get())
                        .define('D', colour.getTag())
                        .define('B', Items.BARREL)
                        .define('A', AllItems.ANDESITE_ALLOY)
                        .pattern("D")
                        .pattern("B")
                        .pattern("A")
                        .unlockedBy("has_barrel", RegistrateRecipeProvider.has(Items.BARREL))
                        .save(p, Create.asResource("crafting/logistics/" + c.getName()));
                     ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, (ItemLike)c.get())
                        .requires(colour.getTag())
                        .requires(AllTags.AllItemTags.POSTBOXES.tag)
                        .unlockedBy("has_postbox", RegistrateRecipeProvider.has(AllTags.AllItemTags.POSTBOXES.tag))
                        .save(p, Create.asResource("crafting/logistics/" + c.getName() + "_from_other_postbox"));
                  }
               )
               .model(
                  (c, p) -> ((ItemModelBuilder)((ItemModelBuilder)p.withExistingParent(colourName + "_postbox", p.modLoc("block/package_postbox/item")))
                           .texture("0", p.modLoc("block/post_box/post_box_" + colourName)))
                        .texture("1", p.modLoc("block/post_box/post_box_" + colourName + "_closed"))
               )
               .tag(new TagKey[]{AllTags.AllItemTags.POSTBOXES.tag})
               .build())
            .register();
      }
   );
   public static final BlockEntry<PackagerLinkBlock> STOCK_LINK = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("stock_link", PackagerLinkBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_BLUE).sound(SoundType.NETHERITE_BLOCK))
            .transform(TagGen.pickaxeOnly()))
         .blockstate(new PackagerLinkGenerator()::generate)
         .item(LogisticallyLinkedBlockItem::new)
         .transform(ModelGen.customItemModel("_", "block_vertical")))
      .register();
   public static final BlockEntry<StockTickerBlock> STOCK_TICKER = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("stock_ticker", StockTickerBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.sound(SoundType.GLASS))
            .transform(TagGen.axeOrPickaxe()))
         .addLayer(() -> RenderType::cutoutMipped)
         .blockstate((c, p) -> p.horizontalBlock((Block)c.get(), AssetLookup.standardModel(c, p)))
         .item(LogisticallyLinkedBlockItem::new)
         .build())
      .register();
   public static final BlockEntry<RedstoneRequesterBlock> REDSTONE_REQUESTER = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "redstone_requester", RedstoneRequesterBlock::new
            )
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
            .properties(p -> p.noOcclusion())
            .transform(TagGen.pickaxeOnly()))
         .blockstate((c, p) -> BlockStateGen.horizontalAxisBlock(c, p, AssetLookup.forPowered(c, p)))
         .item(RedstoneRequesterBlockItem::new)
         .transform(ModelGen.customItemModel("_", "block")))
      .register();
   public static final BlockEntry<FactoryPanelBlock> FACTORY_GAUGE = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "factory_gauge", FactoryPanelBlock::new
                  )
                  .addLayer(() -> RenderType::cutoutMipped)
                  .initialProperties(SharedProperties::copperMetal)
                  .properties(p -> p.noOcclusion())
                  .properties(p -> p.forceSolidOn())
                  .transform(TagGen.pickaxeOnly()))
               .blockstate((c, p) -> p.horizontalFaceBlock((Block)c.get(), AssetLookup.partialBaseModel(c, p)))
               .onRegister(CreateRegistrate.blockModel(() -> FactoryPanelModel::new)))
            .transform(DisplaySource.displaySource(AllDisplaySources.GAUGE_STATUS)))
         .item(FactoryPanelBlockItem::new)
         .model(AssetLookup::customItemModel)
         .build())
      .register();
   public static final DyedBlockList<TableClothBlock> TABLE_CLOTHS = new DyedBlockList<>(
      colour -> {
         String colourName = colour.getSerializedName();
         return ((BlockBuilder)REGISTRATE.block(colourName + "_table_cloth", p -> new TableClothBlock(p, colour))
               .transform(BuilderTransformers.tableCloth(colourName, () -> Blocks.BLACK_CARPET, true)))
            .properties(p -> p.mapColor(colour))
            .recipe(
               (c, p) -> {
                  ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, (ItemLike)c.get(), 2)
                     .requires(DyeHelper.getWoolOfDye(colour))
                     .requires(AllItems.ANDESITE_ALLOY)
                     .unlockedBy("has_wool", RegistrateRecipeProvider.has(ItemTags.WOOL))
                     .save(p, Create.asResource("crafting/logistics/" + c.getName()));
                  ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, (ItemLike)c.get())
                     .requires(colour.getTag())
                     .requires(AllTags.AllItemTags.DYED_TABLE_CLOTHS.tag)
                     .unlockedBy("has_postbox", RegistrateRecipeProvider.has(AllTags.AllItemTags.DYED_TABLE_CLOTHS.tag))
                     .save(p, Create.asResource("crafting/logistics/" + c.getName() + "_from_other_table_cloth"));
               }
            )
            .register();
      }
   );
   public static final BlockEntry<TableClothBlock> ANDESITE_TABLE_CLOTH = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "andesite_table_cloth", p -> new TableClothBlock(p, "andesite")
            )
            .transform(BuilderTransformers.tableCloth("andesite", SharedProperties::stone, false)))
         .properties(p -> p.mapColor(MapColor.STONE).requiresCorrectToolForDrops())
         .recipe((c, p) -> p.stonecutting(DataIngredient.items((Item)AllItems.ANDESITE_ALLOY.get(), new Item[0]), RecipeCategory.DECORATIONS, c::get, 2))
         .transform(TagGen.pickaxeOnly()))
      .lang("Andesite Table Cover")
      .register();
   public static final BlockEntry<TableClothBlock> BRASS_TABLE_CLOTH = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "brass_table_cloth", p -> new TableClothBlock(p, "brass")
            )
            .transform(BuilderTransformers.tableCloth("brass", SharedProperties::softMetal, false)))
         .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW).requiresCorrectToolForDrops())
         .recipe((c, p) -> p.stonecutting(DataIngredient.tag(CommonMetal.BRASS.ingots), RecipeCategory.DECORATIONS, c::get, 2))
         .transform(TagGen.pickaxeOnly()))
      .lang("Brass Table Cover")
      .register();
   public static final BlockEntry<TableClothBlock> COPPER_TABLE_CLOTH = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "copper_table_cloth", p -> new TableClothBlock(p, "copper")
            )
            .transform(BuilderTransformers.tableCloth("copper", SharedProperties::copperMetal, false)))
         .properties(p -> p.requiresCorrectToolForDrops())
         .recipe((c, p) -> p.stonecutting(DataIngredient.tag(CommonMetal.COPPER.ingots), RecipeCategory.DECORATIONS, c::get, 2))
         .transform(TagGen.pickaxeOnly()))
      .lang("Copper Table Cover")
      .register();
   public static final BlockEntry<DisplayLinkBlock> DISPLAY_LINK = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("display_link", DisplayLinkBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN))
            .addLayer(() -> RenderType::translucent)
            .transform(TagGen.axeOrPickaxe()))
         .blockstate((c, p) -> p.directionalBlock((Block)c.get(), AssetLookup.forPowered(c, p)))
         .item(DisplayLinkBlockItem::new)
         .transform(ModelGen.customItemModel("_", "block")))
      .register();
   public static final BlockEntry<FlapDisplayBlock> DISPLAY_BOARD = ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                     "display_board", FlapDisplayBlock::new
                  )
                  .initialProperties(SharedProperties::softMetal)
                  .properties(p -> p.mapColor(MapColor.COLOR_GRAY))
                  .addLayer(() -> RenderType::cutoutMipped)
                  .transform(TagGen.pickaxeOnly()))
               .transform(CStress.setNoImpact()))
            .blockstate((c, p) -> p.horizontalBlock((Block)c.get(), AssetLookup.partialBaseModel(c, p)))
            .transform(DisplayTarget.displayTarget(AllDisplayTargets.DISPLAY_BOARD)))
         .lang("Display Board")
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<NixieTubeBlock> ORANGE_NIXIE_TUBE = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "nixie_tube", p -> new NixieTubeBlock(p, DyeColor.ORANGE)
            )
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.lightLevel($ -> 5).mapColor(DyeColor.ORANGE).forceSolidOn())
            .transform(TagGen.pickaxeOnly()))
         .blockstate(new NixieTubeGenerator()::generate)
         .addLayer(() -> RenderType::translucent)
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final DyedBlockList<NixieTubeBlock> NIXIE_TUBES = new DyedBlockList(
      colour -> {
         if (colour == DyeColor.ORANGE) {
            return ORANGE_NIXIE_TUBE;
         } else {
            String colourName = colour.getSerializedName();
            return ((BlockBuilder)REGISTRATE.block(colourName + "_nixie_tube", p -> new NixieTubeBlock(p, colour))
                  .initialProperties(SharedProperties::softMetal)
                  .properties(p -> p.lightLevel($ -> 5).mapColor(colour).forceSolidOn())
                  .transform(TagGen.pickaxeOnly()))
               .blockstate(new NixieTubeGenerator()::generate)
               .loot((p, b) -> p.dropOther(b, (ItemLike)ORANGE_NIXIE_TUBE.get()))
               .addLayer(() -> RenderType::translucent)
               .register();
         }
      }
   );
   public static final BlockEntry<RoseQuartzLampBlock> ROSE_QUARTZ_LAMP = ((BlockBuilder)REGISTRATE.block("rose_quartz_lamp", RoseQuartzLampBlock::new)
         .initialProperties(() -> Blocks.REDSTONE_LAMP)
         .properties(p -> p.mapColor(MapColor.TERRACOTTA_PINK).lightLevel(s -> s.getValue(RoseQuartzLampBlock.POWERING) ? 15 : 0))
         .blockstate((c, p) -> BlockStateGen.simpleBlock(c, p, s -> {
               boolean powered = (Boolean)s.getValue(RoseQuartzLampBlock.POWERING);
               String name = c.getName() + (powered ? "_powered" : "");
               return p.models().cubeAll(name, p.modLoc("block/" + name));
            }))
         .transform(TagGen.pickaxeOnly()))
      .simpleItem()
      .register();
   public static final BlockEntry<RedstoneLinkBlock> REDSTONE_LINK = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("redstone_link", RedstoneLinkBlock::new)
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN).forceSolidOn())
            .transform(TagGen.axeOrPickaxe()))
         .tag(new TagKey[]{AllTags.AllBlockTags.BRITTLE.tag, AllTags.AllBlockTags.SAFE_NBT.tag})
         .blockstate(new RedstoneLinkGenerator()::generate)
         .addLayer(() -> RenderType::cutoutMipped)
         .item()
         .transform(ModelGen.customItemModel("_", "transmitter")))
      .register();
   public static final BlockEntry<AnalogLeverBlock> ANALOG_LEVER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "analog_lever", AnalogLeverBlock::new
               )
               .initialProperties(() -> Blocks.LEVER)
               .transform(TagGen.axeOrPickaxe()))
            .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
            .blockstate((c, p) -> p.horizontalFaceBlock((Block)c.get(), AssetLookup.partialBaseModel(c, p)))
            .onRegister(ItemUseOverrides::addBlock))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<PlacardBlock> PLACARD = ((BlockBuilder)REGISTRATE.block("placard", PlacardBlock::new)
         .initialProperties(SharedProperties::copperMetal)
         .properties(p -> p.forceSolidOn())
         .transform(TagGen.pickaxeOnly()))
      .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
      .blockstate((c, p) -> p.horizontalFaceBlock((Block)c.get(), AssetLookup.standardModel(c, p)))
      .simpleItem()
      .register();
   public static final BlockEntry<BrassDiodeBlock> PULSE_REPEATER = ((BlockBuilder)REGISTRATE.block("pulse_repeater", BrassDiodeBlock::new)
         .initialProperties(() -> Blocks.REPEATER)
         .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
         .blockstate(new BrassDiodeGenerator()::generate)
         .addLayer(() -> RenderType::cutoutMipped)
         .item()
         .model(AbstractDiodeGenerator::diodeItemModel)
         .build())
      .register();
   public static final BlockEntry<BrassDiodeBlock> PULSE_EXTENDER = ((BlockBuilder)REGISTRATE.block("pulse_extender", BrassDiodeBlock::new)
         .initialProperties(() -> Blocks.REPEATER)
         .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
         .blockstate(new BrassDiodeGenerator()::generate)
         .addLayer(() -> RenderType::cutoutMipped)
         .item()
         .model(AbstractDiodeGenerator::diodeItemModel)
         .build())
      .register();
   public static final BlockEntry<BrassDiodeBlock> PULSE_TIMER = ((BlockBuilder)REGISTRATE.block("pulse_timer", BrassDiodeBlock::new)
         .initialProperties(() -> Blocks.REPEATER)
         .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
         .blockstate(new BrassDiodeGenerator()::generate)
         .addLayer(() -> RenderType::cutoutMipped)
         .item()
         .model(AbstractDiodeGenerator::diodeItemModel)
         .build())
      .register();
   public static final BlockEntry<PoweredLatchBlock> POWERED_LATCH = REGISTRATE.block("powered_latch", PoweredLatchBlock::new)
      .initialProperties(() -> Blocks.REPEATER)
      .blockstate(new PoweredLatchGenerator()::generate)
      .addLayer(() -> RenderType::cutoutMipped)
      .simpleItem()
      .register();
   public static final BlockEntry<ToggleLatchBlock> POWERED_TOGGLE_LATCH = ((BlockBuilder)REGISTRATE.block("powered_toggle_latch", ToggleLatchBlock::new)
         .initialProperties(() -> Blocks.REPEATER)
         .blockstate(new ToggleLatchGenerator()::generate)
         .addLayer(() -> RenderType::cutoutMipped)
         .item()
         .transform(ModelGen.customItemModel("diodes", "latch_off")))
      .register();
   public static final BlockEntry<LecternControllerBlock> LECTERN_CONTROLLER = ((BlockBuilder)REGISTRATE.block(
            "lectern_controller", LecternControllerBlock::new
         )
         .initialProperties(() -> Blocks.LECTERN)
         .transform(TagGen.axeOnly()))
      .blockstate((c, p) -> p.horizontalBlock((Block)c.get(), p.models().getExistingFile(p.mcLoc("block/lectern"))))
      .loot((lt, block) -> lt.dropOther(block, Blocks.LECTERN))
      .register();
   public static final BlockEntry<BacktankBlock> COPPER_BACKTANK = ((BlockBuilder)REGISTRATE.block("copper_backtank", BacktankBlock::new)
         .initialProperties(SharedProperties::copperMetal)
         .transform(BuilderTransformers.backtank(AllItems.COPPER_BACKTANK::get)))
      .register();
   public static final BlockEntry<BacktankBlock> NETHERITE_BACKTANK = ((BlockBuilder)REGISTRATE.block("netherite_backtank", BacktankBlock::new)
         .initialProperties(SharedProperties::netheriteMetal)
         .transform(BuilderTransformers.backtank(AllItems.NETHERITE_BACKTANK::get)))
      .register();
   public static final BlockEntry<PeculiarBellBlock> PECULIAR_BELL = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("peculiar_bell", PeculiarBellBlock::new)
            .properties(p -> p.mapColor(MapColor.GOLD).forceSolidOn())
            .transform(BuilderTransformers.bell()))
         .onRegister(MovementBehaviour.movementBehaviour(new BellMovementBehaviour())))
      .register();
   public static final BlockEntry<HauntedBellBlock> HAUNTED_BELL = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("haunted_bell", HauntedBellBlock::new)
            .properties(p -> p.mapColor(MapColor.SAND).forceSolidOn())
            .transform(BuilderTransformers.bell()))
         .onRegister(MovementBehaviour.movementBehaviour(new HauntedBellMovementBehaviour())))
      .register();
   public static final BlockEntry<DeskBellBlock> DESK_BELL = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("desk_bell", DeskBellBlock::new)
            .properties(p -> p.mapColor(MapColor.SAND))
            .blockstate((c, p) -> p.directionalBlock((Block)c.get(), AssetLookup.forPowered(c, p)))
            .item()
            .transform(ModelGen.customItemModel("_", "block")))
         .onRegister(MovementBehaviour.movementBehaviour(new BellMovementBehaviour())))
      .register();
   public static final DyedBlockList<ToolboxBlock> TOOLBOXES = new DyedBlockList(
      colour -> {
         String colourName = colour.getSerializedName();
         return ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(colourName + "_toolbox", p -> new ToolboxBlock(p, colour))
                     .initialProperties(SharedProperties::wooden)
                     .properties(p -> p.sound(SoundType.WOOD).mapColor(colour).forceSolidOn())
                     .addLayer(() -> RenderType::cutoutMipped)
                     .loot(
                        (lt, block) -> lt.add(
                              block,
                              LootTable.lootTable()
                                 .withPool(
                                    LootPool.lootPool()
                                       .when(ExplosionCondition.survivesExplosion())
                                       .setRolls(ConstantValue.exactly(1.0F))
                                       .add(
                                          LootItem.lootTableItem(block)
                                             .apply(CopyNameFunction.copyName(NameSource.BLOCK_ENTITY))
                                             .apply(
                                                CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY)
                                                   .include(AllDataComponents.TOOLBOX_UUID)
                                                   .include(AllDataComponents.TOOLBOX_INVENTORY)
                                             )
                                       )
                                 )
                           )
                     )
                     .blockstate(
                        (c, p) -> p.horizontalBlock(
                              (Block)c.get(),
                              ((BlockModelBuilder)p.models().withExistingParent(colourName + "_toolbox", p.modLoc("block/toolbox/block")))
                                 .texture("0", p.modLoc("block/toolbox/" + colourName))
                           )
                     )
                     .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block.create.toolbox")))
                  .transform(MountedItemStorageType.mountedItemStorage(AllMountedStorageTypes.TOOLBOX)))
               .tag(new TagKey[]{AllTags.AllBlockTags.TOOLBOXES.tag})
               .item(UncontainableBlockItem::new)
               .model(
                  (c, p) -> ((ItemModelBuilder)p.withExistingParent(colourName + "_toolbox", p.modLoc("block/toolbox/item")))
                        .texture("0", p.modLoc("block/toolbox/" + colourName))
               )
               .tag(new TagKey[]{AllTags.AllItemTags.TOOLBOXES.tag})
               .build())
            .register();
      }
   );
   public static final BlockEntry<ClipboardBlock> CLIPBOARD = ((BlockBuilder)((ItemBuilder)((BlockBuilder)REGISTRATE.block("clipboard", ClipboardBlock::new)
               .initialProperties(SharedProperties::wooden)
               .properties(p -> p.forceSolidOn())
               .transform(TagGen.axeOrPickaxe()))
            .tag(new TagKey[]{AllTags.AllBlockTags.SAFE_NBT.tag})
            .blockstate(
               (c, p) -> p.horizontalFaceBlock(
                     (Block)c.get(), s -> AssetLookup.partialBaseModel(c, p, s.getValue(ClipboardBlock.WRITTEN) ? "written" : "empty")
                  )
            )
            .loot((lt, b) -> lt.add(b, BlockLootSubProvider.noDrop()))
            .item(ClipboardBlockItem::new)
            .onRegister(ClipboardBlockItem::registerModelOverrides))
         .model((c, p) -> ClipboardOverrides.addOverrideModels(c, p))
         .build())
      .register();
   public static final BlockEntry<MetalLadderBlock> ANDESITE_LADDER = ((BlockBuilder)REGISTRATE.block("andesite_ladder", MetalLadderBlock::new)
         .transform(BuilderTransformers.ladder("andesite", () -> DataIngredient.items((Item)AllItems.ANDESITE_ALLOY.get(), new Item[0]), MapColor.STONE)))
      .register();
   public static final BlockEntry<MetalLadderBlock> BRASS_LADDER = ((BlockBuilder)REGISTRATE.block("brass_ladder", MetalLadderBlock::new)
         .transform(BuilderTransformers.ladder("brass", () -> DataIngredient.tag(CommonMetal.BRASS.ingots), MapColor.TERRACOTTA_YELLOW)))
      .register();
   public static final BlockEntry<MetalLadderBlock> COPPER_LADDER = ((BlockBuilder)REGISTRATE.block("copper_ladder", MetalLadderBlock::new)
         .transform(BuilderTransformers.ladder("copper", () -> DataIngredient.tag(CommonMetal.COPPER.ingots), MapColor.COLOR_ORANGE)))
      .register();
   public static final BlockEntry<IronBarsBlock> ANDESITE_BARS = MetalBarsGen.createBars(
      "andesite", true, () -> DataIngredient.items((Item)AllItems.ANDESITE_ALLOY.get(), new Item[0]), MapColor.STONE
   );
   public static final BlockEntry<IronBarsBlock> BRASS_BARS = MetalBarsGen.createBars(
      "brass", true, () -> DataIngredient.tag(CommonMetal.BRASS.ingots), MapColor.TERRACOTTA_YELLOW
   );
   public static final BlockEntry<IronBarsBlock> COPPER_BARS = MetalBarsGen.createBars(
      "copper", true, () -> DataIngredient.tag(CommonMetal.COPPER.ingots), MapColor.COLOR_ORANGE
   );
   public static final BlockEntry<MetalScaffoldingBlock> ANDESITE_SCAFFOLD = ((BlockBuilder)REGISTRATE.block("andesite_scaffolding", MetalScaffoldingBlock::new)
         .transform(
            BuilderTransformers.scaffold(
               "andesite",
               () -> DataIngredient.items((Item)AllItems.ANDESITE_ALLOY.get(), new Item[0]),
               MapColor.STONE,
               AllSpriteShifts.ANDESITE_SCAFFOLD,
               AllSpriteShifts.ANDESITE_SCAFFOLD_INSIDE,
               AllSpriteShifts.ANDESITE_CASING
            )
         ))
      .register();
   public static final BlockEntry<MetalScaffoldingBlock> BRASS_SCAFFOLD = ((BlockBuilder)REGISTRATE.block("brass_scaffolding", MetalScaffoldingBlock::new)
         .transform(
            BuilderTransformers.scaffold(
               "brass",
               () -> DataIngredient.tag(CommonMetal.BRASS.ingots),
               MapColor.TERRACOTTA_YELLOW,
               AllSpriteShifts.BRASS_SCAFFOLD,
               AllSpriteShifts.BRASS_SCAFFOLD_INSIDE,
               AllSpriteShifts.BRASS_CASING
            )
         ))
      .register();
   public static final BlockEntry<MetalScaffoldingBlock> COPPER_SCAFFOLD = ((BlockBuilder)REGISTRATE.block("copper_scaffolding", MetalScaffoldingBlock::new)
         .transform(
            BuilderTransformers.scaffold(
               "copper",
               () -> DataIngredient.tag(CommonMetal.COPPER.ingots),
               MapColor.COLOR_ORANGE,
               AllSpriteShifts.COPPER_SCAFFOLD,
               AllSpriteShifts.COPPER_SCAFFOLD_INSIDE,
               AllSpriteShifts.COPPER_CASING
            )
         ))
      .register();
   public static final BlockEntry<GirderBlock> METAL_GIRDER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block("metal_girder", GirderBlock::new)
               .initialProperties(SharedProperties::softMetal)
               .properties(p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.NETHERITE_BLOCK))
               .transform(TagGen.pickaxeOnly()))
            .blockstate(GirderBlockStateGenerator::blockState)
            .onRegister(CreateRegistrate.blockModel(() -> ConnectedGirderModel::new)))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<GirderEncasedShaftBlock> METAL_GIRDER_ENCASED_SHAFT = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "metal_girder_encased_shaft", GirderEncasedShaftBlock::new
            )
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.NETHERITE_BLOCK))
            .transform(TagGen.pickaxeOnly()))
         .blockstate(GirderBlockStateGenerator::blockStateWithShaft)
         .loot(
            (p, b) -> p.add(
                  b,
                  p.createSingleItemTable((ItemLike)METAL_GIRDER.get())
                     .withPool(
                        (net.minecraft.world.level.storage.loot.LootPool.Builder)p.applyExplosionCondition(
                           (ItemLike)SHAFT.get(), LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem((ItemLike)SHAFT.get()))
                        )
                     )
               )
         )
         .onRegister(CreateRegistrate.blockModel(() -> ConnectedGirderModel::new)))
      .register();
   public static final BlockEntry<Block> COPYCAT_BASE = ((BlockBuilder)REGISTRATE.block("copycat_base", Block::new)
         .initialProperties(SharedProperties::softMetal)
         .properties(p -> p.mapColor(MapColor.GLOW_LICHEN))
         .addLayer(() -> RenderType::cutoutMipped)
         .tag(new TagKey[]{AllTags.AllBlockTags.FAN_TRANSPARENT.tag})
         .transform(TagGen.pickaxeOnly()))
      .blockstate((c, p) -> p.simpleBlock((Block)c.get(), AssetLookup.partialBaseModel(c, p)))
      .register();
   public static final BlockEntry<CopycatStepBlock> COPYCAT_STEP = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "copycat_step", CopycatStepBlock::new
               )
               .properties(p -> p.forceSolidOn())
               .transform(BuilderTransformers.copycat()))
            .onRegister(CreateRegistrate.blockModel(() -> CopycatStepModel::new)))
         .item()
         .recipe((c, p) -> p.stonecutting(DataIngredient.tag(CommonMetal.ZINC.ingots), RecipeCategory.BUILDING_BLOCKS, c::get, 4))
         .transform(ModelGen.customItemModel("copycat_base", "step")))
      .register();
   public static final BlockEntry<CopycatPanelBlock> COPYCAT_PANEL = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "copycat_panel", CopycatPanelBlock::new
               )
               .transform(BuilderTransformers.copycat()))
            .onRegister(CreateRegistrate.blockModel(() -> CopycatPanelModel::new)))
         .item()
         .recipe((c, p) -> p.stonecutting(DataIngredient.tag(CommonMetal.ZINC.ingots), RecipeCategory.BUILDING_BLOCKS, c::get, 4))
         .transform(ModelGen.customItemModel("copycat_base", "panel")))
      .register();
   public static final BlockEntry<WrenchableDirectionalBlock> COPYCAT_BARS = ((BlockBuilder)REGISTRATE.block("copycat_bars", WrenchableDirectionalBlock::new)
         .blockstate(new SpecialCopycatPanelBlockState("bars")::generate)
         .onRegister(CreateRegistrate.blockModel(() -> CopycatBarsModel::new)))
      .register();
   public static final DyedBlockList<SeatBlock> SEATS = new DyedBlockList<>(
      colour -> {
         String colourName = colour.getSerializedName();
         SeatMovementBehaviour movementBehaviour = new SeatMovementBehaviour();
         SeatInteractionBehaviour interactionBehaviour = new SeatInteractionBehaviour();
         return ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                                 colourName + "_seat", p -> new SeatBlock(p, colour)
                              )
                              .initialProperties(SharedProperties::wooden)
                              .properties(p -> p.mapColor(colour))
                              .transform(TagGen.axeOnly()))
                           .onRegister(MovementBehaviour.movementBehaviour(movementBehaviour)))
                        .onRegister(MovingInteractionBehaviour.interactionBehaviour(interactionBehaviour)))
                     .transform(DisplaySource.displaySource(AllDisplaySources.ENTITY_NAME)))
                  .blockstate(
                     (c, p) -> p.simpleBlock(
                           (Block)c.get(),
                           ((BlockModelBuilder)((BlockModelBuilder)p.models().withExistingParent(colourName + "_seat", p.modLoc("block/seat")))
                                 .texture("1", p.modLoc("block/seat/top_" + colourName)))
                              .texture("2", p.modLoc("block/seat/side_" + colourName))
                        )
                  )
                  .recipe(
                     (c, p) -> {
                        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, (ItemLike)c.get())
                           .requires(DyeHelper.getWoolOfDye(colour))
                           .requires(ItemTags.WOODEN_SLABS)
                           .unlockedBy("has_wool", RegistrateRecipeProvider.has(ItemTags.WOOL))
                           .save(p, Create.asResource("crafting/kinetics/" + c.getName()));
                        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, (ItemLike)c.get())
                           .requires(colour.getTag())
                           .requires(AllTags.AllItemTags.SEATS.tag)
                           .unlockedBy("has_seat", RegistrateRecipeProvider.has(AllTags.AllItemTags.SEATS.tag))
                           .save(p, Create.asResource("crafting/kinetics/" + c.getName() + "_from_other_seat"));
                     }
                  )
                  .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block.create.seat")))
               .tag(new TagKey[]{AllTags.AllBlockTags.SEATS.tag})
               .item()
               .tag(new TagKey[]{AllTags.AllItemTags.SEATS.tag})
               .build())
            .register();
      }
   );
   public static final BlockEntry<SlidingDoorBlock> ANDESITE_DOOR = ((BlockBuilder)REGISTRATE.block("andesite_door", p -> SlidingDoorBlock.stone(p, true))
         .transform(BuilderTransformers.slidingDoor("andesite")))
      .properties(p -> p.mapColor(MapColor.STONE).noOcclusion())
      .register();
   public static final BlockEntry<SlidingDoorBlock> BRASS_DOOR = ((BlockBuilder)REGISTRATE.block("brass_door", p -> SlidingDoorBlock.stone(p, false))
         .transform(BuilderTransformers.slidingDoor("brass")))
      .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW).noOcclusion())
      .register();
   public static final BlockEntry<SlidingDoorBlock> COPPER_DOOR = ((BlockBuilder)REGISTRATE.block("copper_door", p -> SlidingDoorBlock.stone(p, true))
         .transform(BuilderTransformers.slidingDoor("copper")))
      .properties(p -> p.mapColor(MapColor.COLOR_ORANGE).noOcclusion())
      .register();
   public static final BlockEntry<SlidingDoorBlock> TRAIN_DOOR = ((BlockBuilder)REGISTRATE.block("train_door", p -> SlidingDoorBlock.metal(p, false))
         .transform(BuilderTransformers.slidingDoor("train")))
      .properties(p -> p.mapColor(MapColor.TERRACOTTA_CYAN).noOcclusion())
      .register();
   public static final BlockEntry<TrainTrapdoorBlock> TRAIN_TRAPDOOR = ((BlockBuilder)REGISTRATE.block("train_trapdoor", TrainTrapdoorBlock::metal)
         .initialProperties(SharedProperties::softMetal)
         .properties(p -> p.mapColor(MapColor.TERRACOTTA_CYAN))
         .transform(BuilderTransformers.trapdoor(true)))
      .register();
   public static final BlockEntry<SlidingDoorBlock> FRAMED_GLASS_DOOR = ((BlockBuilder)REGISTRATE.block(
            "framed_glass_door", p -> SlidingDoorBlock.glass(p, false)
         )
         .transform(BuilderTransformers.slidingDoor("glass")))
      .properties(p -> p.mapColor(MapColor.NONE).noOcclusion())
      .register();
   public static final BlockEntry<TrainTrapdoorBlock> FRAMED_GLASS_TRAPDOOR = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "framed_glass_trapdoor", TrainTrapdoorBlock::glass
            )
            .initialProperties(SharedProperties::softMetal)
            .transform(BuilderTransformers.trapdoor(false)))
         .properties(p -> p.mapColor(MapColor.NONE).noOcclusion())
         .onRegister(CreateRegistrate.connectedTextures(TrapdoorCTBehaviour::new)))
      .addLayer(() -> RenderType::cutoutMipped)
      .register();
   public static final BlockEntry<Block> ZINC_ORE = ((BlockBuilder)((ItemBuilder)((BlockBuilder)REGISTRATE.block("zinc_ore", Block::new)
               .initialProperties(() -> Blocks.GOLD_ORE)
               .properties(p -> p.mapColor(MapColor.METAL).requiresCorrectToolForDrops().sound(SoundType.STONE))
               .transform(TagGen.pickaxeOnly()))
            .loot(
               (lt, b) -> {
                  RegistryLookup<Enchantment> enchantmentRegistryLookup = lt.getRegistries().lookupOrThrow(Registries.ENCHANTMENT);
                  lt.add(
                     b,
                     lt.createSilkTouchDispatchTable(
                        b,
                        (net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer.Builder)lt.applyExplosionDecay(
                           b,
                           LootItem.lootTableItem((ItemLike)AllItems.RAW_ZINC.get())
                              .apply(ApplyBonusCount.addOreBonusCount(enchantmentRegistryLookup.getOrThrow(Enchantments.FORTUNE)))
                        )
                     )
                  );
               }
            )
            .tag(new TagKey[]{BlockTags.NEEDS_IRON_TOOL})
            .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Blocks.ORES})
            .transform(
               TagGen.tagBlockAndItem(
                  Map.of(
                     CommonMetal.ZINC.ores.blocks(),
                     CommonMetal.ZINC.ores.items(),
                     net.neoforged.neoforge.common.Tags.Blocks.ORES_IN_GROUND_STONE,
                     net.neoforged.neoforge.common.Tags.Items.ORES_IN_GROUND_STONE
                  )
               )
            ))
         .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Items.ORES})
         .build())
      .register();
   public static final BlockEntry<Block> DEEPSLATE_ZINC_ORE = ((BlockBuilder)((ItemBuilder)((BlockBuilder)REGISTRATE.block("deepslate_zinc_ore", Block::new)
               .initialProperties(() -> Blocks.DEEPSLATE_GOLD_ORE)
               .properties(p -> p.mapColor(MapColor.STONE).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE))
               .transform(TagGen.pickaxeOnly()))
            .loot(
               (lt, b) -> {
                  RegistryLookup<Enchantment> enchantmentRegistryLookup = lt.getRegistries().lookupOrThrow(Registries.ENCHANTMENT);
                  lt.add(
                     b,
                     lt.createSilkTouchDispatchTable(
                        b,
                        (net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer.Builder)lt.applyExplosionDecay(
                           b,
                           LootItem.lootTableItem((ItemLike)AllItems.RAW_ZINC.get())
                              .apply(ApplyBonusCount.addOreBonusCount(enchantmentRegistryLookup.getOrThrow(Enchantments.FORTUNE)))
                        )
                     )
                  );
               }
            )
            .tag(new TagKey[]{BlockTags.NEEDS_IRON_TOOL})
            .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Blocks.ORES})
            .transform(
               TagGen.tagBlockAndItem(
                  Map.of(
                     CommonMetal.ZINC.ores.blocks(),
                     CommonMetal.ZINC.ores.items(),
                     net.neoforged.neoforge.common.Tags.Blocks.ORES_IN_GROUND_DEEPSLATE,
                     net.neoforged.neoforge.common.Tags.Items.ORES_IN_GROUND_DEEPSLATE
                  )
               )
            ))
         .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Items.ORES})
         .build())
      .register();
   public static final BlockEntry<Block> RAW_ZINC_BLOCK = ((BlockBuilder)((ItemBuilder)((BlockBuilder)REGISTRATE.block("raw_zinc_block", Block::new)
               .initialProperties(() -> Blocks.RAW_GOLD_BLOCK)
               .properties(p -> p.mapColor(MapColor.GLOW_LICHEN).requiresCorrectToolForDrops())
               .transform(TagGen.pickaxeOnly()))
            .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS})
            .tag(new TagKey[]{BlockTags.NEEDS_IRON_TOOL})
            .lang("Block of Raw Zinc")
            .transform(TagGen.tagBlockAndItem(CommonMetal.ZINC.rawStorageBlocks)))
         .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Items.STORAGE_BLOCKS})
         .build())
      .register();
   public static final BlockEntry<Block> ZINC_BLOCK = ((BlockBuilder)((ItemBuilder)((BlockBuilder)REGISTRATE.block("zinc_block", Block::new)
               .initialProperties(() -> Blocks.IRON_BLOCK)
               .properties(p -> p.mapColor(MapColor.GLOW_LICHEN).requiresCorrectToolForDrops())
               .transform(TagGen.pickaxeOnly()))
            .tag(new TagKey[]{BlockTags.NEEDS_IRON_TOOL})
            .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS})
            .tag(new TagKey[]{BlockTags.BEACON_BASE_BLOCKS})
            .transform(TagGen.tagBlockAndItem(CommonMetal.ZINC.storageBlocks)))
         .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Items.STORAGE_BLOCKS})
         .build())
      .lang("Block of Zinc")
      .register();
   public static final BlockEntry<Block> ANDESITE_ALLOY_BLOCK = ((BlockBuilder)((ItemBuilder)((BlockBuilder)REGISTRATE.block("andesite_alloy_block", Block::new)
               .initialProperties(() -> Blocks.ANDESITE)
               .properties(p -> p.mapColor(MapColor.STONE).requiresCorrectToolForDrops())
               .transform(TagGen.pickaxeOnly()))
            .blockstate(BlockStateGen.simpleCubeAll("andesite_block"))
            .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS})
            .transform(TagGen.tagBlockAndItem(AllTags.AllBlockTags.ANDESITE_ALLOY_STORAGE_BLOCKS.tag, AllTags.AllItemTags.ANDESITE_ALLOY_STORAGE_BLOCKS.tag)))
         .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Items.STORAGE_BLOCKS})
         .build())
      .lang("Block of Andesite Alloy")
      .register();
   public static final BlockEntry<Block> INDUSTRIAL_IRON_BLOCK = ((BlockBuilder)REGISTRATE.block("industrial_iron_block", Block::new)
         .transform(BuilderTransformers.palettesIronBlock()))
      .lang("Block of Industrial Iron")
      .register();
   public static final BlockEntry<Block> WEATHERED_IRON_BLOCK = ((BlockBuilder)REGISTRATE.block("weathered_iron_block", Block::new)
         .transform(BuilderTransformers.palettesIronBlock()))
      .lang("Block of Weathered Iron")
      .register();
   public static final BlockEntry<Block> BRASS_BLOCK = ((BlockBuilder)((ItemBuilder)((BlockBuilder)REGISTRATE.block("brass_block", Block::new)
               .initialProperties(() -> Blocks.IRON_BLOCK)
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW).requiresCorrectToolForDrops())
               .transform(TagGen.pickaxeOnly()))
            .blockstate(BlockStateGen.simpleCubeAll("brass_block"))
            .tag(new TagKey[]{BlockTags.NEEDS_IRON_TOOL})
            .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS})
            .tag(new TagKey[]{BlockTags.BEACON_BASE_BLOCKS})
            .transform(TagGen.tagBlockAndItem(CommonMetal.BRASS.storageBlocks)))
         .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Items.STORAGE_BLOCKS})
         .build())
      .lang("Block of Brass")
      .register();
   public static final BlockEntry<CardboardBlock> CARDBOARD_BLOCK = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("cardboard_block", CardboardBlock::new)
            .initialProperties(() -> Blocks.MUSHROOM_STEM)
            .properties(p -> p.mapColor(MapColor.COLOR_BROWN).sound(SoundType.CHISELED_BOOKSHELF).ignitedByLava())
            .transform(TagGen.axeOnly()))
         .blockstate(BlockStateGen.horizontalAxisBlockProvider(false))
         .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS})
         .tag(new TagKey[]{AllTags.AllBlockTags.CARDBOARD_STORAGE_BLOCKS.tag})
         .item()
         .burnTime(4000)
         .tag(new TagKey[]{AllTags.AllItemTags.CARDBOARD_STORAGE_BLOCKS.tag})
         .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Items.STORAGE_BLOCKS})
         .build())
      .lang("Block of Cardboard")
      .register();
   public static final BlockEntry<CardboardBlock> BOUND_CARDBOARD_BLOCK = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "bound_cardboard_block", CardboardBlock::new
            )
            .initialProperties(() -> Blocks.MUSHROOM_STEM)
            .properties(p -> p.mapColor(MapColor.COLOR_BROWN).sound(SoundType.CHISELED_BOOKSHELF).ignitedByLava())
            .transform(TagGen.axeOnly()))
         .blockstate(BlockStateGen.horizontalAxisBlockProvider(false))
         .loot(
            (r, b) -> r.add(
                  b,
                  LootTable.lootTable()
                     .withPool(
                        LootPool.lootPool()
                           .setRolls(ConstantValue.exactly(1.0F))
                           .add(
                              ((net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer.Builder)LootItem.lootTableItem(b)
                                    .when(((BlockLootSubProviderAccessor)r).create$hasSilkTouch()))
                                 .otherwise(
                                    (net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer.Builder)r.applyExplosionCondition(
                                       b, LootItem.lootTableItem(Items.STRING)
                                    )
                                 )
                           )
                     )
                     .withPool(
                        (net.minecraft.world.level.storage.loot.LootPool.Builder)r.applyExplosionCondition(
                           b,
                           LootPool.lootPool()
                              .setRolls(ConstantValue.exactly(1.0F))
                              .add(LootItem.lootTableItem(CARDBOARD_BLOCK.asItem()))
                              .when(((BlockLootSubProviderAccessor)r).create$hasSilkTouch().invert())
                        )
                     )
               )
         )
         .item()
         .burnTime(4000)
         .build())
      .lang("Bound Block of Cardboard")
      .register();
   public static final BlockEntry<ExperienceBlock> EXPERIENCE_BLOCK = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("experience_block", ExperienceBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(
               p -> p.mapColor(MapColor.PLANT)
                     .sound(
                        new DeferredSoundType(
                           1.0F,
                           0.5F,
                           () -> SoundEvents.AMETHYST_BLOCK_BREAK,
                           () -> SoundEvents.AMETHYST_BLOCK_STEP,
                           () -> SoundEvents.AMETHYST_BLOCK_PLACE,
                           () -> SoundEvents.AMETHYST_BLOCK_HIT,
                           () -> SoundEvents.AMETHYST_BLOCK_FALL
                        )
                     )
                     .requiresCorrectToolForDrops()
                     .lightLevel(s -> 15)
            )
            .blockstate((c, p) -> p.simpleBlock((Block)c.get(), AssetLookup.standardModel(c, p)))
            .transform(TagGen.pickaxeOnly()))
         .lang("Block of Experience")
         .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS})
         .tag(new TagKey[]{BlockTags.BEACON_BASE_BLOCKS})
         .item()
         .properties(p -> p.rarity(Rarity.UNCOMMON))
         .tag(new TagKey[]{net.neoforged.neoforge.common.Tags.Items.STORAGE_BLOCKS})
         .build())
      .register();
   public static final BlockEntry<RotatedPillarBlock> ROSE_QUARTZ_BLOCK = ((BlockBuilder)REGISTRATE.block("rose_quartz_block", RotatedPillarBlock::new)
         .initialProperties(() -> Blocks.AMETHYST_BLOCK)
         .properties(p -> p.mapColor(MapColor.TERRACOTTA_PINK).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE))
         .transform(TagGen.pickaxeOnly()))
      .blockstate((c, p) -> p.axisBlock((RotatedPillarBlock)c.get(), p.modLoc("block/palettes/rose_quartz_side"), p.modLoc("block/palettes/rose_quartz_top")))
      .recipe((c, p) -> p.stonecutting(DataIngredient.items((Item)AllItems.ROSE_QUARTZ.get(), new Item[0]), RecipeCategory.BUILDING_BLOCKS, c::get, 2))
      .simpleItem()
      .lang("Block of Rose Quartz")
      .register();
   public static final BlockEntry<Block> ROSE_QUARTZ_TILES = ((BlockBuilder)REGISTRATE.block("rose_quartz_tiles", Block::new)
         .initialProperties(() -> Blocks.DEEPSLATE)
         .properties(p -> p.mapColor(MapColor.TERRACOTTA_PINK).requiresCorrectToolForDrops())
         .transform(TagGen.pickaxeOnly()))
      .blockstate(BlockStateGen.simpleCubeAll("palettes/rose_quartz_tiles"))
      .recipe((c, p) -> p.stonecutting(DataIngredient.items((Item)AllItems.POLISHED_ROSE_QUARTZ.get(), new Item[0]), RecipeCategory.BUILDING_BLOCKS, c::get, 2))
      .simpleItem()
      .register();
   public static final BlockEntry<Block> SMALL_ROSE_QUARTZ_TILES = ((BlockBuilder)REGISTRATE.block("small_rose_quartz_tiles", Block::new)
         .initialProperties(() -> Blocks.DEEPSLATE)
         .properties(p -> p.mapColor(MapColor.TERRACOTTA_PINK).requiresCorrectToolForDrops())
         .transform(TagGen.pickaxeOnly()))
      .blockstate(BlockStateGen.simpleCubeAll("palettes/small_rose_quartz_tiles"))
      .recipe((c, p) -> p.stonecutting(DataIngredient.items((Item)AllItems.POLISHED_ROSE_QUARTZ.get(), new Item[0]), RecipeCategory.BUILDING_BLOCKS, c::get, 2))
      .simpleItem()
      .register();
   public static final CopperBlockSet COPPER_SHINGLES = new CopperBlockSet(
      REGISTRATE,
      "copper_shingles",
      "copper_roof_top",
      CopperBlockSet.DEFAULT_VARIANTS,
      (c, p) -> p.stonecutting(DataIngredient.tag(CommonMetal.COPPER.ingots), RecipeCategory.BUILDING_BLOCKS, c::get, 2),
      (ws, block) -> CreateRegistrate.connectedTextures(() -> new RoofBlockCTBehaviour(AllSpriteShifts.COPPER_SHINGLES.get(ws))).accept(block)
   );
   public static final CopperBlockSet COPPER_TILES = new CopperBlockSet(
      REGISTRATE,
      "copper_tiles",
      "copper_roof_top",
      CopperBlockSet.DEFAULT_VARIANTS,
      (c, p) -> p.stonecutting(DataIngredient.tag(CommonMetal.COPPER.ingots), RecipeCategory.BUILDING_BLOCKS, c::get, 2),
      (ws, block) -> CreateRegistrate.connectedTextures(() -> new RoofBlockCTBehaviour(AllSpriteShifts.COPPER_TILES.get(ws))).accept(block)
   );

   public static void register() {
   }

   static {
      REGISTRATE.setCreativeTab(AllCreativeModeTabs.BASE_CREATIVE_TAB);
      REGISTRATE.setCreativeTab(AllCreativeModeTabs.PALETTES_CREATIVE_TAB);
   }
}
