package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlock;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.fluids.FluidStack;

@GameTestGroup(
   path = "contraptions"
)
public class TestContraptions {
   @GameTest(
      template = "arrow_dispenser",
      timeoutTicks = 200
   )
   public static void arrowDispenser(CreateGameTestHelper helper) {
      BlockPos lever = new BlockPos(2, 3, 1);
      helper.pullLever(lever);
      BlockPos pos1 = new BlockPos(0, 5, 0);
      BlockPos pos2 = new BlockPos(4, 5, 4);
      helper.succeedWhen(() -> {
         helper.assertSecondsPassed(7);
         List<Arrow> arrows = helper.getEntitiesBetween(EntityType.ARROW, pos1, pos2);
         if (arrows.size() != 4) {
            helper.fail("Expected 4 arrows");
         }

         helper.powerLever(lever);
         BlockPos dispenser = new BlockPos(2, 5, 2);
         helper.assertContainerContains(dispenser, Items.ARROW);
      });
   }

   @GameTest(
      template = "crop_farming",
      timeoutTicks = 200
   )
   public static void cropFarming(CreateGameTestHelper helper) {
      BlockPos lever = new BlockPos(4, 3, 1);
      helper.pullLever(lever);
      BlockPos output = new BlockPos(1, 3, 12);
      helper.succeedWhen(() -> helper.assertAnyContained(output, Items.WHEAT, Items.POTATO, Items.CARROT));
   }

   @GameTest(
      template = "mounted_item_extract",
      timeoutTicks = 400
   )
   public static void mountedItemExtract(CreateGameTestHelper helper) {
      BlockPos barrel = new BlockPos(1, 3, 2);
      Object2LongMap<Item> content = helper.getItemContent(barrel);
      BlockPos lever = new BlockPos(1, 5, 1);
      helper.pullLever(lever);
      BlockPos outputPos = new BlockPos(4, 2, 1);
      helper.succeedWhen(() -> {
         helper.assertContentPresent(content, outputPos);
         helper.powerLever(lever);
         helper.assertContainerEmpty(barrel);
      });
   }

   @GameTest(
      template = "mounted_fluid_drain",
      timeoutTicks = 200
   )
   public static void mountedFluidDrain(CreateGameTestHelper helper) {
      BlockPos tank = new BlockPos(1, 3, 2);
      FluidStack fluid = helper.getTankContents(tank);
      if (fluid.isEmpty()) {
         helper.fail("Tank empty");
      }

      BlockPos lever = new BlockPos(1, 5, 1);
      helper.pullLever(lever);
      BlockPos output = new BlockPos(4, 2, 1);
      helper.succeedWhen(() -> {
         helper.assertFluidPresent(fluid, output);
         helper.powerLever(lever);
         helper.assertTankEmpty(tank);
      });
   }

