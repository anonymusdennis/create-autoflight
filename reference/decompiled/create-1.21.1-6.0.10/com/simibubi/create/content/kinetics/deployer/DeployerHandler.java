package com.simibubi.create.content.kinetics.deployer;

import com.google.common.collect.HashMultimap;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockItem;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItemComponent;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.extensions.IBaseRailBlockExtension;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public class DeployerHandler {
   private static final Map<BlockPos, List<ItemEntity>> CAPTURED_BLOCK_DROPS = new HashMap<>();
   public static final Map<BlockPos, List<ItemEntity>> CAPTURED_BLOCK_DROPS_VIEW = Collections.unmodifiableMap(CAPTURED_BLOCK_DROPS);

   static boolean shouldActivate(ItemStack held, Level world, BlockPos targetPos, @Nullable Direction facing) {
      if (held.getItem() instanceof BlockItem && world.getBlockState(targetPos).getBlock() == ((BlockItem)held.getItem()).getBlock()) {
         return false;
      } else {
         if (held.getItem() instanceof BucketItem bucketItem) {
            Fluid fluid = bucketItem.content;
            if (fluid != Fluids.EMPTY && world.getFluidState(targetPos).getType() == fluid) {
               return false;
            }
         }

         return held.isEmpty() || facing != Direction.DOWN || BlockEntityBehaviour.get(world, targetPos, TransportedItemStackHandlerBehaviour.TYPE) == null;
      }
   }

   static void activate(DeployerFakePlayer player, Vec3 vec, BlockPos clickedPos, Vec3 extensionVector, DeployerBlockEntity.Mode mode) {
      HashMultimap<Holder<Attribute>, AttributeModifier> attributeModifiers = HashMultimap.create();
      ItemStack mainHandItem = player.getMainHandItem();
      mainHandItem.getAttributeModifiers().modifiers().forEach(e -> attributeModifiers.put(e.attribute(), e.modifier()));
      EnchantmentHelper.forEachModifier(mainHandItem, EquipmentSlot.MAINHAND, (x$0, x$1) -> attributeModifiers.put(x$0, x$1));
      player.getAttributes().addTransientAttributeModifiers(attributeModifiers);
      activateInner(player, vec, clickedPos, extensionVector, mode);
      player.getAttributes().removeAttributeModifiers(attributeModifiers);
   }

   private static void activateInner(DeployerFakePlayer player, Vec3 vec, BlockPos clickedPos, Vec3 extensionVector, DeployerBlockEntity.Mode mode) {
      Vec3 rayOrigin = vec.add(extensionVector.scale(1.515625));
      Vec3 rayTarget = vec.add(extensionVector.scale(2.484375));
      player.setPos(rayOrigin.x, rayOrigin.y, rayOrigin.z);
      BlockPos pos = BlockPos.containing(vec);
      ItemStack stack = player.getMainHandItem();
      Item item = stack.getItem();
      ServerLevel level = player.serverLevel();
      List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AABB(clickedPos))
         .stream()
         .filter(e -> !(e instanceof AbstractContraptionEntity))
         .toList();
      InteractionHand hand = InteractionHand.MAIN_HAND;
      if (!entities.isEmpty()) {
         Entity entity = entities.get(level.random.nextInt(entities.size()));
         List<ItemEntity> capturedDrops = new ArrayList<>();
         boolean success = false;
         entity.captureDrops(capturedDrops);
         if (mode == DeployerBlockEntity.Mode.USE) {
            InteractionResult cancelResult = CommonHooks.onInteractEntity(player, entity, hand);
            if (cancelResult == InteractionResult.FAIL) {
               entity.captureDrops(null);
               return;
            }

            if (cancelResult == null) {
               if (entity.interact(player, hand).consumesAction()) {
                  if (entity instanceof AbstractVillager villager && villager.getTradingPlayer() instanceof DeployerFakePlayer) {
                     villager.setTradingPlayer(null);
                  }

                  success = true;
               } else if (entity instanceof LivingEntity livingEntity && stack.interactLivingEntity(player, livingEntity, hand).consumesAction()) {
                  success = true;
               }
            }

            if (!success && entity instanceof Player playerEntity) {
               if (stack.has(DataComponents.FOOD)) {
                  FoodProperties foodProperties = item.getFoodProperties(stack, player);
                  if (foodProperties != null && playerEntity.canEat(foodProperties.canAlwaysEat())) {
                     ItemStack copy = stack.copy();
                     player.setItemInHand(hand, stack.finishUsingItem(level, playerEntity));
                     player.spawnedItemEffects = copy;
                     success = true;
                  }
               }

               if (AllTags.AllItemTags.DEPLOYABLE_DRINK.matches(stack)) {
                  player.spawnedItemEffects = stack.copy();
                  player.setItemInHand(hand, stack.finishUsingItem(level, playerEntity));
                  success = true;
               }
            }
         }

         if (mode == DeployerBlockEntity.Mode.PUNCH) {
            player.resetAttackStrengthTicker();
            player.attack(entity);
            success = true;
         }

         entity.captureDrops(null);
         capturedDrops.forEach(e -> player.getInventory().placeItemBackInInventory(e.getItem()));
         if (success) {
            return;
         }
      }

      ClipContext rayTraceContext = new ClipContext(rayOrigin, rayTarget, Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, player);
      BlockHitResult result = level.clip(rayTraceContext);
      if (result.getBlockPos() != clickedPos) {
         result = new BlockHitResult(result.getLocation(), result.getDirection(), clickedPos, result.isInside());
      }

      BlockState clickedState = level.getBlockState(clickedPos);
      Direction face = result.getDirection();
      if (face == null) {
         face = Direction.getNearest(extensionVector.x, extensionVector.y, extensionVector.z).getOpposite();
      }

      if (mode == DeployerBlockEntity.Mode.PUNCH) {
         if (level.mayInteract(player, clickedPos)) {
            if (clickedState.getShape(level, clickedPos).isEmpty()) {
               player.blockBreakingProgress = null;
            } else {
               LeftClickBlock event = CommonHooks.onLeftClickBlock(player, clickedPos, face, Action.START_DESTROY_BLOCK);
               if (!event.isCanceled()) {
                  if (!BlockHelper.extinguishFire(level, player, clickedPos, face)) {
                     if (event.getUseBlock() != TriState.FALSE) {
                        clickedState.attack(level, clickedPos, player);
                     }

                     if (!stack.isEmpty()) {
                        float progress = clickedState.getDestroyProgress(player, level, clickedPos) * 16.0F;
                        float before = 0.0F;
                        Pair<BlockPos, Float> blockBreakingProgress = player.blockBreakingProgress;
                        if (blockBreakingProgress != null) {
                           before = (Float)blockBreakingProgress.getValue();
                        }

                        progress += before;
                        level.playSound(null, clickedPos, clickedState.getSoundType().getHitSound(), SoundSource.NEUTRAL, 0.25F, 1.0F);
                        if (progress >= 1.0F) {
                           tryHarvestBlock(player, player.gameMode, clickedPos);
                           level.destroyBlockProgress(player.getId(), clickedPos, -1);
                           player.blockBreakingProgress = null;
                        } else if (progress <= 0.0F) {
                           player.blockBreakingProgress = null;
                        } else {
                           if ((int)(before * 10.0F) != (int)(progress * 10.0F)) {
                              level.destroyBlockProgress(player.getId(), clickedPos, (int)(progress * 10.0F));
                           }

                           player.blockBreakingProgress = Pair.of(clickedPos, progress);
                        }
                     }
                  }
               }
            }
         }
      } else {
         UseOnContext itemusecontext = new UseOnContext(player, hand, result);
         TriState useBlock = TriState.DEFAULT;
         TriState useItem = TriState.DEFAULT;
         if (!clickedState.getShape(level, clickedPos).isEmpty()) {
            RightClickBlock event = CommonHooks.onRightClickBlock(player, hand, clickedPos, result);
            useBlock = event.getUseBlock();
            useItem = event.getUseItem();
         }

         if (useItem != TriState.FALSE) {
            InteractionResult actionresult = stack.onItemUseFirst(itemusecontext);
            if (actionresult != InteractionResult.PASS) {
               return;
            }
         }

         boolean holdingSomething = !player.getMainHandItem().isEmpty();
         boolean flag1 = !player.isShiftKeyDown() || !holdingSomething || stack.doesSneakBypassUse(level, clickedPos, player);
         if (useBlock == TriState.FALSE || !flag1 || !safeOnUse(clickedState, level, clickedPos, player, hand, result).consumesAction()) {
            if (!stack.isEmpty()) {
               if (useItem != TriState.FALSE) {
                  if (!(item instanceof CartAssemblerBlockItem) || !clickedState.canBeReplaced(new BlockPlaceContext(itemusecontext))) {
                     if (item == Items.FLINT_AND_STEEL) {
                        Direction newFace = result.getDirection();
                        BlockPos newPos = result.getBlockPos();
                        if (!BaseFireBlock.canBePlacedAt(level, clickedPos, newFace)) {
                           newFace = Direction.UP;
                        }

                        if (clickedState.isAir()) {
                           newPos = newPos.relative(face.getOpposite());
                        }

                        result = new BlockHitResult(result.getLocation(), newFace, newPos, result.isInside());
                        itemusecontext = new UseOnContext(player, hand, result);
                     }

                     InteractionResult onItemUse = stack.useOn(itemusecontext);
                     if (onItemUse.consumesAction()) {
                        if (item instanceof BlockItem bi && (bi.getBlock() instanceof IBaseRailBlockExtension || bi.getBlock() instanceof ITrackBlock)) {
                           player.placedTracks = true;
                        }
                     } else if (item != Items.ENDER_PEARL) {
                        if (!AllTags.AllItemTags.DEPLOYABLE_DRINK.matches(item)) {
                           Level itemUseWorld = level;
                           if (item instanceof BucketItem || item instanceof SandPaperItem) {
                              itemUseWorld = new DeployerHandler.ItemUseWorld(level, face, pos);
                           }

                           InteractionResultHolder<ItemStack> onItemRightClick = item.use(itemUseWorld, player, hand);
                           if (onItemRightClick.getResult().consumesAction() && item instanceof MobBucketItem bucketItem) {
                              bucketItem.checkExtraContent(player, level, stack, clickedPos);
                           }

                           ItemStack resultStack = (ItemStack)onItemRightClick.getObject();
                           if (resultStack != stack
                              || resultStack.getCount() != stack.getCount()
                              || resultStack.getUseDuration(player) > 0
                              || resultStack.getDamageValue() != stack.getDamageValue()) {
                              player.setItemInHand(hand, (ItemStack)onItemRightClick.getObject());
                           }

                           if (stack.getItem() instanceof SandPaperItem && stack.has(AllDataComponents.SAND_PAPER_POLISHING)) {
                              player.spawnedItemEffects = ((SandPaperItemComponent)stack.get(AllDataComponents.SAND_PAPER_POLISHING)).item();
                              AllSoundEvents.SANDING_SHORT.playOnServer(level, pos, 0.25F, 1.0F);
                           }

                           if (!player.getUseItem().isEmpty()) {
                              player.setItemInHand(hand, stack.finishUsingItem(level, player));
                           }

                           player.stopUsingItem();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean tryHarvestBlock(ServerPlayer player, ServerPlayerGameMode interactionManager, BlockPos pos) {
      ServerLevel world = player.serverLevel();
      BlockState blockstate = world.getBlockState(pos);
      GameType gameType = interactionManager.getGameModeForPlayer();
      if (CommonHooks.fireBlockBreak(world, gameType, player, pos, blockstate).isCanceled()) {
         return false;
      } else {
         BlockEntity blockEntity = world.getBlockEntity(pos);
         if (player.blockActionRestricted(world, pos, gameType)) {
            return false;
         } else {
            ItemStack prevHeldItem = player.getMainHandItem();
            ItemStack heldItem = prevHeldItem.copy();
            boolean canHarvest = blockstate.canHarvestBlock(world, pos, player);
            prevHeldItem.mineBlock(world, blockstate, pos, player);
            if (prevHeldItem.isEmpty() && !heldItem.isEmpty()) {
               EventHooks.onPlayerDestroyItem(player, heldItem, InteractionHand.MAIN_HAND);
            }

            BlockPos posUp = pos.above();
            BlockState stateUp = world.getBlockState(posUp);
            if (blockstate.getBlock() instanceof DoublePlantBlock
               && blockstate.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER
               && stateUp.getBlock() == blockstate.getBlock()
               && stateUp.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
               world.setBlock(pos, Blocks.AIR.defaultBlockState(), 35);
               world.setBlock(posUp, Blocks.AIR.defaultBlockState(), 35);
            } else if (!blockstate.onDestroyedByPlayer(world, pos, player, canHarvest, world.getFluidState(pos))) {
               return true;
            }

            blockstate.getBlock().destroy(world, pos, blockstate);
            if (!canHarvest) {
               return true;
            } else {
               net.minecraft.world.level.block.Block.getDrops(blockstate, world, pos, blockEntity, player, prevHeldItem)
                  .forEach(item -> player.getInventory().placeItemBackInInventory(item));
               blockstate.spawnAfterBreak(world, pos, prevHeldItem, true);
               return true;
            }
         }
      }
   }

   public static InteractionResult safeOnUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
      List<ItemEntity> drops = new ArrayList<>(4);
      CAPTURED_BLOCK_DROPS.put(pos, drops);

      InteractionResult var13;
      try {
         InteractionResult result = BlockHelper.invokeUse(state, world, player, hand, ray);

         for (ItemEntity itemEntity : drops) {
            player.getInventory().placeItemBackInInventory(itemEntity.getItem());
         }

         var13 = result;
      } finally {
         CAPTURED_BLOCK_DROPS.remove(pos);
      }

      return var13;
   }

   private static final class ItemUseWorld extends WrappedLevel implements ServerLevelAccessor {
      private final Direction face;
      private final BlockPos pos;
      boolean rayMode = false;

      private ItemUseWorld(ServerLevel level, Direction face, BlockPos pos) {
         super(level);
         this.face = face;
         this.pos = pos;
      }

      public ServerLevel getLevel() {
         return (ServerLevel)this.level;
      }

      public BlockHitResult clip(ClipContext context) {
         this.rayMode = true;
         BlockHitResult rayTraceBlocks = super.clip(context);
         this.rayMode = false;
         return rayTraceBlocks;
      }

      public BlockState getBlockState(BlockPos position) {
         return !this.rayMode
               || !this.pos.relative(this.face.getOpposite(), 3).equals(position) && !this.pos.relative(this.face.getOpposite(), 1).equals(position)
            ? this.level.getBlockState(position)
            : Blocks.BEDROCK.defaultBlockState();
      }
   }
}
