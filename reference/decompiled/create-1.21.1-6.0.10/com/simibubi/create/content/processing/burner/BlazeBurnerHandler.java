package com.simibubi.create.content.processing.burner;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

@EventBusSubscriber
public class BlazeBurnerHandler {
   @SubscribeEvent
   public static void onThrowableImpact(ProjectileImpactEvent event) {
      thrownEggsGetEatenByBurner(event);
      splashExtinguishesBurner(event);
   }

   public static void thrownEggsGetEatenByBurner(ProjectileImpactEvent event) {
      Projectile projectile = event.getProjectile();
      if (projectile instanceof ThrownEgg) {
         if (event.getRayTraceResult().getType() == Type.BLOCK) {
            if (projectile.level().getBlockEntity(BlockPos.containing(event.getRayTraceResult().getLocation())) instanceof BlazeBurnerBlockEntity heater) {
               event.setCanceled(true);
               projectile.setDeltaMovement(Vec3.ZERO);
               projectile.discard();
               Level world = projectile.level();
               if (!world.isClientSide) {
                  if (!heater.isCreative() && heater.activeFuel != BlazeBurnerBlockEntity.FuelType.SPECIAL) {
                     heater.activeFuel = BlazeBurnerBlockEntity.FuelType.NORMAL;
                     heater.remainingBurnTime = Mth.clamp(heater.remainingBurnTime + 80, 0, 10000);
                     heater.updateBlockState();
                     heater.notifyUpdate();
                  }

                  AllSoundEvents.BLAZE_MUNCH.playOnServer(world, heater.getBlockPos());
               }
            }
         }
      }
   }

   public static void splashExtinguishesBurner(ProjectileImpactEvent event) {
      Projectile projectile = event.getProjectile();
      if (!projectile.level().isClientSide) {
         if (projectile instanceof ThrownPotion entity) {
            if (event.getRayTraceResult().getType() == Type.BLOCK) {
               ItemStack stack = entity.getItem();
               PotionContents potionContents = (PotionContents)stack.get(DataComponents.POTION_CONTENTS);
               if (potionContents != null && potionContents.is(Potions.WATER) && !potionContents.hasEffects()) {
                  BlockHitResult result = (BlockHitResult)event.getRayTraceResult();
                  Level world = entity.level();
                  Direction face = result.getDirection();
                  BlockPos pos = result.getBlockPos().relative(face);
                  extinguishLitBurners(world, pos, face);
                  extinguishLitBurners(world, pos.relative(face.getOpposite()), face);

                  for (Direction face1 : Plane.HORIZONTAL) {
                     extinguishLitBurners(world, pos.relative(face1), face1);
                  }
               }
            }
         }
      }
   }

   private static void extinguishLitBurners(Level world, BlockPos pos, Direction direction) {
      BlockState state = world.getBlockState(pos);
      if (AllBlocks.LIT_BLAZE_BURNER.has(state)) {
         world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
         world.setBlockAndUpdate(pos, AllBlocks.BLAZE_BURNER.getDefaultState());
      }
   }
}