   @GameTest(
      template = "ploughing"
   )
   public static void ploughing(CreateGameTestHelper helper) {
      BlockPos dirt = new BlockPos(4, 2, 1);
      BlockPos lever = new BlockPos(3, 3, 2);
      helper.pullLever(lever);
      helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.FARMLAND, dirt));
   }

   @GameTest(
      template = "redstone_contacts"
   )
   public static void redstoneContacts(CreateGameTestHelper helper) {
      BlockPos end = new BlockPos(5, 10, 1);
      BlockPos lever = new BlockPos(1, 3, 2);
      helper.pullLever(lever);
      helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, end));
   }

   @GameTest(
      template = "controls",
      timeoutTicks = 200
   )
   public static void controls(CreateGameTestHelper helper) {
      BlockPos button = new BlockPos(5, 5, 4);
      BlockPos gearshift = new BlockPos(4, 5, 4);
      BlockPos bearingPos = new BlockPos(4, 4, 4);
      AtomicInteger step = new AtomicInteger(1);
      List<BlockPos> dirt = List.of(new BlockPos(4, 2, 6), new BlockPos(2, 2, 4), new BlockPos(4, 2, 2));
      List<BlockPos> wheat = List.of(new BlockPos(4, 3, 7), new BlockPos(1, 3, 4), new BlockPos(4, 3, 1));
      helper.whenSecondsPassed(1, () -> helper.pressButton(button));
      helper.succeedWhen(
         () -> {
            helper.assertBlockPresent(Blocks.STONE_BUTTON, button);
            helper.assertBlockProperty(gearshift, SequencedGearshiftBlock.STATE, 0);
            if (step.get() != 4) {
               MechanicalBearingBlockEntity bearing = helper.getBlockEntity(
                  (BlockEntityType<MechanicalBearingBlockEntity>)AllBlockEntityTypes.MECHANICAL_BEARING.get(), bearingPos
               );
               if (bearing.getMovedContraption() == null) {
                  helper.fail("Contraption not assembled");
               }

               Contraption contraption = bearing.getMovedContraption().getContraption();
               switch (step.get()) {
                  case 1:
                     helper.assertBlockPresent(Blocks.FARMLAND, dirt.get(0));
                     helper.assertBlockProperty(wheat.get(0), CropBlock.AGE, 0);
                     helper.toggleActorsOfType(contraption, (ItemLike)AllBlocks.MECHANICAL_HARVESTER.get());
                     helper.pressButton(button);
                     step.incrementAndGet();
                     helper.fail("Entering step 2");
                     break;
                  case 2:
                     helper.assertBlockPresent(Blocks.FARMLAND, dirt.get(1));
                     helper.assertBlockProperty(wheat.get(1), CropBlock.AGE, 7);
                     helper.toggleActorsOfType(contraption, (ItemLike)AllBlocks.MECHANICAL_PLOUGH.get());
                     helper.pressButton(button);
                     step.incrementAndGet();
                     helper.fail("Entering step 3");
                     break;
                  case 3:
                     helper.assertBlockPresent(Blocks.DIRT, dirt.get(2));
                     helper.assertBlockProperty(wheat.get(2), CropBlock.AGE, 7);
                     helper.pressButton(button);
                     step.incrementAndGet();
                     helper.fail("Entering step 4");
               }
            }
         }
      );
   }

   @GameTest(
      template = "elevator"
   )
   public static void elevator(CreateGameTestHelper helper) {
      BlockPos pulley = new BlockPos(5, 12, 3);
      BlockPos secondaryPulley = new BlockPos(5, 12, 1);
      BlockPos bottomLamp = new BlockPos(2, 3, 2);
      BlockPos topLamp = new BlockPos(2, 12, 2);
      BlockPos lever = new BlockPos(1, 11, 2);
      BlockPos elevatorStart = new BlockPos(4, 2, 2);
      BlockPos cowSpawn = new BlockPos(4, 4, 2);
      BlockPos cowEnd = new BlockPos(4, 13, 2);
      helper.runAtTickTime(1L, () -> helper.spawn(EntityType.COW, cowSpawn));
      helper.runAtTickTime(
         15L,
         () -> helper.<ElevatorPulleyBlockEntity>getBlockEntity((BlockEntityType<ElevatorPulleyBlockEntity>)AllBlockEntityTypes.ELEVATOR_PULLEY.get(), pulley)
               .clicked()
      );
      helper.succeedWhen(
         () -> {
            helper.assertSecondsPassed(1);
            if (!(Boolean)helper.getBlockState(lever).getValue(LeverBlock.POWERED)) {
               helper.getFirstEntity((EntityType)AllEntityTypes.CONTROLLED_CONTRAPTION.get(), elevatorStart);
               helper.assertBlockProperty(topLamp, RedstoneLampBlock.LIT, false);
               helper.assertBlockProperty(bottomLamp, RedstoneLampBlock.LIT, true);
               ElevatorPulleyBlockEntity secondary = helper.getBlockEntity(
                  (BlockEntityType<ElevatorPulleyBlockEntity>)AllBlockEntityTypes.ELEVATOR_PULLEY.get(), secondaryPulley
               );
               if (secondary.getMirrorParent() == null) {
                  helper.fail("Secondary pulley has no parent");
               }

               helper.pullLever(lever);
               helper.fail("Entering step 2");
            } else {
               helper.assertBlockProperty(topLamp, RedstoneLampBlock.LIT, true);
               helper.assertBlockProperty(bottomLamp, RedstoneLampBlock.LIT, false);
               helper.assertEntityPresent(EntityType.COW, cowEnd);
               helper.<ElevatorPulleyBlockEntity>getBlockEntity((BlockEntityType<ElevatorPulleyBlockEntity>)AllBlockEntityTypes.ELEVATOR_PULLEY.get(), pulley)
                  .clicked();
            }
         }
      );
   }

   @GameTest(
      template = "roller_filling"
   )
   public static void rollerFilling(CreateGameTestHelper helper) {
      BlockPos lever = new BlockPos(7, 6, 1);
      BlockPos barrelEnd = new BlockPos(2, 5, 2);
      List<BlockPos> existing = BlockPos.betweenClosedStream(new BlockPos(1, 3, 2), new BlockPos(4, 2, 2)).toList();
      List<BlockPos> filled = BlockPos.betweenClosedStream(new BlockPos(1, 2, 1), new BlockPos(4, 3, 3)).filter(pos -> !existing.contains(pos)).toList();
      List<BlockPos> tracks = BlockPos.betweenClosedStream(new BlockPos(1, 4, 2), new BlockPos(4, 4, 2)).toList();
      helper.pullLever(lever);
      helper.succeedWhen(() -> {
         helper.assertSecondsPassed(4);
         existing.forEach(pos -> helper.assertBlockPresent((Block)AllBlocks.RAILWAY_CASING.get(), pos));
         filled.forEach(pos -> helper.assertBlockPresent((Block)AllBlocks.ANDESITE_CASING.get(), pos));
         tracks.forEach(pos -> helper.assertBlockPresent((Block)AllBlocks.TRACK.get(), pos));
         helper.assertContainerEmpty(barrelEnd);
      });
   }

   @GameTest(
      template = "roller_paving_and_clearing",
      timeoutTicks = 200
   )
   public static void rollerPavingAndClearing(CreateGameTestHelper helper) {
      BlockPos lever = new BlockPos(8, 5, 1);
      List<BlockPos> paved = BlockPos.betweenClosedStream(new BlockPos(1, 2, 1), new BlockPos(4, 2, 1)).toList();
      BlockPos cleared = new BlockPos(2, 3, 1);
      helper.pullLever(lever);
      helper.succeedWhen(() -> {
         helper.assertSecondsPassed(9);
         paved.forEach(pos -> helper.assertBlockPresent((Block)AllBlocks.ANDESITE_CASING.get(), pos));
         helper.assertBlockPresent(Blocks.AIR, cleared);
      });
   }

   @GameTest(
      template = "dispensers_dont_fight"
   )
   public static void dispensersDontFight(CreateGameTestHelper helper) {
      BlockPos lever = new BlockPos(2, 3, 1);
      BlockPos bottom = new BlockPos(6, 4, 1);
      BlockPos top = new BlockPos(6, 6, 1);
      BlockPos dispenser = new BlockPos(3, 4, 1);
      helper.pullLever(lever);
      helper.succeedWhen(() -> {
         helper.assertEntitiesPresent(EntityType.ARROW, bottom, 3, 0.0);
         helper.assertEntityNotPresent(EntityType.ARROW, top);
         helper.assertBlockPresent(Blocks.DISPENSER, dispenser);
         helper.assertContainerContains(dispenser, new ItemStack(Items.ARROW, 2));
      });
   }

   @GameTest(
      template = "dispensers_refill"
   )
   public static void dispensersRefill(CreateGameTestHelper helper) {
      BlockPos lever = new BlockPos(2, 3, 1);
      helper.pullLever(lever);
      BlockPos barrel = lever.above();
      BlockPos dispenser = barrel.east();
      helper.succeedWhen(() -> {
         helper.assertBlockPresent(Blocks.DISPENSER, dispenser);
         helper.assertContainerContains(dispenser, new ItemStack(Items.SPECTRAL_ARROW, 2));
         helper.assertContainerEmpty(barrel);
      });
   }

   @GameTest(
      template = "vaults_protect_fuel"
   )
   public static void vaultsProtectFuel(CreateGameTestHelper helper) {
      BlockPos lever = new BlockPos(2, 2, 1);
      helper.pullLever(lever);
      BlockPos barrelLamp = new BlockPos(1, 3, 3);
      BlockPos vaultLamp = barrelLamp.east(2);
      helper.runAtTickTime(10L, () -> helper.pullLever(lever));
      helper.succeedWhen(() -> {
         helper.assertBlockProperty(barrelLamp, RedstoneLampBlock.LIT, false);
         helper.assertBlockProperty(vaultLamp, RedstoneLampBlock.LIT, true);
      });
   }
}
