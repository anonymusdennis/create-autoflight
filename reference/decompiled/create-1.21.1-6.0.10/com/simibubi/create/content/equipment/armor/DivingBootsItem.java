package com.simibubi.create.content.equipment.armor;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent.Pre;

@EventBusSubscriber
public class DivingBootsItem extends BaseArmorItem {
   public static final EquipmentSlot SLOT = EquipmentSlot.FEET;
   public static final Type TYPE = Type.BOOTS;

   public DivingBootsItem(Holder<ArmorMaterial> material, Properties properties, ResourceLocation textureLoc) {
      super(material, TYPE, properties, textureLoc);
   }

   public static boolean isWornBy(Entity entity) {
      return !getWornItem(entity).isEmpty();
   }

   public static ItemStack getWornItem(Entity entity) {
      if (entity instanceof LivingEntity livingEntity) {
         ItemStack stack = livingEntity.getItemBySlot(SLOT);
         return !(stack.getItem() instanceof DivingBootsItem) ? ItemStack.EMPTY : stack;
      } else {
         return ItemStack.EMPTY;
      }
   }

   @SubscribeEvent
   public static void accelerateDescentUnderwater(Pre event) {
      if (event.getEntity() instanceof LivingEntity entity) {
         if (affects(entity)) {
            Vec3 motion = entity.getDeltaMovement();
            boolean isJumping = entity.jumping;
            entity.setOnGround(entity.onGround() || entity.verticalCollision);
            if (isJumping && entity.onGround()) {
               motion = motion.add(0.0, 0.5, 0.0);
               entity.setOnGround(false);
            } else {
               motion = motion.add(0.0, -0.05F, 0.0);
            }

            float multiplier = 1.3F;
            if (motion.multiply(1.0, 0.0, 1.0).length() < 0.145F && (entity.zza > 0.0F || entity.xxa != 0.0F) && !entity.isShiftKeyDown()) {
               motion = motion.multiply((double)multiplier, 1.0, (double)multiplier);
            }

            entity.setDeltaMovement(motion);
         }
      }
   }

   protected static boolean affects(LivingEntity entity) {
      if (!isWornBy(entity)) {
         entity.getPersistentData().remove("HeavyBoots");
         return false;
      } else {
         NBTHelper.putMarker(entity.getPersistentData(), "HeavyBoots");
         if (!entity.isInWater()) {
            return false;
         } else if (entity.getPose() == Pose.SWIMMING) {
            return false;
         } else {
            if (entity instanceof Player playerEntity && playerEntity.getAbilities().flying) {
               return false;
            }

            return true;
         }
      }
   }

   public static Vec3 getMovementMultiplier(LivingEntity entity) {
      double yMotion = entity.getDeltaMovement().y;
      double vMultiplier = yMotion < 0.0 ? Math.max(0.0, 2.5 - Math.abs(yMotion) * 2.0) : 1.0;
      if (entity.onGround()) {
         entity.getPersistentData().putBoolean("LavaGrounded", true);
         double hMultiplier = entity.isSprinting() ? 1.85 : 1.75;
         return new Vec3(hMultiplier, vMultiplier, hMultiplier);
      } else {
         if (entity.jumping && entity.getPersistentData().contains("LavaGrounded")) {
            boolean eyeInFluid = entity.isEyeInFluid(FluidTags.LAVA);
            vMultiplier = yMotion == 0.0 ? 0.0 : (eyeInFluid ? 1.0 : 0.5) / yMotion;
         } else if (yMotion > 0.0) {
            vMultiplier = 1.3;
         }

         entity.getPersistentData().remove("LavaGrounded");
         return new Vec3(1.75, vMultiplier, 1.75);
      }
   }
}
