package com.simibubi.create.content.equipment.sandPaper;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.item.CustomUseEffectsItem;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.simibubi.create.foundation.mixin.accessor.LivingEntityAccessor;
import java.util.function.Consumer;
import javax.annotation.ParametersAreNonnullByDefault;
import net.createmod.catnip.data.TriState;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SandPaperItem extends Item implements CustomUseEffectsItem {
   public SandPaperItem(Properties properties) {
      super(properties.durability(8));
   }

   public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
      ItemStack itemstack = playerIn.getItemInHand(handIn);
      InteractionResultHolder<ItemStack> FAIL = new InteractionResultHolder(InteractionResult.FAIL, itemstack);
      if (itemstack.has(AllDataComponents.SAND_PAPER_POLISHING)) {
         playerIn.startUsingItem(handIn);
         return new InteractionResultHolder(InteractionResult.PASS, itemstack);
      } else {
         InteractionHand otherHand = handIn == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
         ItemStack itemInOtherHand = playerIn.getItemInHand(otherHand);
         if (SandPaperPolishingRecipe.canPolish(worldIn, itemInOtherHand)) {
            ItemStack item = itemInOtherHand.copy();
            ItemStack toPolish = item.split(1);
            playerIn.startUsingItem(handIn);
            itemstack.set(AllDataComponents.SAND_PAPER_POLISHING, new SandPaperItemComponent(toPolish));
            playerIn.setItemInHand(otherHand, item);
            return new InteractionResultHolder(InteractionResult.SUCCESS, itemstack);
         } else {
            BlockHitResult raytraceresult = getPlayerPOVHitResult(worldIn, playerIn, Fluid.NONE);
            Vec3 hitVec = raytraceresult.getLocation();
            AABB bb = new AABB(hitVec, hitVec).inflate(1.0);
            ItemEntity pickUp = null;

            for (ItemEntity itemEntity : worldIn.getEntitiesOfClass(ItemEntity.class, bb)) {
               if (itemEntity.isAlive() && !(itemEntity.position().distanceTo(playerIn.position()) > 3.0)) {
                  ItemStack stack = itemEntity.getItem();
                  if (SandPaperPolishingRecipe.canPolish(worldIn, stack)) {
                     pickUp = itemEntity;
                     break;
                  }
               }
            }

            if (pickUp == null) {
               return FAIL;
            } else {
               ItemStack item = pickUp.getItem().copy();
               ItemStack toPolish = item.split(1);
               playerIn.startUsingItem(handIn);
               if (!worldIn.isClientSide) {
                  itemstack.set(AllDataComponents.SAND_PAPER_POLISHING, new SandPaperItemComponent(toPolish));
                  if (item.isEmpty()) {
                     pickUp.discard();
                  } else {
                     pickUp.setItem(item);
                  }
               }

               return new InteractionResultHolder(InteractionResult.SUCCESS, itemstack);
            }
         }
      }
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
      if (entityLiving instanceof Player player) {
         if (stack.has(AllDataComponents.SAND_PAPER_POLISHING)) {
            ItemStack toPolish = ((SandPaperItemComponent)stack.get(AllDataComponents.SAND_PAPER_POLISHING)).item();
            ItemStack polished = SandPaperPolishingRecipe.applyPolish(level, entityLiving.position(), toPolish, stack);
            if (level.isClientSide) {
               spawnParticles(entityLiving.getEyePosition(1.0F).add(entityLiving.getLookAngle().scale(0.5)), toPolish, level);
               return stack;
            }

            Inventory playerInv = player.getInventory();
            if (!polished.isEmpty()) {
               playerInv.placeItemBackInInventory(polished);
            }

            if (toPolish.hasCraftingRemainingItem()) {
               playerInv.placeItemBackInInventory(toPolish.getCraftingRemainingItem());
            }

            stack.remove(AllDataComponents.SAND_PAPER_POLISHING);
            stack.hurtAndBreak(1, entityLiving, LivingEntity.getSlotForHand(entityLiving.getUsedItemHand()));
         }

         return stack;
      } else {
         return stack;
      }
   }

   public static void spawnParticles(Vec3 location, ItemStack polishedStack, Level world) {
      for (int i = 0; i < 20; i++) {
         Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, world.random, 0.125F);
         world.addParticle(new ItemParticleOption(ParticleTypes.ITEM, polishedStack), location.x, location.y, location.z, motion.x, motion.y, motion.z);
      }
   }

   public void releaseUsing(ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) {
      if (entityLiving instanceof Player player) {
         if (stack.has(AllDataComponents.SAND_PAPER_POLISHING)) {
            ItemStack toPolish = ((SandPaperItemComponent)stack.get(AllDataComponents.SAND_PAPER_POLISHING)).item();
            player.getInventory().placeItemBackInInventory(toPolish);
            stack.remove(AllDataComponents.SAND_PAPER_POLISHING);
         }
      }
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      ItemStack stack = context.getItemInHand();
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      BlockState newState = state.getToolModifiedState(context, ItemAbilities.AXE_SCRAPE, false);
      if (newState != null) {
         AllSoundEvents.SANDING_LONG.play(level, player, pos, 1.0F, 1.0F + (level.random.nextFloat() * 0.5F - 1.0F) / 5.0F);
         level.levelEvent(player, 3005, pos, 0);
      } else {
         newState = state.getToolModifiedState(context, ItemAbilities.AXE_WAX_OFF, false);
         if (newState != null) {
            AllSoundEvents.SANDING_LONG.play(level, player, pos, 1.0F, 1.0F + (level.random.nextFloat() * 0.5F - 1.0F) / 5.0F);
            level.levelEvent(player, 3004, pos, 0);
         }
      }

      if (newState != null) {
         level.setBlockAndUpdate(pos, newState);
         if (player != null) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
      return itemAbility == ItemAbilities.AXE_SCRAPE || itemAbility == ItemAbilities.AXE_WAX_OFF;
   }

   @Override
   public TriState shouldTriggerUseEffects(ItemStack stack, LivingEntity entity) {
      return TriState.TRUE;
   }

   @Override
   public boolean triggerUseEffects(ItemStack stack, LivingEntity entity, int count, RandomSource random) {
      if (stack.has(AllDataComponents.SAND_PAPER_POLISHING)) {
         ItemStack polishing = ((SandPaperItemComponent)stack.get(AllDataComponents.SAND_PAPER_POLISHING)).item();
         if (!polishing.isEmpty()) {
            ((LivingEntityAccessor)entity).create$callSpawnItemParticles(polishing, 1);
         }
      }

      if ((entity.getTicksUsingItem() - 6) % 7 == 0) {
         entity.playSound(entity.getEatingSound(stack), 0.9F + 0.2F * random.nextFloat(), random.nextFloat() * 0.2F + 0.9F);
      }

      return true;
   }

   public SoundEvent getEatingSound() {
      return AllSoundEvents.SANDING_SHORT.getMainEvent();
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.EAT;
   }

   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 32;
   }

   public int getEnchantmentValue() {
      return 1;
   }

   @OnlyIn(Dist.CLIENT)
   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(SimpleCustomRenderer.create(this, new SandPaperItemRenderer()));
   }
}
