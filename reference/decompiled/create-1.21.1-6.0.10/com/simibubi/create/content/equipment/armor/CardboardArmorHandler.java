package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.foundation.advancement.AllAdvancements;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent.Size;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingVisibilityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent.Pre;

@EventBusSubscriber
public class CardboardArmorHandler {
   @SubscribeEvent
   public static void playerHitboxChangesWhenHidingAsBox(Size event) {
      Entity entity = event.getEntity();
      if (entity.isAddedToLevel()) {
         if (testForStealth(entity)) {
            float scale;
            if (entity instanceof LivingEntity le) {
               scale = le.getScale();
            } else {
               scale = 1.0F;
            }

            event.setNewSize(EntityDimensions.fixed(0.6F * scale, 0.8F * scale).withEyeHeight(0.6F * scale));
            if (!entity.level().isClientSide() && entity instanceof Player p) {
               AllAdvancements.CARDBOARD_ARMOR.awardTo(p);
            }
         }
      }
   }

   @SubscribeEvent
   public static void playerChangesEquipment(LivingEquipmentChangeEvent event) {
      if (event.getEntity() instanceof Player player
         && player.getPose() == Pose.CROUCHING
         && (
            isCardboardArmor(player.getItemBySlot(EquipmentSlot.HEAD))
               || isCardboardArmor(player.getItemBySlot(EquipmentSlot.CHEST))
               || isCardboardArmor(player.getItemBySlot(EquipmentSlot.LEGS))
               || isCardboardArmor(player.getItemBySlot(EquipmentSlot.FEET))
         )
         && !player.level().isClientSide()) {
         Pose pose = player.getPose();
         player.setPose(pose == Pose.CROUCHING ? Pose.STANDING : Pose.CROUCHING);
         player.setPose(pose);
      }
   }

   @SubscribeEvent
   public static void playersStealthWhenWearingCardboard(LivingVisibilityEvent event) {
      LivingEntity entity = event.getEntity();
      if (testForStealth(entity)) {
         event.modifyVisibility(0.0);
      }
   }

   @SubscribeEvent
   public static void mobsMayLoseTargetWhenItIsWearingCardboard(Pre event) {
      if (event.getEntity() instanceof LivingEntity entity) {
         if (entity.tickCount % 16 == 0) {
            if (entity instanceof Mob mob) {
               if (testForStealth(mob.getTarget())) {
                  mob.setTarget(null);
                  if (mob.targetSelector != null) {
                     for (WrappedGoal goal : mob.targetSelector.getAvailableGoals()) {
                        if (goal.isRunning() && goal.getGoal() instanceof TargetGoal tg) {
                           tg.stop();
                        }
                     }
                  }
               }

               if (entity instanceof NeutralMob nMob && entity.level() instanceof ServerLevel sl) {
                  UUID uuid = nMob.getPersistentAngerTarget();
                  if (uuid != null && testForStealth(sl.getEntity(uuid))) {
                     nMob.stopBeingAngry();
                  }
               }

               if (testForStealth(mob.getLastHurtByMob())) {
                  mob.setLastHurtByMob(null);
                  mob.setLastHurtByPlayer(null);
               }
            }
         }
      }
   }

   public static boolean testForStealth(Entity entityIn) {
      if (entityIn instanceof LivingEntity entity) {
         if (entity.getPose() != Pose.CROUCHING) {
            return false;
         } else {
            if (entity instanceof Player player && player.getAbilities().flying) {
               return false;
            }

            if (!isCardboardArmor(entity.getItemBySlot(EquipmentSlot.HEAD))) {
               return false;
            } else if (!isCardboardArmor(entity.getItemBySlot(EquipmentSlot.CHEST))) {
               return false;
            } else {
               return !isCardboardArmor(entity.getItemBySlot(EquipmentSlot.LEGS)) ? false : isCardboardArmor(entity.getItemBySlot(EquipmentSlot.FEET));
            }
         }
      } else {
         return false;
      }
   }

   public static boolean isCardboardArmor(ItemStack stack) {
      return stack.getItem() instanceof CardboardArmorItem;
   }
}
