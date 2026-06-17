package com.simibubi.create.content.equipment.zapper;

import com.simibubi.create.AllDataComponents;
import java.util.function.Function;
import java.util.function.Predicate;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ShootableGadgetItemMethods {
   public static void applyCooldown(Player player, ItemStack item, InteractionHand hand, Predicate<ItemStack> predicate, int cooldown) {
      if (cooldown > 0) {
         boolean gunInOtherHand = predicate.test(player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND));
         player.getCooldowns().addCooldown(item.getItem(), gunInOtherHand ? cooldown * 2 / 3 : cooldown);
      }
   }

   public static void sendPackets(Player player, Function<Boolean, ? extends ShootGadgetPacket> factory) {
      if (player instanceof ServerPlayer) {
         CatnipServices.NETWORK.sendToClientsTrackingEntity(player, (CustomPacketPayload)factory.apply(false));
         CatnipServices.NETWORK.sendToClient((ServerPlayer)player, (CustomPacketPayload)factory.apply(true));
      }
   }

   public static boolean shouldSwap(Player player, ItemStack item, InteractionHand hand, Predicate<ItemStack> predicate) {
      boolean isSwap = item.has(AllDataComponents.SHAPER_SWAP);
      boolean mainHand = hand == InteractionHand.MAIN_HAND;
      boolean gunInOtherHand = predicate.test(player.getItemInHand(mainHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND));
      if (mainHand && isSwap && gunInOtherHand) {
         return true;
      } else {
         if (mainHand && !isSwap && gunInOtherHand) {
            item.set(AllDataComponents.SHAPER_SWAP, true);
         }

         if (!mainHand && isSwap) {
            item.remove(AllDataComponents.SHAPER_SWAP);
         }

         if (!mainHand && gunInOtherHand) {
            player.getItemInHand(InteractionHand.MAIN_HAND).remove(AllDataComponents.SHAPER_SWAP);
         }

         player.startUsingItem(hand);
         return false;
      }
   }

   public static Vec3 getGunBarrelVec(Player player, boolean mainHand, Vec3 rightHandForward) {
      Vec3 start = player.position().add(0.0, (double)player.getEyeHeight(), 0.0);
      float yaw = (float)((double)(player.getYRot() / -180.0F) * Math.PI);
      float pitch = (float)((double)(player.getXRot() / -180.0F) * Math.PI);
      int flip = mainHand == (player.getMainArm() == HumanoidArm.RIGHT) ? -1 : 1;
      Vec3 barrelPosNoTransform = new Vec3((double)flip * rightHandForward.x, rightHandForward.y, rightHandForward.z);
      return start.add(barrelPosNoTransform.xRot(pitch).yRot(yaw));
   }
}
