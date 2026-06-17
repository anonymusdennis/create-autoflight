package dev.eriksonn.aeronautics.content.ponder.scenes;

import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;
import com.simibubi.create.foundation.particle.AirParticleData;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder.WorldInstructions;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonBlockEntity;
import java.util.Objects;
import java.util.Random;
import net.createmod.catnip.math.Pointing;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.EffectInstructions;
import net.createmod.ponder.api.scene.OverlayInstructions;
import net.createmod.ponder.api.scene.PositionUtil;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.api.scene.SelectionUtil;
import net.createmod.ponder.api.scene.VectorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class MountedPotatoCannonScenes {
   public static void mountedPotatoCannonIntro(SceneBuilder builder, SceneBuildingUtil util) {
      CreateSceneBuilder scene = new CreateSceneBuilder(builder);
      WorldInstructions world = scene.world();
      SelectionUtil select = util.select();
      OverlayInstructions overlay = scene.overlay();
      VectorUtil vector = util.vector();
      PositionUtil grid = util.grid();
      scene.title("mounted_potato_cannon", "Using the Mounted Potato Cannon");
      scene.configureBasePlate(0, 0, 5);
      scene.showBasePlate();
      BlockPos cannon = grid.at(2, 2, 2);
      BlockPos lever = grid.at(1, 2, 3);
      BlockPos beltInput = grid.at(0, 1, 3);
      Selection cannonAll = select.fromTo(2, 1, 2, 2, 2, 3);
      Selection itemInput = select.fromTo(0, 1, 2, 1, 2, 5).substract(select.position(lever)).add(select.position(2, 0, 5));
      Selection kinetics = select.fromTo(3, 0, 2, 5, 2, 2).substract(select.fromTo(3, 0, 2, 4, 0, 2));
      ItemStack potato = new ItemStack(Items.BAKED_POTATO);
      Vec3 aimingVec = vector.of(0.0, 0.0, -0.9);
      Vec3 barrelVec = vector.centerOf(cannon).add(aimingVec.scale(1.2));
      Random rnd = new Random();
      world.showSection(cannonAll.substract(select.position(cannon)), Direction.UP);
      world.showSection(select.position(lever), Direction.UP);
      scene.idle(15);
      world.showSection(select.position(cannon), Direction.DOWN);
      scene.idle(20);
      overlay.showText(80).text("The Mounted Potato Cannon is a block version of the Potato Cannon").placeNearTarget().pointAt(vector.centerOf(cannon));
      scene.idle(100);
      overlay.showText(70)
         .text("It accepts the same ammo as the Potato Cannon...")
         .colored(PonderPalette.INPUT)
         .pointAt(vector.centerOf(cannon))
         .placeNearTarget()
         .attachKeyFrame();
      scene.idle(10);
      overlay.showControls(vector.blockSurface(cannon, Direction.UP), Pointing.DOWN, 30).withItem(potato).rightClick();
      scene.idle(70);
      world.showSection(itemInput, Direction.DOWN);
      world.setKineticSpeed(select.position(2, 0, 5), 32.0F);
      world.setKineticSpeed(select.fromTo(1, 1, 5, 0, 1, 2), -32.0F);
      scene.idle(20);
      overlay.showText(80)
         .text("...and can be loaded by automated means")
         .colored(PonderPalette.INPUT)
         .pointAt(vector.centerOf(cannon.west()))
         .placeNearTarget();
      scene.idle(10);

      for (int i = 0; i < 4; i++) {
         world.createItemOnBelt(beltInput, Direction.WEST, potato);
         scene.idle(20);
         world.removeItemsFromBelt(grid.at(1, 1, 3));
         world.flapFunnel(grid.at(1, 2, 3), false);
      }

      scene.idle(20);
      world.hideSection(itemInput, Direction.UP);
      scene.idle(30);
      world.showSection(kinetics, Direction.DOWN);
      scene.idle(6);
      world.setKineticSpeed(select.position(cannon), -16.0F);
      world.setKineticSpeed(select.fromTo(5, 2, 2, 3, 2, 2), -16.0F);
      world.setKineticSpeed(select.fromTo(5, 0, 2, 5, 1, 2), 16.0F);
      windUpMountedPotatoCannon(scene, select.position(cannon), 30);
      overlay.showText(80)
         .attachKeyFrame()
         .text("Use rotational force to charge up the Cannon")
         .placeNearTarget()
         .pointAt(vector.blockSurface(cannon, Direction.WEST));
      scene.idle(100);
      overlay.showText(80)
         .attachKeyFrame()
         .text("After the Cannon is fully charged, power it with redstone to fire")
         .colored(PonderPalette.RED)
         .placeNearTarget()
         .pointAt(vector.blockSurface(lever, Direction.EAST).add(-0.25, 0.0, 0.0));
      scene.idle(40);
      scene.effects().indicateRedstone(lever);
      world.toggleRedstonePower(select.position(lever));
      world.cycleBlockProperty(cannon, BlockStateProperties.POWERED);

      for (int i = 0; i < 3; i++) {
         if (i == 1) {
            overlay.showText(60)
               .text("The Cannon can be continuously powered for automatic fire")
               .colored(PonderPalette.BLUE)
               .placeNearTarget()
               .pointAt(vector.centerOf(cannon));
         }

         fireMountedPotatoCannon(scene, select.position(cannon));
         playMountedPotatoCannonParticles(scene, barrelVec, aimingVec, rnd);
         spawnPotatoCannonProjectile(scene, potato, barrelVec, aimingVec, false);
         scene.idle(10);
         windUpMountedPotatoCannon(scene, select.position(cannon), 30);
         if (i == 2) {
            scene.effects().indicateRedstone(lever);
            world.toggleRedstonePower(select.position(lever));
            world.cycleBlockProperty(cannon, BlockStateProperties.POWERED);
         }

         scene.idle(50);
      }
   }

   private static void windUpMountedPotatoCannon(SceneBuilder scene, Selection cannon, int windupTime) {
      scene.world().modifyBlockEntityNBT(cannon, MountedPotatoCannonBlockEntity.class, tag -> {
         CompoundTag inventory = new CompoundTag();
         inventory.put("item", (Tag)ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, new ItemStack(Items.POTATO)).result().orElseThrow());
         tag.put("inventory", inventory);
         tag.putInt("ItemTimer", 20);
         NBTHelper.writeEnum(tag, "State", MountedPotatoCannonBlockEntity.State.CHARGING);
         tag.putInt("ChargeTimer", windupTime);
      });
   }

   private static void fireMountedPotatoCannon(SceneBuilder scene, Selection cannon) {
      scene.world().modifyBlockEntityNBT(cannon, MountedPotatoCannonBlockEntity.class, tag -> {
         tag.put("inventory", new CompoundTag());
         tag.putInt("BarrelTimer", 0);
         tag.putInt("ItemTimer", 0);
         NBTHelper.writeEnum(tag, "State", MountedPotatoCannonBlockEntity.State.FIRING);
      });
   }

   private static void playMountedPotatoCannonParticles(SceneBuilder scene, Vec3 pos, Vec3 aiming, Random rnd) {
      EffectInstructions effects = scene.effects();

      for (int i = 0; i < 8; i++) {
         Vec3 vel = aiming.add(new Vec3(rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5)).scale(1.5);
         effects.emitParticles(pos, effects.simpleParticleEmitter(new AirParticleData(0.5F, 0.1F), vel), 1.0F, 1);
      }
   }

   private static ElementLink<EntityElement> spawnPotatoCannonProjectile(SceneBuilder scene, ItemStack stack, Vec3 pos, Vec3 aiming, boolean physics) {
      return scene.world().createEntity(level -> {
         PotatoProjectileEntity entity = Objects.requireNonNull((PotatoProjectileEntity)AllEntityTypes.POTATO_PROJECTILE.create(level), "entity");
         entity.setItem(stack);
         entity.setPos(pos);
         entity.xo = pos.x;
         entity.yo = pos.y;
         entity.zo = pos.z;
         entity.setDeltaMovement(aiming.scale(2.5));
         entity.noPhysics = !physics;
         return entity;
      });
   }
}
