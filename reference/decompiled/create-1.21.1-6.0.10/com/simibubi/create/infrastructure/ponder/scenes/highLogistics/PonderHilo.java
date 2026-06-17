package com.simibubi.create.infrastructure.ponder.scenes.highLogistics;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.WiFiParticle;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class PonderHilo {
   public static void packagerCreate(CreateSceneBuilder scene, BlockPos pos, ItemStack box) {
      scene.world().modifyBlockEntity(pos, PackagerBlockEntity.class, be -> {
         be.animationTicks = 20;
         be.animationInward = false;
         be.heldBox = box;
      });
   }

   public static void packagerUnpack(CreateSceneBuilder scene, BlockPos pos, ItemStack box) {
      scene.world().modifyBlockEntity(pos, PackagerBlockEntity.class, be -> {
         be.animationTicks = 20;
         be.animationInward = true;
         be.previouslyUnwrapped = box;
      });
   }

   public static void packagerClear(CreateSceneBuilder scene, BlockPos pos) {
      scene.world().modifyBlockEntity(pos, PackagerBlockEntity.class, be -> be.heldBox = ItemStack.EMPTY);
   }

   public static ElementLink<EntityElement> packageHopsOffBelt(CreateSceneBuilder scene, BlockPos beltPos, Direction side, ItemStack box) {
      scene.world().removeItemsFromBelt(beltPos);
      return scene.world()
         .createEntity(
            l -> {
               PackageEntity packageEntity = new PackageEntity(
                  l,
                  (double)beltPos.getX() + 0.5 + (double)side.getStepX() * 0.675,
                  (double)beltPos.getY() + 0.875,
                  (double)beltPos.getZ() + 0.5 + (double)side.getStepZ() * 0.675
               );
               packageEntity.setDeltaMovement(new Vec3((double)side.getStepX(), 1.0, (double)side.getStepZ()).scale(0.125));
               packageEntity.box = box;
               return packageEntity;
            }
         );
   }

   public static void linkEffect(CreateSceneBuilder scene, BlockPos pos) {
      scene.world().flashDisplayLink(pos);
      scene.addInstruction(s -> {
         Vec3 vec3 = Vec3.atCenterOf(pos);
         s.getWorld().addParticle(new WiFiParticle.Data(), vec3.x, vec3.y, vec3.z, 1.0, 1.0, 1.0);
      });
   }

   public static void requesterEffect(CreateSceneBuilder scene, BlockPos pos) {
      scene.addInstruction(s -> {
         Vec3 vec3 = Vec3.atCenterOf(pos);
         s.getWorld().addParticle(new WiFiParticle.Data(), vec3.x, vec3.y, vec3.z, 1.0, 1.0, 1.0);
      });
   }
}
