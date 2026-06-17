package com.simibubi.create.content.equipment.extendoGrip;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent.Pre;

@EventBusSubscriber
public class ExtendoGripItem extends Item {
   public static final int MAX_DAMAGE = 200;
   public static final AttributeModifier singleRangeAttributeModifier = new AttributeModifier(
      Create.asResource("single_range_attribute_modifier"), 3.0, Operation.ADD_VALUE
   );
   public static final AttributeModifier doubleRangeAttributeModifier = new AttributeModifier(
      Create.asResource("double_range_attribute_modifier"), 5.0, Operation.ADD_VALUE
   );
   private static final Supplier<Multimap<Holder<Attribute>, AttributeModifier>> rangeModifier = Suppliers.memoize(
      () -> ImmutableMultimap.of(Attributes.BLOCK_INTERACTION_RANGE, singleRangeAttributeModifier)
   );
   private static final Supplier<Multimap<Holder<Attribute>, AttributeModifier>> doubleRangeModifier = Suppliers.memoize(
      () -> ImmutableMultimap.of(Attributes.BLOCK_INTERACTION_RANGE, doubleRangeAttributeModifier)
   );
   private static DamageSource lastActiveDamageSource;
   public static final String EXTENDO_MARKER = "createExtendo";
   public static final String DUAL_EXTENDO_MARKER = "createDualExtendo";

   public ExtendoGripItem(Properties properties) {
      super(properties.durability(200));
   }

   @SubscribeEvent
   public static void holdingExtendoGripIncreasesRange(Pre event) {
      if (event.getEntity() instanceof Player player) {
         CompoundTag var9 = player.getPersistentData();
         boolean inOff = AllItems.EXTENDO_GRIP.isIn(player.getOffhandItem());
         boolean inMain = AllItems.EXTENDO_GRIP.isIn(player.getMainHandItem());
         boolean holdingDualExtendo = inOff && inMain;
         boolean holdingExtendo = inOff ^ inMain;
         holdingExtendo &= !holdingDualExtendo;
         boolean wasHoldingExtendo = var9.contains("createExtendo");
         boolean wasHoldingDualExtendo = var9.contains("createDualExtendo");
         if (holdingExtendo != wasHoldingExtendo) {
            if (!holdingExtendo) {
               player.getAttributes().removeAttributeModifiers(rangeModifier.get());
               var9.remove("createExtendo");
            } else {
               AllAdvancements.EXTENDO_GRIP.awardTo(player);
               player.getAttributes().addTransientAttributeModifiers(rangeModifier.get());
               var9.putBoolean("createExtendo", true);
            }
         }

         if (holdingDualExtendo != wasHoldingDualExtendo) {
            if (!holdingDualExtendo) {
               player.getAttributes().removeAttributeModifiers(doubleRangeModifier.get());
               var9.remove("createDualExtendo");
            } else {
               AllAdvancements.EXTENDO_GRIP_DUAL.awardTo(player);
               player.getAttributes().addTransientAttributeModifiers(doubleRangeModifier.get());
               var9.putBoolean("createDualExtendo", true);
            }
         }
      }
   }

   @SubscribeEvent
   public static void addReachToJoiningPlayersHoldingExtendo(PlayerLoggedInEvent event) {
      Player player = event.getEntity();
      CompoundTag persistentData = player.getPersistentData();
      if (persistentData.contains("createDualExtendo")) {
         player.getAttributes().addTransientAttributeModifiers(doubleRangeModifier.get());
      } else if (persistentData.contains("createExtendo")) {
         player.getAttributes().addTransientAttributeModifiers(rangeModifier.get());
      }
   }

