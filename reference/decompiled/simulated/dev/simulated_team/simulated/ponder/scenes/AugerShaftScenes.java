package dev.simulated_team.simulated.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.logistics.funnel.AndesiteFunnelBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock.Shape;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder.WorldInstructions;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerCogBlock;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftBlock;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.ponder.instructions.AirflowAABBInstruction;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.OverlayInstructions;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.api.scene.SelectionUtil;
import net.createmod.ponder.api.scene.VectorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AugerShaftScenes {
   public static void augerShaftIntro(SceneBuilder builder, SceneBuildingUtil util) {
      CreateSceneBuilder scene = new CreateSceneBuilder(builder);
      scene.title("auger_shaft_intro", "Using the Auger Shaft");
      scene.configureBasePlate(0, 0, 5);
      scene.showBasePlate();
      WorldInstructions world = scene.world();
      SelectionUtil select = util.select();
      OverlayInstructions overlay = scene.overlay();
      VectorUtil vector = util.vector();
      BlockPos augerCog = util.grid().at(2, 2, 3);
      Selection augerShafts = select.fromTo(2, 2, 1, 2, 2, 4);
      BlockPos inputFunnel = util.grid().at(2, 3, 1);
      BlockPos invalidFunnel = util.grid().at(2, 2, 0);
      BlockPos invalidSideFunnel = util.grid().at(1, 2, 3);
      Selection validFunnel = select.fromTo(1, 1, 4, 1, 2, 4);
      Selection smallCogs = select.fromTo(2, 1, 5, 2, 2, 5);
      BlockPos largeCog = util.grid().at(1, 0, 5);
      Selection andesiteCasing = select.fromTo(2, 1, 1, 2, 1, 4);
      world.setKineticSpeed(select.position(largeCog), 16.0F);
      world.setKineticSpeed(select.position(2, 1, 5), -32.0F);
      world.setKineticSpeed(select.position(2, 2, 5), 32.0F);
      world.setKineticSpeed(augerShafts, 32.0F);
      world.showSection(smallCogs, Direction.UP);
      world.showSection(select.position(largeCog), Direction.UP);
      world.showSection(andesiteCasing, Direction.UP);
      scene.idle(5);
      world.showSection(augerShafts, Direction.DOWN);
      scene.idle(10);
      world.showSection(select.position(inputFunnel), Direction.DOWN);
      world.setBlock(inputFunnel, (BlockState)AllBlocks.ANDESITE_FUNNEL.getDefaultState().setValue(AndesiteFunnelBlock.FACING, Direction.UP), false);
      scene.idle(12);
      world.modifyBlock(new BlockPos(2, 2, 1), s -> (BlockState)s.setValue(AugerShaftBlock.UP, true), false);
      scene.idle(8);
      overlay.showText(80)
         .attachKeyFrame()
         .text("Powered Auger Shafts will transport items and extract them from actors")
         .placeNearTarget()
         .pointAt(vector.blockSurface(util.grid().at(2, 2, 2), Direction.WEST));

      for (int i = 0; i < 3; i++) {
         ItemStack stack = new ItemStack(Items.COPPER_BLOCK);
         ElementLink<EntityElement> remove = world.createItemEntity(new Vec3(2.5, 5.0, 1.5), Vec3.ZERO, stack);
         scene.idle(9);
         world.modifyEntity(remove, Entity::discard);
         scene.idle(10);
      }

      scene.idle(30);
      world.setBlock(inputFunnel, Blocks.AIR.defaultBlockState(), true);
      world.modifyBlock(new BlockPos(2, 2, 1), s -> (BlockState)s.setValue(AugerShaftBlock.UP, false), false);
      scene.idle(20);
      scene.addKeyframe();
      scene.overlay().showControls(vector.topOf(augerCog), Pointing.DOWN, 80).rightClick().withItem(AllItems.WRENCH.asStack());
      scene.idle(7);
      world.modifyBlock(augerCog.north(), s -> (BlockState)s.setValue(AugerShaftBlock.SECTION, AugerShaftBlock.BarrelSection.FRONT), false);
      world.modifyBlock(augerCog.south(), s -> (BlockState)s.setValue(AugerShaftBlock.SECTION, AugerShaftBlock.BarrelSection.SINGLE), false);
      world.setBlock(augerCog, (BlockState)SimBlocks.AUGER_COG.getDefaultState().setValue(AugerCogBlock.AXIS, Axis.Z), true);
      overlay.showText(80)
         .text("Using a Wrench, you can cycle between the Auger Shaft and Auger Cog")
         .placeNearTarget()
         .pointAt(vector.blockSurface(augerCog, Direction.UP));
      scene.idle(100);
      scene.overlay().showControls(vector.topOf(augerCog.north()), Pointing.DOWN, 80).rightClick().withItem(AllBlocks.INDUSTRIAL_IRON_BLOCK.asStack());
      scene.idle(7);
      world.modifyBlock(
         augerCog.north(),
         s -> (BlockState)((BlockState)s.setValue(AugerShaftBlock.SECTION, AugerShaftBlock.BarrelSection.FRONT)).setValue(AugerShaftBlock.ENCASED, true),
         true
      );
      overlay.showText(80)
         .text("Using Industrial Iron Blocks, Auger Shafts can be encased")
         .placeNearTarget()
         .pointAt(vector.blockSurface(augerCog.north(), Direction.UP));
      scene.idle(100);
      Vec3 augerCenter = new Vec3(2.5, 2.5, 5.0);
      AABB bb = new AABB(augerCenter, augerCenter).inflate(0.5, 0.5, 0.0);
      scene.addInstruction(new AirflowAABBInstruction(PonderPalette.GREEN, bb.expandTowards(0.0, 0.0, -4.0), 150, Direction.SOUTH, 0.75F, 1.5F));
      overlay.showText(60)
         .attachKeyFrame()
         .text("Item flow can be observed via the indicator on the Auger Cog...")
         .placeNearTarget()
         .pointAt(vector.blockSurface(augerCog, Direction.WEST));
      scene.idle(80);
      scene.overlay().showControls(new Vec3(3.0, 2.5, 2.5), Pointing.RIGHT, 60).withItem(AllItems.GOGGLES.asStack());
      overlay.showText(60).text("...Or by inspecting the Auger while wearing Goggles").placeNearTarget().pointAt(new Vec3(2.0, 2.5, 2.5));
      scene.idle(85);
      world.setBlock(invalidFunnel, (BlockState)AllBlocks.ANDESITE_FUNNEL.getDefaultState().setValue(AndesiteFunnelBlock.FACING, Direction.NORTH), false);
      world.setBlock(
         invalidSideFunnel,
         (BlockState)((BlockState)AllBlocks.ANDESITE_FUNNEL.getDefaultState().setValue(AndesiteFunnelBlock.FACING, Direction.WEST))
            .setValue(AndesiteFunnelBlock.EXTRACTING, true),
         false
      );
      world.setBlock(
         new BlockPos(1, 2, 4),
         (BlockState)((BlockState)AllBlocks.ANDESITE_BELT_FUNNEL.getDefaultState().setValue(BeltFunnelBlock.HORIZONTAL_FACING, Direction.WEST))
            .setValue(BeltFunnelBlock.SHAPE, Shape.PUSHING),
         false
      );
      world.showSection(select.position(invalidFunnel), Direction.DOWN);
      world.showSection(select.position(invalidSideFunnel), Direction.DOWN);
      scene.idle(10);
      overlay.showOutlineWithText(select.position(invalidSideFunnel), 60)
         .attachKeyFrame()
         .colored(PonderPalette.RED)
         .text("Items cannot be inserted or extracted from the Auger Cog...")
         .pointAt(invalidSideFunnel.getCenter())
         .placeNearTarget();
      scene.idle(80);
      overlay.showOutlineWithText(select.position(invalidFunnel), 60)
         .colored(PonderPalette.RED)
         .text("...or either end of the Auger")
         .pointAt(invalidFunnel.getCenter())
         .placeNearTarget();
      scene.idle(80);
      world.showSection(validFunnel, Direction.DOWN);
      scene.idle(12);
      world.modifyBlock(new BlockPos(2, 2, 4), s -> (BlockState)s.setValue(AugerShaftBlock.WEST, true), false);
      world.createItemOnBeltLike(new BlockPos(1, 1, 4), Direction.EAST, new ItemStack(Items.COPPER_BLOCK));
   }

   public static void augerShaftExtracting(SceneBuilder builder, SceneBuildingUtil util) {
      CreateSceneBuilder scene = new CreateSceneBuilder(builder);
      scene.title("auger_shaft_extracting", "Extracting from Harvesting actors using the Auger Shaft");
      scene.configureBasePlate(0, 0, 7);
      scene.showBasePlate();
      WorldInstructions world = scene.world();
      SelectionUtil select = util.select();
      OverlayInstructions overlay = scene.overlay();
      VectorUtil vector = util.vector();
      BlockPos largeCog = util.grid().at(7, 0, 3);
      Selection kineticsShaft = select.fromTo(7, 1, 4, 4, 1, 3);
      Selection allCogs = select.fromTo(2, 1, 3, 4, 2, 3);
      Selection bottomActors = select.fromTo(2, 1, 2, 4, 1, 2);
      Selection allActors = select.fromTo(2, 1, 2, 4, 2, 2);
      Selection bottomAuger = select.fromTo(3, 1, 3, 3, 1, 5);
      Selection topAuger = select.fromTo(3, 2, 3, 3, 2, 5);
      Selection planks = select.fromTo(2, 1, 1, 4, 2, 1);
      Selection andesiteCasing = select.fromTo(3, 1, 4, 3, 1, 5);
      Selection funnel = select.fromTo(2, 1, 5, 2, 2, 5);
      world.setKineticSpeed(bottomAuger, 0.0F);
      world.showSection(bottomAuger, Direction.DOWN);
      scene.idle(15);
      world.showSection(bottomActors, Direction.DOWN);
      overlay.showText(180)
         .attachKeyFrame()
         .text("Auger Shafts collect materials from Harvesting actors")
         .placeNearTarget()
         .pointAt(vector.blockSurface(new BlockPos(2, 1, 2), Direction.WEST));
      scene.idle(20);
      BlockState[] states = new BlockState[]{
         AllBlocks.MECHANICAL_HARVESTER.getDefaultState(), AllBlocks.MECHANICAL_SAW.getDefaultState(), AllBlocks.MECHANICAL_DRILL.getDefaultState()
      };

      for (BlockState state : states) {
         scene.idle(20);
         world.hideSection(bottomActors, Direction.EAST);
         scene.idle(20);
         world.setBlocks(bottomActors, state, false);
         world.showSection(bottomActors, Direction.EAST);
      }

      scene.idle(40);
      world.hideSection(bottomActors, Direction.UP);
      world.hideSection(bottomAuger, Direction.UP);
      scene.idle(20);
      world.setBlock(
         new BlockPos(3, 1, 3), (BlockState)AllBlocks.ANDESITE_ENCASED_COGWHEEL.getDefaultState().setValue(EncasedCogwheelBlock.AXIS, Axis.Z), false
      );
      world.setBlocks(andesiteCasing, AllBlocks.ANDESITE_CASING.getDefaultState(), false);
      world.setKineticSpeed(select.position(3, 1, 3), -32.0F);
      world.setKineticSpeed(select.position(3, 1, 4), -32.0F);
      world.setKineticSpeed(select.position(2, 1, 2), 32.0F);
      world.setKineticSpeed(select.position(3, 1, 2), -32.0F);
      world.setKineticSpeed(select.position(4, 1, 2), 32.0F);
      world.showSection(andesiteCasing, Direction.DOWN);
      world.showSection(topAuger, Direction.DOWN);
      world.showSection(allActors, Direction.DOWN);
      world.showSection(select.position(largeCog), Direction.DOWN);
      world.showSection(kineticsShaft, Direction.DOWN);
      world.showSection(allCogs, Direction.DOWN);
      scene.idle(20);
      AABB bb = new AABB(3.0, 2.0, 3.0, 4.0, 3.0, 3.0);
      overlay.chaseBoundingBoxOutline(PonderPalette.INPUT, bb, bb.expandTowards(0.0, 0.0, 0.0), 5);
      overlay.chaseBoundingBoxOutline(PonderPalette.INPUT, bb, bb.expandTowards(0.0, 0.0, -1.0), 15);
      scene.idle(15);
      overlay.chaseBoundingBoxOutline(PonderPalette.INPUT, bb, bb.inflate(1.0, 0.0, 0.0).expandTowards(0.0, -1.0, -1.0), 70);
      overlay.showText(70)
         .attachKeyFrame()
         .text("All horizontally connected actors are included")
         .placeNearTarget()
         .pointAt(vector.blockSurface(new BlockPos(2, 2, 2), Direction.WEST));
      scene.idle(80);
      scene.rotateCameraY(-90.0F);
      scene.idle(30);
      world.showSection(planks, Direction.DOWN);
      scene.idle(30);
      BlockPos p1 = util.grid().at(2, 1, 1);
      BlockPos p2 = util.grid().at(3, 1, 1);
      BlockPos p3 = util.grid().at(4, 1, 1);
      BlockPos p4 = util.grid().at(2, 2, 1);
      BlockPos p5 = util.grid().at(3, 2, 1);
      BlockPos p6 = util.grid().at(4, 2, 1);

      for (int i = 0; i < 10; i++) {
         scene.idle(10);
         world.incrementBlockBreakingProgress(p1);
         world.incrementBlockBreakingProgress(p2);
         world.incrementBlockBreakingProgress(p3);
         world.incrementBlockBreakingProgress(p4);
         world.incrementBlockBreakingProgress(p5);
         world.incrementBlockBreakingProgress(p6);
         if (i == 1) {
            overlay.showText(70)
               .attachKeyFrame()
               .colored(PonderPalette.INPUT)
               .text("When a connected actor breaks a block...")
               .placeNearTarget()
               .pointAt(vector.blockSurface(new BlockPos(3, 2, 2), Direction.UP));
         }
      }

      scene.idle(10);
      scene.addInstruction(new AirflowAABBInstruction(PonderPalette.GREEN, bb.expandTowards(0.0, 0.0, 3.0), 110, Direction.SOUTH, 0.75F, 1.5F));
      scene.idle(40);
      overlay.showText(70)
         .colored(PonderPalette.OUTPUT)
         .text("...its items are automatically collected")
         .placeNearTarget()
         .pointAt(vector.blockSurface(new BlockPos(3, 2, 4), Direction.EAST));
      scene.idle(70);
      world.setBlock(
         new BlockPos(2, 2, 5),
         (BlockState)((BlockState)AllBlocks.ANDESITE_BELT_FUNNEL.getDefaultState().setValue(BeltFunnelBlock.HORIZONTAL_FACING, Direction.WEST))
            .setValue(BeltFunnelBlock.SHAPE, Shape.PUSHING),
         false
      );
      world.showSection(funnel, Direction.DOWN);
      scene.idle(12);
      world.modifyBlock(new BlockPos(3, 2, 5), s -> (BlockState)s.setValue(AugerShaftBlock.WEST, true), false);
      world.createItemOnBeltLike(new BlockPos(2, 1, 5), Direction.EAST, new ItemStack(Items.OAK_PLANKS));
   }
}
