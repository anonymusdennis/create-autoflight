package dev.simulated_team.simulated.ponder.scenes;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder.EffectInstructions;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder.WorldInstructions;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorBlockEntity;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlock;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import dev.simulated_team.simulated.ponder.SmoothMovementUtils;
import dev.simulated_team.simulated.ponder.instructions.ChasingLineInstruction;
import dev.simulated_team.simulated.ponder.instructions.CustomAnimateWorldSectionInstruction;
import dev.simulated_team.simulated.ponder.instructions.PullTheAssemblerKronkInstruction;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.OverlayInstructions;
import net.createmod.ponder.api.scene.PositionUtil;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.api.scene.SelectionUtil;
import net.createmod.ponder.api.scene.VectorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RedstoneScenes {
   public static void modulatingReceiver(SceneBuilder builder, SceneBuildingUtil util) {
      CreateSceneBuilder scene = new CreateSceneBuilder(builder);
      WorldInstructions world = scene.world();
      SelectionUtil select = util.select();
      OverlayInstructions overlay = scene.overlay();
      EffectInstructions effects = scene.effects();
      VectorUtil vector = util.vector();
      PositionUtil grid = util.grid();
      scene.title("modulating_receiver", "Using Modulating Linked Receivers");
      scene.configureBasePlate(0, 0, 7);
      world.showSection(select.layer(0), Direction.UP);
      BlockPos modulatingLink = new BlockPos(1, 1, 3);
      BlockPos nixieTube = new BlockPos(0, 1, 4);
      BlockPos mainDust = new BlockPos(0, 1, 3);
      Selection link1 = select.fromTo(1, 1, 1, 3, 1, 1);
      Selection link2 = select.fromTo(4, 1, 3, 5, 1, 4);
      Selection link3 = select.fromTo(3, 1, 5, 3, 2, 6);
      Selection link4 = select.fromTo(6, 1, 5, 6, 2, 6);
      world.showSection(link1, Direction.UP);
      world.showSection(link2, Direction.UP);
      scene.idle(10);
      world.showSection(select.fromTo(3, 1, 5, 6, 2, 6), Direction.DOWN);
      scene.idle(5);
      world.showSection(select.position(mainDust), Direction.DOWN);
      world.showSection(select.position(nixieTube), Direction.DOWN);
      scene.idle(5);
      world.showSection(select.position(modulatingLink), Direction.DOWN);
      scene.idle(20);
      Vec3 top = new Vec3(1.5, 1.5, 3.5);
      Selection linkSelection = select.position(modulatingLink);
      overlay.showText(50)
         .attachKeyFrame()
         .text("Modulating Linked Receivers can detect the distance to other nearby Redstone Links")
         .placeNearTarget()
         .pointAt(top);
      scene.idle(60);
      overlay.showControls(vector.topOf(modulatingLink), Pointing.DOWN, 40).rightClick();
      scene.idle(7);
      overlay.showOutlineWithText(linkSelection, 50)
         .colored(PonderPalette.BLUE)
         .text("Right-click it to open the Configuration UI")
         .pointAt(top)
         .placeNearTarget();
      scene.idle(65);
      overlay.showText(50).attachKeyFrame().text("This allows setting a minimum range...").pointAt(top).placeNearTarget();
      scene.idle(20);
      Vec3 v = vector.blockSurface(modulatingLink, Direction.DOWN);
      AABB bb = new AABB(v, v);
      overlay.chaseBoundingBoxOutline(PonderPalette.OUTPUT, bb, bb, 1);
      overlay.chaseBoundingBoxOutline(PonderPalette.OUTPUT, bb, bb.expandTowards(2.5, 0.0, 0.0), 30);
      scene.idle(10);

      for (int i = 0; i < 8; i++) {
         double angle = Math.PI * ((double)i / 4.0 - 0.125);
         Vec3 relPos = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)).scale(2.70575).add(1.5, 1.0, 3.5);
         Vec3 relPos2 = new Vec3(Math.cos(angle + (Math.PI / 4)), 0.0, Math.sin(angle + (Math.PI / 4))).scale(2.70575).add(1.5, 1.0, 3.5);
         overlay.showLine(PonderPalette.OUTPUT, relPos, relPos2, 230 - i);
         scene.idle(1);
      }

      scene.idle(25);
      overlay.showText(50).attachKeyFrame().text("...and a maximum range").pointAt(top).placeNearTarget();
      scene.idle(20);
      overlay.chaseBoundingBoxOutline(PonderPalette.OUTPUT, bb, bb, 1);
      overlay.chaseBoundingBoxOutline(PonderPalette.OUTPUT, bb, bb.expandTowards(4.5, 0.0, 0.0), 30);
      scene.idle(10);

      for (int i = 0; i < 8; i++) {
         double angle = Math.PI * ((double)i / 4.0 - 0.125);
         Vec3 relPos = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)).scale(4.87035).add(1.5, 1.0, 3.5);
         Vec3 relPos2 = new Vec3(Math.cos(angle + (Math.PI / 4)), 0.0, Math.sin(angle + (Math.PI / 4))).scale(4.87035).add(1.5, 1.0, 3.5);
         overlay.showLine(PonderPalette.OUTPUT, relPos, relPos2, 40 - i);
         scene.idle(1);
      }

      scene.idle(50);
      world.toggleRedstonePower(link1);
      effects.indicateRedstone(new BlockPos(3, 1, 1));
      scene.idle(5);
      world.toggleRedstonePower(select.position(modulatingLink));
      world.modifyBlock(mainDust, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, 15), false);
      world.modifyBlockEntityNBT(select.position(nixieTube), NixieTubeBlockEntity.class, nbt -> nbt.putInt("RedstoneStrength", 15));
      effects.indicateRedstone(modulatingLink);
      Vec3 link1Vec = vector.blockSurface(new BlockPos(1, 1, 1), Direction.DOWN).add(0.0, 0.1875, 0.0);
      scene.idle(15);
      overlay.showText(50)
         .attachKeyFrame()
         .text("Redstone Links within the minimum range will be received at their full signal strength")
         .pointAt(link1Vec)
         .placeNearTarget();
      scene.idle(70);
      world.toggleRedstonePower(link1);
      scene.idle(5);
      world.toggleRedstonePower(select.position(modulatingLink));
      world.modifyBlock(mainDust, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, 0), false);
      world.modifyBlockEntityNBT(select.position(nixieTube), NixieTubeBlockEntity.class, nbt -> nbt.putInt("RedstoneStrength", 0));
      scene.idle(10);

      for (int i = 0; i < 8; i++) {
         double angle = Math.PI * ((double)i / 4.0 - 0.125);
         Vec3 relPos = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)).scale(4.87035).add(1.5, 1.0, 3.5);
         Vec3 relPos2 = new Vec3(Math.cos(angle + (Math.PI / 4)), 0.0, Math.sin(angle + (Math.PI / 4))).scale(4.87035).add(1.5, 1.0, 3.5);
         overlay.showLine(PonderPalette.OUTPUT, relPos, relPos2, 270 - i);
         scene.idle(1);
      }

      scene.idle(10);
      world.toggleRedstonePower(link4);
      effects.indicateRedstone(new BlockPos(6, 2, 6));
      scene.idle(10);
      Vec3 link4Vec = vector.blockSurface(new BlockPos(6, 1, 5), Direction.SOUTH).add(0.0, 0.0, -0.1875);
      scene.idle(15);
      overlay.showText(50).attachKeyFrame().text("Redstone Links outside the maximum range will not be received at all").pointAt(link4Vec).placeNearTarget();
      scene.idle(60);
      world.toggleRedstonePower(link4);
      scene.idle(10);

      for (int i = 0; i < 8; i++) {
         double angle = Math.PI * ((double)i / 4.0 - 0.125);
         Vec3 relPos = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)).scale(2.70575).add(1.5, 1.0, 3.5);
         Vec3 relPos2 = new Vec3(Math.cos(angle + (Math.PI / 4)), 0.0, Math.sin(angle + (Math.PI / 4))).scale(2.70575).add(1.5, 1.0, 3.5);
         overlay.showLine(PonderPalette.OUTPUT, relPos, relPos2, 160 - i);
         scene.idle(1);
      }

      scene.idle(10);
      world.toggleRedstonePower(link2);
      effects.indicateRedstone(new BlockPos(4, 1, 3));
      scene.idle(5);
      world.toggleRedstonePower(select.position(modulatingLink));
      effects.indicateRedstone(modulatingLink);
      world.modifyBlock(mainDust, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, 4), false);
      world.modifyBlockEntityNBT(select.position(nixieTube), NixieTubeBlockEntity.class, nbt -> nbt.putInt("RedstoneStrength", 4));
      scene.idle(25);
      world.toggleRedstonePower(link3);
      effects.indicateRedstone(new BlockPos(3, 2, 6));
      scene.idle(5);
      effects.indicateRedstone(modulatingLink);
      world.modifyBlock(mainDust, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, 11), false);
      world.modifyBlockEntityNBT(select.position(nixieTube), NixieTubeBlockEntity.class, nbt -> nbt.putInt("RedstoneStrength", 11));
      Vec3 link3Vec = vector.blockSurface(new BlockPos(3, 1, 5), Direction.SOUTH).add(0.0, 0.0, -0.1875);
      scene.idle(15);
      overlay.showText(60)
         .attachKeyFrame()
         .text("Redstone Links in-between the two ranges will be received with proportional signal strengths")
         .pointAt(link3Vec)
         .placeNearTarget();
      scene.idle(70);
      world.toggleRedstonePower(link2);
      scene.idle(5);
      world.toggleRedstonePower(link3);
      scene.idle(5);
      world.toggleRedstonePower(select.position(modulatingLink));
      world.modifyBlock(mainDust, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, 0), false);
      world.modifyBlockEntityNBT(select.position(nixieTube), NixieTubeBlockEntity.class, nbt -> nbt.putInt("RedstoneStrength", 0));
      scene.idle(20);
      Vec3 frontSlot = top.add(0.1875, -0.3125, -0.3125);
      Vec3 backSlot = top.add(0.1875, -0.3125, 0.3125);
      Vec3 front1Slot = link1Vec.add(0.18, -0.05, -0.15);
      Vec3 back1Slot = link1Vec.add(0.18, -0.05, 0.15);
      Vec3 top3Slot = link3Vec.add(-0.09, 0.15, 0.0);
      Vec3 bottom3Slot = link3Vec.add(-0.09, -0.2, 0.0);
      scene.addKeyframe();
      scene.idle(10);
      overlay.showFilterSlotInput(frontSlot, Direction.UP, 100);
      overlay.showFilterSlotInput(backSlot, Direction.UP, 100);
      scene.idle(10);
      ItemStack iron = new ItemStack(Items.IRON_INGOT);
      ItemStack gold = new ItemStack(Items.GOLD_INGOT);
      ItemStack sapling = new ItemStack(Items.OAK_SAPLING);
      overlay.showControls(frontSlot, Pointing.UP, 40).withItem(iron);
      scene.idle(7);
      overlay.showControls(backSlot, Pointing.DOWN, 40).withItem(sapling);
      setNBTValue(scene, modulatingLink, "FrequencyLast", iron);
      scene.idle(7);
      setNBTValue(scene, modulatingLink, "FrequencyFirst", sapling);
      scene.idle(20);
      overlay.showControls(front1Slot, Pointing.UP, 40).withItem(gold);
      scene.idle(7);
      overlay.showControls(back1Slot, Pointing.DOWN, 40).withItem(sapling);
      setNBTValue(scene, new BlockPos(1, 1, 1), "FrequencyLast", gold);
      scene.idle(7);
      setNBTValue(scene, new BlockPos(1, 1, 1), "FrequencyFirst", sapling);
      scene.idle(20);
      overlay.showControls(bottom3Slot, Pointing.UP, 40).withItem(iron);
      scene.idle(7);
      overlay.showControls(top3Slot, Pointing.DOWN, 40).withItem(sapling);
      setNBTValue(scene, new BlockPos(3, 1, 5), "FrequencyLast", iron);
      scene.idle(7);
      setNBTValue(scene, new BlockPos(3, 1, 5), "FrequencyFirst", sapling);
      scene.idle(50);
      world.toggleRedstonePower(link1);
      effects.indicateRedstone(new BlockPos(3, 1, 1));
      scene.idle(25);
      world.toggleRedstonePower(link3);
      effects.indicateRedstone(new BlockPos(3, 2, 6));
      scene.idle(5);
      world.toggleRedstonePower(select.position(modulatingLink));
      effects.indicateRedstone(modulatingLink);
      world.modifyBlock(mainDust, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, 11), false);
      world.modifyBlockEntityNBT(select.position(nixieTube), NixieTubeBlockEntity.class, nbt -> nbt.putInt("RedstoneStrength", 11));
      scene.idle(15);
      overlay.showText(50)
         .attachKeyFrame()
         .text("Modulating Linked Receivers respect the same item frequencies that Redstone Links use")
         .pointAt(top)
         .placeNearTarget();
      scene.idle(50);
   }

   private static void setNBTValue(CreateSceneBuilder scene, BlockPos modulatingLink, String FrequencyLast, ItemStack iron) {
      scene.addInstruction(subScene -> {
         PonderLevel level = subScene.getWorld();
         BlockEntity blockEntity = level.getBlockEntity(modulatingLink);
         RegistryAccess registryAccess = level.registryAccess();
         CompoundTag tag = blockEntity.saveWithFullMetadata(registryAccess);
         tag.put(FrequencyLast, iron.save(registryAccess, new CompoundTag()));
         blockEntity.loadWithComponents(tag, registryAccess);
      });
   }

   public static void directionalReceiver(SceneBuilder builder, SceneBuildingUtil util) {
      CreateSceneBuilder scene = new CreateSceneBuilder(builder);
      WorldInstructions world = scene.world();
      SelectionUtil select = util.select();
      OverlayInstructions overlay = scene.overlay();
      EffectInstructions effects = scene.effects();
      VectorUtil vector = util.vector();
      PositionUtil grid = util.grid();
      scene.title("directional_receiver", "Using Directional Linked Receivers");
      scene.configureBasePlate(0, 0, 7);
      world.showSection(select.layer(0), Direction.UP);
      scene.rotateCameraY(-25.0F);
      BlockPos directionalLink = new BlockPos(4, 1, 3);
      BlockPos nixieTube = new BlockPos(5, 2, 3);
      Selection link1 = select.fromTo(0, 1, 3, 1, 1, 3);
      Selection link2 = select.fromTo(1, 1, 5, 2, 1, 5);
      Selection link3 = select.fromTo(2, 1, 1, 3, 1, 1);
      Selection link4 = select.fromTo(4, 1, 0, 6, 1, 0);
      Vec3 linkVec = vector.blockSurface(directionalLink, Direction.DOWN).add(0.0, 0.1875, 0.0);
      Vec3 link1Vec = vector.blockSurface(new BlockPos(1, 1, 3), Direction.DOWN).add(0.0, 0.1875, 0.0);
      Vec3 link2Vec = vector.blockSurface(new BlockPos(2, 1, 5), Direction.DOWN).add(0.0, 0.1875, 0.0);
      Vec3 link3Vec = vector.blockSurface(new BlockPos(3, 1, 1), Direction.DOWN).add(0.0, 0.1875, 0.0);
      world.showSection(link1, Direction.UP);
      world.showSection(link2, Direction.UP);
      world.showSection(link3, Direction.UP);
      scene.idle(10);
      world.showSection(select.fromTo(5, 1, 2, 5, 1, 4), Direction.DOWN);
      scene.idle(5);
      world.showSection(select.position(nixieTube), Direction.DOWN);
      scene.idle(5);
      world.showSection(select.position(directionalLink), Direction.EAST);
      scene.idle(20);
      Vec3 top = new Vec3(4.5, 1.5, 3.5);
      overlay.showText(50)
         .attachKeyFrame()
         .text("Directional Linked Receivers can detect the angle to other nearby Redstone Links")
         .placeNearTarget()
         .pointAt(top);
      scene.idle(60);
      world.toggleRedstonePower(link1);
      effects.indicateRedstone(new BlockPos(0, 1, 3));
      scene.idle(5);
      world.toggleRedstonePower(select.position(directionalLink));
      effects.indicateRedstone(directionalLink);
      world.modifyBlockEntityNBT(select.position(nixieTube), NixieTubeBlockEntity.class, nbt -> nbt.putInt("RedstoneStrength", 15));
      scene.idle(10);
      scene.addInstruction(
         new ChasingLineInstruction(
            link1Vec.subtract(0.0, 0.1, 0.0), linkVec.subtract(0.0, 0.1, 0.0), 1, PonderPalette.OUTPUT.getColor(), 10, 95, SmoothMovementUtils.quadraticRise()
         )
      );
      scene.idle(20);
      overlay.showText(50)
         .attachKeyFrame()
         .text("Redstone Links directly infront will be received at their full signal strength")
         .pointAt(link1Vec)
         .placeNearTarget();
      scene.idle(70);
      world.toggleRedstonePower(link1);
      scene.idle(5);
      world.toggleRedstonePower(select.position(directionalLink));
      world.modifyBlockEntityNBT(select.position(nixieTube), NixieTubeBlockEntity.class, nbt -> nbt.putInt("RedstoneStrength", 0));
      scene.idle(20);
      world.toggleRedstonePower(link2);
      effects.indicateRedstone(new BlockPos(1, 1, 5));
      scene.idle(5);
      world.toggleRedstonePower(select.position(directionalLink));
      world.modifyBlockEntityNBT(select.position(nixieTube), NixieTubeBlockEntity.class, nbt -> nbt.putInt("RedstoneStrength", 7));
      effects.indicateRedstone(directionalLink);
      scene.idle(10);
      scene.addInstruction(
         new ChasingLineInstruction(
            link2Vec.subtract(0.0, 0.1, 0.0), linkVec.subtract(0.0, 0.1, 0.0), 1, PonderPalette.OUTPUT.getColor(), 10, 25, SmoothMovementUtils.quadraticRise()
         )
      );
      scene.idle(30);
      world.toggleRedstonePower(link2);
      scene.idle(5);
      world.toggleRedstonePower(select.position(directionalLink));
      world.modifyBlockEntityNBT(select.position(nixieTube), NixieTubeBlockEntity.class, nbt -> nbt.putInt("RedstoneStrength", 0));
      scene.idle(20);
      world.toggleRedstonePower(link3);
      effects.indicateRedstone(new BlockPos(2, 1, 1));
      scene.idle(5);
      world.toggleRedstonePower(select.position(directionalLink));
      world.modifyBlockEntityNBT(select.position(nixieTube), NixieTubeBlockEntity.class, nbt -> nbt.putInt("RedstoneStrength", 4));
      effects.indicateRedstone(directionalLink);
      scene.idle(10);
      scene.addInstruction(
         new ChasingLineInstruction(
            link3Vec.subtract(0.0, 0.1, 0.0), linkVec.subtract(0.0, 0.1, 0.0), 1, PonderPalette.OUTPUT.getColor(), 10, 80, SmoothMovementUtils.quadraticRise()
         )
      );
      scene.idle(30);
      overlay.showText(50).attachKeyFrame().text("A shallower angle results in a lower received signal strength").pointAt(link3Vec).placeNearTarget();
      scene.idle(70);
      world.toggleRedstonePower(link3);
      scene.idle(5);
      world.toggleRedstonePower(select.position(directionalLink));
      world.modifyBlockEntityNBT(select.position(nixieTube), NixieTubeBlockEntity.class, nbt -> nbt.putInt("RedstoneStrength", 0));
      scene.idle(20);
      world.showSection(link4, Direction.DOWN);
      scene.idle(20);
      world.toggleRedstonePower(link4);
      effects.indicateRedstone(new BlockPos(4, 1, 0));
      scene.idle(20);
      AABB bb = new AABB(5.0, 1.0, 3.0, 5.0, 2.0, 4.0);
      overlay.chaseBoundingBoxOutline(PonderPalette.RED, bb, bb, 10);
      scene.idle(10);
      overlay.chaseBoundingBoxOutline(PonderPalette.RED, bb, bb.inflate(0.0, 3.5, 3.5), 10);
      scene.idle(10);
      overlay.chaseBoundingBoxOutline(PonderPalette.RED, bb, bb.inflate(0.0, 3.5, 3.5).expandTowards(10.0, 0.0, 0.0), 70);
      overlay.showText(60)
         .attachKeyFrame()
         .colored(PonderPalette.RED)
         .text("Directional Linked Receivers cannot receive signals from Redstone Links behind them")
         .pointAt(new Vec3(5.0, 1.0, 7.5))
         .placeNearTarget();
      scene.idle(80);
      world.hideSection(link4, Direction.UP);
      scene.idle(20);
      Vec3 frontSlot = top.add(0.3125, -0.3125, -0.1875);
      Vec3 backSlot = top.add(0.3125, 0.3125, -0.1875);
      Vec3 front1Slot = link1Vec.add(0.18, -0.05, -0.15);
      Vec3 back1Slot = link1Vec.add(0.18, -0.05, 0.15);
      Vec3 top3Slot = link3Vec.add(0.18, -0.05, 0.15);
      Vec3 bottom3Slot = link3Vec.add(0.18, -0.05, -0.15);
      scene.addKeyframe();
      scene.idle(10);
      overlay.showFilterSlotInput(frontSlot, Direction.WEST, 100);
      overlay.showFilterSlotInput(backSlot, Direction.WEST, 100);
      scene.idle(10);
      ItemStack iron = new ItemStack(Items.IRON_INGOT);
      ItemStack gold = new ItemStack(Items.GOLD_INGOT);
      ItemStack sapling = new ItemStack(Items.OAK_SAPLING);
      overlay.showControls(frontSlot, Pointing.UP, 40).withItem(iron);
      scene.idle(7);
      overlay.showControls(backSlot, Pointing.DOWN, 40).withItem(sapling);
      setNBTValue(scene, directionalLink, "FrequencyLast", iron);
      scene.idle(7);
      setNBTValue(scene, directionalLink, "FrequencyFirst", sapling);
      scene.idle(20);
      overlay.showControls(bottom3Slot, Pointing.UP, 40).withItem(iron);
      scene.idle(7);
      overlay.showControls(top3Slot, Pointing.DOWN, 40).withItem(sapling);
      setNBTValue(scene, new BlockPos(3, 1, 1), "FrequencyLast", iron);
      scene.idle(7);
      setNBTValue(scene, new BlockPos(3, 1, 1), "FrequencyFirst", sapling);
      scene.idle(20);
      overlay.showControls(front1Slot, Pointing.UP, 40).withItem(gold);
      scene.idle(7);
      overlay.showControls(back1Slot, Pointing.DOWN, 40).withItem(sapling);
      setNBTValue(scene, new BlockPos(1, 1, 3), "FrequencyLast", gold);
      scene.idle(7);
      setNBTValue(scene, new BlockPos(1, 1, 3), "FrequencyFirst", sapling);
      scene.idle(50);
      world.toggleRedstonePower(link1);
      effects.indicateRedstone(new BlockPos(0, 1, 3));
      scene.idle(25);
      world.toggleRedstonePower(link3);
      effects.indicateRedstone(new BlockPos(2, 1, 1));
      scene.idle(5);
      world.toggleRedstonePower(select.position(directionalLink));
      effects.indicateRedstone(directionalLink);
      world.modifyBlockEntityNBT(select.position(nixieTube), NixieTubeBlockEntity.class, nbt -> nbt.putInt("RedstoneStrength", 4));
      scene.idle(15);
      overlay.showText(50)
         .attachKeyFrame()
         .text("Directional Linked Receivers respect the same item frequencies that Redstone Links use")
         .pointAt(top)
         .placeNearTarget();
      scene.idle(50);
   }

   public static void redstoneAccumulator(SceneBuilder builder, SceneBuildingUtil util) {
      CreateSceneBuilder scene = new CreateSceneBuilder(builder);
      WorldInstructions world = scene.world();
      SelectionUtil select = util.select();
      OverlayInstructions overlay = scene.overlay();
      EffectInstructions effects = scene.effects();
      VectorUtil vector = util.vector();
      PositionUtil grid = util.grid();
      scene.title("redstone_accumulator", "Controlling signals using Redstone Accumulators");
      scene.configureBasePlate(0, 0, 5);
      scene.showBasePlate();
      Selection addInput = select.fromTo(2, 1, 0, 2, 1, 1);
      Selection subInput = select.fromTo(0, 1, 2, 1, 1, 2);
      BlockPos outputDustPos = grid.at(2, 1, 3);
      BlockPos tubePos = grid.at(2, 1, 4);
      Selection tube = select.position(tubePos);
      Selection output = select.fromTo(outputDustPos, tubePos);
      world.showSection(addInput.add(subInput).add(output), Direction.UP);
      scene.idle(5);
      BlockPos accumulatorPos = grid.at(2, 1, 2);
      Selection accumulator = select.position(accumulatorPos);
      world.showSection(accumulator, Direction.DOWN);
      scene.idle(20);
      overlay.showText(100)
         .text("Redstone Accumulators store an analog signal that can be modified over time")
         .placeNearTarget()
         .pointAt(vector.centerOf(accumulatorPos).add(0.0, -0.125, 0.0));
      scene.idle(120);
      AABB bb = new AABB(accumulatorPos).inflate(-0.05F, -0.45F, -0.48F).move(0.0, -0.45, -0.575);
      overlay.chaseBoundingBoxOutline(PonderPalette.GREEN, bb, bb, 120);
      overlay.showText(120)
         .attachKeyFrame()
         .placeNearTarget()
         .pointAt(vector.blockSurface(accumulatorPos, Direction.NORTH).add(0.0, -0.5, -0.05))
         .text("Signals at the back increase the output strength")
         .colored(PonderPalette.GREEN);
      world.toggleRedstonePower(addInput);
      effects.indicateRedstone(grid.at(2, 1, 0));

      for (int i = 1; i <= 11; i++) {
         scene.idle(10);
         world.modifyBlockEntityNBT(accumulator, RedstoneAccumulatorBlockEntity.class, tag -> tag.putInt("OutputSignal", i));
         world.modifyBlock(outputDustPos, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, i), false);
         world.modifyBlockEntityNBT(tube, NixieTubeBlockEntity.class, tag -> tag.putInt("RedstoneStrength", i));
      }

      world.toggleRedstonePower(addInput);
      effects.indicateRedstone(grid.at(2, 1, 0));
      scene.idle(30);
      bb = new AABB(accumulatorPos).inflate(-0.48F, -0.45F, -0.05F).move(0.575, -0.45, 0.0);
      AABB bb2 = new AABB(accumulatorPos).inflate(-0.48F, -0.45F, -0.05F).move(-0.575, -0.45, 0.0);
      overlay.chaseBoundingBoxOutline(PonderPalette.RED, bb, bb, 90);
      overlay.chaseBoundingBoxOutline(PonderPalette.RED, bb2, bb2, 90);
      overlay.showText(90)
         .attachKeyFrame()
         .text("Signals from the sides decrease the output strength")
         .placeNearTarget()
         .pointAt(vector.blockSurface(accumulatorPos, Direction.WEST).add(-0.05, -0.5, 0.0))
         .colored(PonderPalette.RED);
      scene.idle(20);
      world.toggleRedstonePower(subInput);
      effects.indicateRedstone(grid.at(0, 1, 2));

      for (int i = 1; i <= 7; i++) {
         int fi = 11 - i;
         world.modifyBlockEntityNBT(accumulator, RedstoneAccumulatorBlockEntity.class, tag -> tag.putInt("OutputSignal", fi));
         world.modifyBlock(outputDustPos, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, fi), false);
         world.modifyBlockEntityNBT(tube, NixieTubeBlockEntity.class, tag -> tag.putInt("RedstoneStrength", fi));
         scene.idle(10);
      }

      world.toggleRedstonePower(subInput);
      effects.indicateRedstone(grid.at(0, 1, 2));
      scene.idle(30);
      Vec3 circuitTop = vector.blockSurface(accumulatorPos, Direction.DOWN).add(0.0, 0.4375, 0.0);
      overlay.showFilterSlotInput(circuitTop, Direction.UP, 80);
      overlay.showControls(circuitTop, Pointing.DOWN, 70).rightClick();
      scene.idle(10);
      overlay.showText(60).attachKeyFrame().text("Using the value panel, the rate of change can be configured").placeNearTarget().pointAt(circuitTop);
      world.modifyBlockEntityNBT(accumulator, RedstoneAccumulatorBlockEntity.class, tag -> tag.putInt("DelayTicks", 120));
      scene.idle(80);
      world.toggleRedstonePower(addInput);
      effects.indicateRedstone(grid.at(2, 1, 0));

      for (int i = 4; i <= 15; i++) {
         int fi = i;
         world.modifyBlockEntityNBT(accumulator, RedstoneAccumulatorBlockEntity.class, tag -> tag.putInt("OutputSignal", fi));
         world.modifyBlock(outputDustPos, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, fi), false);
         world.modifyBlockEntityNBT(tube, NixieTubeBlockEntity.class, tag -> tag.putInt("RedstoneStrength", fi));
         scene.idle(20);
         if (i == 6) {
            overlay.showText(60).text("Configured delays can range up to an hour").placeNearTarget().pointAt(circuitTop);
         }

         if (i == 8) {
            scene.markAsFinished();
         }
      }

      world.toggleRedstonePower(addInput);
      effects.indicateRedstone(grid.at(2, 1, 0));
   }

   public static void redstoneInductor(SceneBuilder builder, SceneBuildingUtil util) {
      CreateSceneBuilder scene = new CreateSceneBuilder(builder);
      WorldInstructions world = scene.world();
      SelectionUtil select = util.select();
      OverlayInstructions overlay = scene.overlay();
      EffectInstructions effects = scene.effects();
      VectorUtil vector = util.vector();
      PositionUtil grid = util.grid();
      scene.title("redstone_inductor", "Controlling signals using Redstone Inductor");
      scene.configureBasePlate(0, 0, 5);
      scene.showBasePlate();
      BlockPos lever = grid.at(2, 1, 0);
      BlockPos inputTubePos = grid.at(3, 1, 1);
      Selection inputTube = select.position(inputTubePos);
      BlockPos inputWirePos = grid.at(2, 1, 1);
      BlockPos outputTubePos = grid.at(2, 1, 4);
      Selection outputTube = select.position(outputTubePos);
      BlockPos outputWirePos = grid.at(2, 1, 3);
      world.showSection(select.fromTo(lever, inputTubePos), Direction.UP);
      world.showSection(select.fromTo(outputWirePos, outputTubePos), Direction.UP);
      scene.idle(5);
      BlockPos inductorPos = grid.at(2, 1, 2);
      Selection inductor = select.position(inductorPos);
      world.showSection(inductor, Direction.DOWN);
      scene.idle(20);
      Vec3 circuitTop = vector.blockSurface(inductorPos, Direction.DOWN).add(0.0, 0.4375, 0.0);
      Vec3 outputSide = vector.blockSurface(outputTubePos, Direction.WEST);
      overlay.showText(140).text("Redstone Inductors output an analog signal that changes over time to match its input").placeNearTarget().pointAt(circuitTop);
      scene.idle(160);

      for (int i = 1; i <= 7; i++) {
         world.modifyBlockEntityNBT(select.position(lever), AnalogLeverBlockEntity.class, tag -> tag.putInt("State", i));
         world.modifyBlock(inputWirePos, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, i), false);
         world.modifyBlockEntityNBT(inputTube, NixieTubeBlockEntity.class, tag -> tag.putInt("RedstoneStrength", i));
         scene.idle(2);
      }

      world.toggleRedstonePower(inductor);
      scene.idle(10);

      for (int i = 1; i <= 7; i++) {
         world.modifyBlockEntityNBT(inductor, RedstoneInductorBlockEntity.class, tag -> tag.putInt("OutputSignal", i));
         world.modifyBlock(outputWirePos, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, i), false);
         world.modifyBlockEntityNBT(outputTube, NixieTubeBlockEntity.class, tag -> tag.putInt("RedstoneStrength", i));
         scene.idle(10);
      }

      scene.idle(30);

      for (int i = 1; i <= 7; i++) {
         int fi = 7 - i;
         world.modifyBlockEntityNBT(select.position(lever), AnalogLeverBlockEntity.class, tag -> tag.putInt("State", fi));
         world.modifyBlock(inputWirePos, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, fi), false);
         world.modifyBlockEntityNBT(inputTube, NixieTubeBlockEntity.class, tag -> tag.putInt("RedstoneStrength", fi));
         scene.idle(2);
      }

      world.toggleRedstonePower(inductor);
      scene.idle(10);

      for (int i = 1; i <= 7; i++) {
         int fi = 7 - i;
         world.modifyBlockEntityNBT(inductor, RedstoneInductorBlockEntity.class, tag -> tag.putInt("OutputSignal", fi));
         world.modifyBlock(outputWirePos, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, fi), false);
         world.modifyBlockEntityNBT(outputTube, NixieTubeBlockEntity.class, tag -> tag.putInt("RedstoneStrength", fi));
         scene.idle(10);
      }

      scene.idle(20);
      overlay.showFilterSlotInput(circuitTop, Direction.UP, 60);
      overlay.showControls(circuitTop, Pointing.DOWN, 60).rightClick();
      scene.idle(10);
      overlay.showText(60).attachKeyFrame().text("Using the value panel, the rate of change can be configured").placeNearTarget().pointAt(circuitTop);
      world.modifyBlockEntityNBT(inductor, RedstoneAccumulatorBlockEntity.class, tag -> tag.putInt("DelayTicks", 120));
      scene.idle(60);

      for (int i = 1; i <= 15; i++) {
         int fi = i;
         world.modifyBlockEntityNBT(select.position(lever), AnalogLeverBlockEntity.class, tag -> tag.putInt("State", fi));
         world.modifyBlock(inputWirePos, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, fi), false);
         world.modifyBlockEntityNBT(inputTube, NixieTubeBlockEntity.class, tag -> tag.putInt("RedstoneStrength", fi));
         scene.idle(2);
      }

      world.toggleRedstonePower(inductor);
      effects.indicateRedstone(grid.at(2, 1, 0));

      for (int i = 1; i <= 15; i++) {
         int fi = i;
         world.modifyBlockEntityNBT(inductor, RedstoneAccumulatorBlockEntity.class, tag -> tag.putInt("OutputSignal", fi));
         world.modifyBlock(outputWirePos, s -> (BlockState)s.setValue(RedStoneWireBlock.POWER, fi), false);
         world.modifyBlockEntityNBT(outputTube, NixieTubeBlockEntity.class, tag -> tag.putInt("RedstoneStrength", fi));
         if (i == 2) {
            overlay.showText(60).text("Configured delays can range up to an hour").placeNearTarget().pointAt(circuitTop);
         }

         if (i == 4) {
            scene.markAsFinished();
         }

         scene.idle(30);
      }
   }

   public static void throttleLever(SceneBuilder builder, SceneBuildingUtil util) {
      CreateSceneBuilder scene = new CreateSceneBuilder(builder);
      WorldInstructions world = scene.world();
      SelectionUtil select = util.select();
      OverlayInstructions overlay = scene.overlay();
      EffectInstructions effects = scene.effects();
      VectorUtil vector = util.vector();
      PositionUtil grid = util.grid();
      scene.title("throttle_lever", "Controlling signals using the Throttle Lever");
      scene.configureBasePlate(0, 0, 5);
      scene.showBasePlate();
      BlockPos[] wireLocations = new BlockPos[]{
         grid.at(2, 1, 1),
         grid.at(2, 1, 0),
         grid.at(3, 1, 0),
         grid.at(4, 1, 0),
         grid.at(4, 1, 1),
         grid.at(4, 1, 2),
         grid.at(4, 1, 3),
         grid.at(4, 1, 4),
         grid.at(3, 1, 4),
         grid.at(2, 1, 4),
         grid.at(1, 1, 4),
         grid.at(0, 1, 4),
         grid.at(0, 1, 3),
         grid.at(0, 1, 2),
         grid.at(0, 1, 1)
      };
      Selection leverSelection = select.fromTo(2, 1, 2, 2, 2, 2);
      Selection lamp = select.position(0, 1, 0);
      BlockPos leverPos = grid.at(2, 2, 2);
      Vec3 leverVec = vector.centerOf(leverPos).add(0.0, -0.25, 0.0);
      world.showSection(select.layersFrom(0).substract(lamp).substract(leverSelection), Direction.UP);
      scene.idle(5);
      world.showSection(lamp, Direction.DOWN);
      scene.idle(10);
      world.showSection(leverSelection, Direction.DOWN);
      scene.idle(20);
      overlay.showText(60).text("Throttle Levers make for a compact and precise source of redstone power").placeNearTarget().attachKeyFrame().pointAt(leverVec);
      scene.idle(70);
      IntegerProperty power = RedStoneWireBlock.POWER;
      overlay.showControls(vector.centerOf(leverPos), Pointing.DOWN, 50).rightClick();
      scene.idle(7);

      for (int i = 0; i < 13; i++) {
         scene.idle(2);
         int state = i + 1;
         world.modifyBlockEntityNBT(leverSelection, ThrottleLeverBlockEntity.class, nbt -> nbt.putInt("State", state));
         world.modifyBlock(wireLocations[i], s -> (BlockState)s.setValue(power, 14 - state), false);
         effects.indicateRedstone(wireLocations[i]);
      }

      scene.idle(5);

      for (int i = 13; i > 0; i--) {
         scene.idle(2);
         int state = i - 1;
         if (i > 10) {
            world.modifyBlockEntityNBT(leverSelection, ThrottleLeverBlockEntity.class, nbt -> nbt.putInt("State", state));
            effects.indicateRedstone(wireLocations[i - 1]);
         }

         world.modifyBlock(wireLocations[i], s -> (BlockState)s.setValue(power, state > 10 ? 0 : 10 - state), false);
      }

      world.modifyBlock(wireLocations[0], s -> (BlockState)s.setValue(power, 10), false);
      scene.idle(20);
      overlay.showText(60).attachKeyFrame().text("Right-click and drag to set its analog power output").placeNearTarget().pointAt(leverVec);
      scene.idle(70);
      overlay.showControls(vector.centerOf(leverPos), Pointing.DOWN, 40).rightClick().withItem(AllItems.WRENCH.asStack());
      scene.idle(6);

      for (int i = 0; i < 10; i++) {
         int state = i + 1;
         world.modifyBlock(wireLocations[i], s -> (BlockState)s.setValue(power, state > 6 ? 0 : 6 - state), false);
         if (state > 4) {
            effects.indicateRedstone(wireLocations[i + 1]);
         }
      }

      world.modifyBlock(leverPos, s -> (BlockState)s.setValue(ThrottleLeverBlock.INVERTED, true), false);
      scene.idle(1);
      world.modifyBlockEntityNBT(leverSelection, ThrottleLeverBlockEntity.class, nbt -> nbt.putInt("State", 5));
      effects.indicateRedstone(leverPos);
      scene.idle(30);
      overlay.showText(60).attachKeyFrame().text("Using a Wrench, the signal can be inverted").placeNearTarget().pointAt(leverVec);
      scene.idle(80);
      overlay.showControls(vector.centerOf(leverPos), Pointing.DOWN, 40).rightClick();
      scene.idle(7);

      for (int ix = 0; ix < 15; ix++) {
         scene.idle(2);
         int state = ix + 1;
         effects.indicateRedstone(wireLocations[ix]);
         world.modifyBlockEntityNBT(leverSelection, ThrottleLeverBlockEntity.class, nbt -> nbt.putInt("State", Math.min(state + 5, 15)));
         world.modifyBlock(wireLocations[ix], s -> (BlockState)s.setValue(power, 15 - state), false);
      }

      world.toggleRedstonePower(lamp);
      effects.indicateRedstone(leverPos);
      effects.indicateRedstone(grid.at(0, 1, 0));
      scene.idle(20);
   }

   public static void redstoneMagnet(SceneBuilder scene, SceneBuildingUtil util) {
      scene.title("redstone_magnet", "Using Redstone Magnets");
      scene.configureBasePlate(0, 0, 9);
      scene.world().showSection(util.select().layer(0), Direction.UP);
      scene.scaleSceneView(0.8F);
      scene.idle(10);
      BlockPos magnet1Pos = new BlockPos(7, 3, 4);
      BlockPos lever1Pos = new BlockPos(7, 2, 3);
      BlockPos magnet2Pos = new BlockPos(4, 3, 4);
      BlockPos lever2Pos = new BlockPos(3, 3, 4);
      scene.addInstruction(new PullTheAssemblerKronkInstruction(new BlockPos(1, 3, 4), true, true));
      scene.world().showSection(util.select().fromTo(7, 1, 3, 8, 2, 5), Direction.DOWN);
      scene.idle(5);
      scene.world().showSection(util.select().position(magnet1Pos), Direction.DOWN);
      scene.idle(7);
      ElementLink<WorldSectionElement> cart = scene.world().showIndependentSection(util.select().fromTo(1, 1, 3, 4, 2, 5), Direction.DOWN);
      scene.idle(5);

      for (int i = 4; i >= 1; i--) {
         scene.world().showSectionAndMerge(util.select().position(i, 3, 4), Direction.DOWN, cart);
         scene.idle(3);
      }

      scene.world().showSectionAndMerge(util.select().fromTo(1, 3, 3, 3, 3, 3), Direction.DOWN, cart);
      scene.world().showSectionAndMerge(util.select().fromTo(1, 3, 5, 3, 3, 5), Direction.DOWN, cart);
      scene.idle(5);
      scene.overlay()
         .showText(60)
         .attachKeyFrame()
         .text("Redstone Magnets can attract or repel other Redstone Magnets")
         .pointAt(util.vector().topOf(magnet2Pos))
         .placeNearTarget();
      scene.idle(70);
      scene.effects().indicateRedstone(lever1Pos);
      scene.world().toggleRedstonePower(util.select().fromTo(magnet1Pos, lever1Pos));
      scene.idle(10);
      scene.effects().indicateRedstone(lever2Pos);
      scene.world().toggleRedstonePower(util.select().fromTo(magnet2Pos, lever2Pos));
      scene.idle(10);
      Vec3 m1 = Vec3.atCenterOf(magnet1Pos).add(-0.5, 0.0, 0.0);
      Vec3 m2 = Vec3.atCenterOf(magnet2Pos).add(0.5, 0.0, 0.0);
      AABB bb1 = new AABB(m1, m1);
      scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, bb1, bb1, 1);
      scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, bb1, bb1.expandTowards(-0.96, 0.0, 0.0), 20);
      AABB bb2 = new AABB(m2, m2);
      scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb2, bb2, 1);
      scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb2, bb2.expandTowards(0.96, 0.0, 0.0), 20);
      scene.idle(10);
      scene.addInstruction(CustomAnimateWorldSectionInstruction.move(cart, new Vec3(2.0, 0.0, 0.0), 20, SmoothMovementUtils.cubicRise()));
      scene.idle(25);
      scene.overlay()
         .showText(50)
         .attachKeyFrame()
         .text("Opposite poles attract each other")
         .pointAt(util.vector().topOf(magnet2Pos.offset(2, 0, 0)))
         .placeNearTarget();
      scene.idle(40);
      scene.world().toggleRedstonePower(util.select().fromTo(magnet1Pos, lever1Pos));
      scene.world().toggleRedstonePower(util.select().fromTo(magnet2Pos, lever2Pos));
      scene.world().moveSection(cart, new Vec3(-2.0, 0.0, 0.0), 15);
      scene.idle(20);
      ElementLink<WorldSectionElement> magnetSection = scene.world().makeSectionIndependent(util.select().position(magnet1Pos));
      scene.addInstruction(CustomAnimateWorldSectionInstruction.move(magnetSection, new Vec3(0.0, 0.5, 0.0), 20, SmoothMovementUtils.quadraticJump()));
      scene.addInstruction(CustomAnimateWorldSectionInstruction.rotate(magnetSection, new Vec3(0.0, 0.0, 180.0), 20, SmoothMovementUtils.cubicSmoothing()));
      scene.idle(25);
      scene.effects().indicateRedstone(lever1Pos);
      scene.world().toggleRedstonePower(util.select().fromTo(magnet1Pos, lever1Pos));
      scene.idle(10);
      scene.effects().indicateRedstone(lever2Pos);
      scene.world().toggleRedstonePower(util.select().fromTo(magnet2Pos, lever2Pos));
      scene.idle(10);
      m1 = Vec3.atCenterOf(magnet1Pos).add(Vec3.atCenterOf(magnet2Pos)).scale(0.5);
      m1 = m1.add(0.03, 0.0, 0.0);
      m2 = m1.add(-0.03, 0.0, 0.0);
      bb1 = new AABB(m1, m1);
      scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb1, bb1, 1);
      scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb1, bb1.expandTowards(0.96, 0.0, 0.0), 20);
      bb2 = new AABB(m2, m2);
      scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb2, bb2, 1);
      scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb2, bb2.expandTowards(-0.96, 0.0, 0.0), 20);
      scene.idle(10);
      scene.addInstruction(CustomAnimateWorldSectionInstruction.move(cart, new Vec3(-10.0, 0.0, 0.0), 60, SmoothMovementUtils.asymptoticAcceleration(3.0F)));
      scene.idle(18);
      scene.addInstruction(CustomAnimateWorldSectionInstruction.move(cart, new Vec3(0.0, -5.0, 0.0), 40, SmoothMovementUtils.quadraticRise()));
      scene.addInstruction(CustomAnimateWorldSectionInstruction.rotate(cart, new Vec3(0.0, 0.0, 170.0), 40, SmoothMovementUtils.asymptoticAcceleration(2.0F)));
      scene.overlay()
         .showText(50)
         .attachKeyFrame()
         .text("Similar poles repel each other")
         .pointAt(util.vector().topOf(magnet2Pos.offset(-3, 0, 0)))
         .placeNearTarget();
      scene.idle(20);
      scene.world().hideIndependentSection(cart, null);
      scene.idle(30);
      cart = scene.world().showIndependentSection(util.select().fromTo(1, 1, 3, 4, 3, 5), Direction.DOWN);
      scene.world().moveSection(cart, new Vec3(0.5, 0.0, 1.5), 0);
      scene.world().rotateSection(cart, 0.0, 90.0, 0.0, 0);
      scene.world().toggleRedstonePower(util.select().fromTo(magnet1Pos, lever1Pos));
      scene.world().toggleRedstonePower(util.select().fromTo(magnet2Pos, lever2Pos));
      scene.idle(5);
      scene.addInstruction(CustomAnimateWorldSectionInstruction.move(magnetSection, new Vec3(0.0, 0.5, 0.0), 20, SmoothMovementUtils.quadraticJump()));
      scene.addInstruction(CustomAnimateWorldSectionInstruction.rotate(magnetSection, new Vec3(0.0, 0.0, 180.0), 20, SmoothMovementUtils.cubicSmoothing()));
      scene.idle(25);
      scene.effects().indicateRedstone(lever1Pos);
      scene.world().toggleRedstonePower(util.select().fromTo(magnet1Pos, lever1Pos));
      scene.idle(10);
      scene.effects().indicateRedstone(lever2Pos.offset(0, 0, 1));
      scene.world().toggleRedstonePower(util.select().fromTo(magnet2Pos, lever2Pos));
      scene.idle(10);
      m1 = Vec3.atCenterOf(magnet1Pos).add(-0.5, 0.0, 0.0);
      m2 = Vec3.atCenterOf(magnet2Pos).add(-1.0, 0.0, -0.5);
      bb1 = new AABB(m1, m1);
      scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, bb1, bb1, 1);
      scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, bb1, bb1.expandTowards(-0.96, 0.0, 0.0), 20);
      bb2 = new AABB(m2, m2);
      scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb2, bb2, 1);
      scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb2, bb2.expandTowards(0.0, 0.0, -0.96), 20);
      scene.idle(10);
      scene.addInstruction(CustomAnimateWorldSectionInstruction.move(cart, new Vec3(0.0, 0.0, -1.5), 30, SmoothMovementUtils.cubicSmoothing()));
      scene.addInstruction(CustomAnimateWorldSectionInstruction.rotate(cart, new Vec3(0.0, -90.0, 0.0), 30, SmoothMovementUtils.cubicSmoothing()));
      scene.idle(15);
      scene.addInstruction(CustomAnimateWorldSectionInstruction.move(cart, new Vec3(1.5, 0.0, 0.0), 20, SmoothMovementUtils.cubicRise()));
      scene.idle(25);
      scene.overlay()
         .showText(50)
         .attachKeyFrame()
         .text("Attracting magnets attempt to align with each other")
         .pointAt(util.vector().topOf(magnet2Pos.offset(2, 0, 0)))
         .placeNearTarget();
      scene.idle(50);
   }
}
