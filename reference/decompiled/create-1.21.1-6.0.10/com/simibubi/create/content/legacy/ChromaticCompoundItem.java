package com.simibubi.create.content.legacy;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CRecipes;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class ChromaticCompoundItem extends Item {
   public ChromaticCompoundItem(Properties properties) {
      super(properties);
   }

   public int getLight(ItemStack stack) {
      return (Integer)stack.getOrDefault(AllDataComponents.CHROMATIC_COMPOUND_COLLECTING_LIGHT, 0);
   }

   public boolean isBarVisible(ItemStack stack) {
      return this.getLight(stack) > 0;
   }

   public int getBarWidth(ItemStack stack) {
      return Math.round(13.0F * (float)this.getLight(stack) / (float)((Integer)AllConfigs.server().recipes.lightSourceCountForRefinedRadiance.get()).intValue());
   }

   public int getBarColor(ItemStack stack) {
      return Color.mixColors(
         4275305, 16777215, (float)this.getLight(stack) / (float)((Integer)AllConfigs.server().recipes.lightSourceCountForRefinedRadiance.get()).intValue()
      );
   }

   public int getMaxStackSize(ItemStack stack) {
      return this.isBarVisible(stack) ? 1 : 16;
   }

   public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
      Level world = entity.level();
      ItemStack itemStack = entity.getItem();
      Vec3 positionVec = entity.position();
      CRecipes config = AllConfigs.server().recipes;
      if (world.isClientSide) {
         int light = this.getLight(itemStack);
         if (world.random.nextInt((Integer)config.lightSourceCountForRefinedRadiance.get() + 20) < light) {
            Vec3 start = VecHelper.offsetRandomly(positionVec, world.random, 3.0F);
            Vec3 motion = positionVec.subtract(start).normalize().scale(0.2F);
            world.addParticle(ParticleTypes.END_ROD, start.x, start.y, start.z, motion.x, motion.y, motion.z);
         }

         return false;
      } else {
         double y = entity.getY();
         double yMotion = entity.getDeltaMovement().y;
         int minHeight = world.getMinBuildHeight();
         CompoundTag data = entity.getPersistentData();
         if (y < (double)minHeight && y - yMotion < (double)(-10 + minHeight) && (Boolean)config.enableShadowSteelRecipe.get()) {
            ItemStack newStack = AllItems.SHADOW_STEEL.asStack();
            newStack.setCount(stack.getCount());
            data.putBoolean("JustCreated", true);
            entity.setItem(newStack);
         }

         if (!(Boolean)config.enableRefinedRadianceRecipe.get()) {
            return false;
         } else if (this.getLight(itemStack) >= (Integer)config.lightSourceCountForRefinedRadiance.get()) {
            ItemStack newStack = AllItems.REFINED_RADIANCE.asStack();
            ItemEntity newEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), newStack);
            newEntity.setDeltaMovement(entity.getDeltaMovement());
            newEntity.getPersistentData().putBoolean("JustCreated", true);
            itemStack.remove(AllDataComponents.CHROMATIC_COMPOUND_COLLECTING_LIGHT);
            world.addFreshEntity(newEntity);
            stack.split(1);
            entity.setItem(stack);
            if (stack.isEmpty()) {
               entity.discard();
            }

            return false;
         } else {
            boolean isOverBeacon = false;
            int entityX = Mth.floor(entity.getX());
            int entityZ = Mth.floor(entity.getZ());
            int localWorldHeight = world.getHeight(Types.WORLD_SURFACE, entityX, entityZ);
            MutableBlockPos testPos = new MutableBlockPos(entityX, Math.min(Mth.floor(entity.getY()), localWorldHeight), entityZ);

            while (testPos.getY() > minHeight) {
               testPos.move(Direction.DOWN);
               BlockState state = world.getBlockState(testPos);
               if (state.getLightBlock(world, testPos) >= 15 && state.getBlock() != Blocks.BEDROCK) {
                  break;
               }

               if (state.getBlock() == Blocks.BEACON) {
                  if (world.getBlockEntity(testPos) instanceof BeaconBlockEntity bte && !bte.beamSections.isEmpty()) {
                     isOverBeacon = true;
                  }
                  break;
               }
            }

            if (isOverBeacon) {
               ItemStack newStack = AllItems.REFINED_RADIANCE.asStack();
               newStack.setCount(stack.getCount());
               data.putBoolean("JustCreated", true);
               entity.setItem(newStack);
               return false;
            } else {
               RandomSource r = world.random;
               int range = 3;
               float rate = 0.5F;
               if (r.nextFloat() > rate) {
                  return false;
               } else {
                  BlockPos randomOffset = BlockPos.containing(VecHelper.offsetRandomly(positionVec, r, (float)range));
                  BlockState statex = world.getBlockState(randomOffset);
                  TransportedItemStackHandlerBehaviour behaviour = BlockEntityBehaviour.get(world, randomOffset, TransportedItemStackHandlerBehaviour.TYPE);
                  if (behaviour == null) {
                     if (this.checkLight(stack, entity, world, itemStack, positionVec, randomOffset, statex)) {
                        world.destroyBlock(randomOffset, false);
                     }

                     return false;
                  } else {
                     MutableBoolean success = new MutableBoolean(false);
                     behaviour.handleProcessingOnAllItems(ts -> {
                        ItemStack heldStack = ts.stack;
                        if (heldStack.getItem() instanceof BlockItem blockItem) {
                           if (blockItem.getBlock() == null) {
                              return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
                           } else {
                              BlockState stateToCheck = blockItem.getBlock().defaultBlockState();
                              if (!success.getValue() && this.checkLight(stack, entity, world, itemStack, positionVec, randomOffset, stateToCheck)) {
                                 success.setTrue();
                                 if (ts.stack.getCount() == 1) {
                                    return TransportedItemStackHandlerBehaviour.TransportedResult.removeItem();
                                 } else {
                                    TransportedItemStack left = ts.copy();
                                    left.stack.shrink(1);
                                    return TransportedItemStackHandlerBehaviour.TransportedResult.convertTo(left);
                                 }
                              } else {
                                 return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
                              }
                           }
                        } else {
                           return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
                        }
                     });
                     return false;
                  }
               }
            }
         }
      }
   }

   public boolean checkLight(ItemStack stack, ItemEntity entity, Level world, ItemStack itemStack, Vec3 positionVec, BlockPos randomOffset, BlockState state) {
      if (state.getLightEmission(world, randomOffset) == 0) {
         return false;
      } else if (state.getDestroySpeed(world, randomOffset) == -1.0F) {
         return false;
      } else if (state.getBlock() == Blocks.BEACON) {
         return false;
      } else {
         ClipContext context = new ClipContext(
            positionVec.add(new Vec3(0.0, 0.5, 0.0)), VecHelper.getCenterOf(randomOffset), Block.COLLIDER, Fluid.NONE, entity
         );
         if (!randomOffset.equals(world.clip(context).getBlockPos())) {
            return false;
         } else {
            ItemStack newStack = stack.split(1);
            newStack.set(AllDataComponents.CHROMATIC_COMPOUND_COLLECTING_LIGHT, this.getLight(itemStack) + 1);
            ItemEntity newEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), newStack);
            newEntity.setDeltaMovement(entity.getDeltaMovement());
            newEntity.setDefaultPickUpDelay();
            world.addFreshEntity(newEntity);
            entity.lifespan = 6000;
            if (stack.isEmpty()) {
               entity.discard();
            }

            return true;
         }
      }
   }
}