   @SubscribeEvent
   @OnlyIn(Dist.CLIENT)
   public static void dontMissEntitiesWhenYouHaveHighReachDistance(InteractionKeyMappingTriggered event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (mc.level != null && player != null) {
         if (isHoldingExtendoGrip(player)) {
            if (!(mc.hitResult instanceof BlockHitResult) || mc.hitResult.getType() == Type.MISS) {
               double d0 = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
               if (!player.isCreative()) {
                  d0 -= 0.5;
               }

               Vec3 Vector3d = player.getEyePosition(AnimationTickHolder.getPartialTicks());
               Vec3 Vector3d1 = player.getViewVector(1.0F);
               Vec3 Vector3d2 = Vector3d.add(Vector3d1.x * d0, Vector3d1.y * d0, Vector3d1.z * d0);
               AABB AABB = player.getBoundingBox().expandTowards(Vector3d1.scale(d0)).inflate(1.0, 1.0, 1.0);
               EntityHitResult entityraytraceresult = ProjectileUtil.getEntityHitResult(
                  player, Vector3d, Vector3d2, AABB, e -> !e.isSpectator() && e.isPickable(), d0 * d0
               );
               if (entityraytraceresult != null) {
                  Entity entity1 = entityraytraceresult.getEntity();
                  Vec3 Vector3d3 = entityraytraceresult.getLocation();
                  double d2 = Vector3d.distanceToSqr(Vector3d3);
                  if (d2 < d0 * d0 || mc.hitResult == null || mc.hitResult.getType() == Type.MISS) {
                     mc.hitResult = entityraytraceresult;
                     if (entity1 instanceof LivingEntity || entity1 instanceof ItemFrame) {
                        mc.crosshairPickEntity = entity1;
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void consumeDurabilityOnBlockBreak(BreakEvent event) {
      findAndDamageExtendoGrip(event.getPlayer());
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void consumeDurabilityOnPlace(EntityPlaceEvent event) {
      Entity entity = event.getEntity();
      if (entity instanceof Player) {
         findAndDamageExtendoGrip((Player)entity);
      }
   }

   private static void findAndDamageExtendoGrip(Player player) {
      if (player != null) {
         if (!player.level().isClientSide) {
            EquipmentSlot equipmentSlot = EquipmentSlot.MAINHAND;
            ItemStack extendo = player.getMainHandItem();
            if (!AllItems.EXTENDO_GRIP.isIn(extendo)) {
               extendo = player.getOffhandItem();
               equipmentSlot = EquipmentSlot.OFFHAND;
            }

            if (AllItems.EXTENDO_GRIP.isIn(extendo)) {
               if (!BacktankUtil.canAbsorbDamage(player, maxUses())) {
                  extendo.hurtAndBreak(1, player, equipmentSlot);
               }
            }
         }
      }
   }

   public boolean isBarVisible(ItemStack stack) {
      return BacktankUtil.isBarVisible(stack, maxUses());
   }

   public int getBarWidth(ItemStack stack) {
      return BacktankUtil.getBarWidth(stack, maxUses());
   }

   public int getBarColor(ItemStack stack) {
      return BacktankUtil.getBarColor(stack, maxUses());
   }

   private static int maxUses() {
      return (Integer)AllConfigs.server().equipment.maxExtendoGripActions.get();
   }

   public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
      return true;
   }

   @SubscribeEvent
   public static void bufferLivingAttackEvent(LivingIncomingDamageEvent event) {
      lastActiveDamageSource = event.getSource();
      DamageSource source = event.getSource();
      if (source != null) {
         Entity trueSource = source.getEntity();
         if (trueSource instanceof Player) {
            findAndDamageExtendoGrip((Player)trueSource);
         }
      }
   }

   @SubscribeEvent
   public static void attacksByExtendoGripHaveMoreKnockback(LivingKnockBackEvent event) {
      if (lastActiveDamageSource != null) {
         Entity entity = lastActiveDamageSource.getDirectEntity();
         lastActiveDamageSource = null;
         if (entity instanceof Player player) {
            if (isHoldingExtendoGrip(player)) {
               event.setStrength(event.getStrength() + 2.0F);
            }
         }
      }
   }

   private static boolean isUncaughtClientInteraction(Entity entity, Entity target) {
      if (entity.distanceToSqr(target) < 36.0) {
         return false;
      } else {
         return !entity.level().isClientSide ? false : entity instanceof Player;
      }
   }

   @SubscribeEvent
   @OnlyIn(Dist.CLIENT)
   public static void notifyServerOfLongRangeAttacks(AttackEntityEvent event) {
      Entity entity = event.getEntity();
      Entity target = event.getTarget();
      if (isUncaughtClientInteraction(entity, target)) {
         Player player = (Player)entity;
         if (isHoldingExtendoGrip(player)) {
            CatnipServices.NETWORK.sendToServer(new ExtendoGripInteractionPacket(target));
         }
      }
   }

   @SubscribeEvent
   @OnlyIn(Dist.CLIENT)
   public static void notifyServerOfLongRangeInteractions(EntityInteract event) {
      Entity entity = event.getEntity();
      Entity target = event.getTarget();
      if (isUncaughtClientInteraction(entity, target)) {
         Player player = (Player)entity;
         if (isHoldingExtendoGrip(player)) {
            CatnipServices.NETWORK.sendToServer(new ExtendoGripInteractionPacket(target, event.getHand()));
         }
      }
   }

   @SubscribeEvent
   @OnlyIn(Dist.CLIENT)
   public static void notifyServerOfLongRangeSpecificInteractions(EntityInteractSpecific event) {
      Player entity = event.getEntity();
      Entity target = event.getTarget();
      if (isUncaughtClientInteraction(entity, target)) {
         if (isHoldingExtendoGrip(entity)) {
            CatnipServices.NETWORK.sendToServer(new ExtendoGripInteractionPacket(target, event.getHand(), event.getLocalPos()));
         }
      }
   }

   public static boolean isHoldingExtendoGrip(Player player) {
      boolean inOff = AllItems.EXTENDO_GRIP.isIn(player.getOffhandItem());
      boolean inMain = AllItems.EXTENDO_GRIP.isIn(player.getMainHandItem());
      return inOff || inMain;
   }

   @OnlyIn(Dist.CLIENT)
   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(SimpleCustomRenderer.create(this, new ExtendoGripItemRenderer()));
   }
}
