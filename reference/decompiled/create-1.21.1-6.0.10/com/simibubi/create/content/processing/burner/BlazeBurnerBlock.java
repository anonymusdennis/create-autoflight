package com.simibubi.create.content.processing.burner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import javax.annotation.ParametersAreNonnullByDefault;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BlazeBurnerBlock extends HorizontalDirectionalBlock implements IBE<BlazeBurnerBlockEntity>, IWrenchable, SpecialBlockItemRequirement {
   public static final EnumProperty<BlazeBurnerBlock.HeatLevel> HEAT_LEVEL = EnumProperty.create("blaze", BlazeBurnerBlock.HeatLevel.class);
   public static final MapCodec<BlazeBurnerBlock> CODEC = simpleCodec(BlazeBurnerBlock::new);

   public BlazeBurnerBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.NONE));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{HEAT_LEVEL, FACING});
   }

   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState p_220082_4_, boolean p_220082_5_) {
      if (!world.isClientSide) {
         if (world.getBlockEntity(pos.above()) instanceof BasinBlockEntity basin) {
            basin.notifyChangeOfContents();
         }
      }
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
      return getLitOrUnlitStack(state);
   }

   @Override
   public Class<BlazeBurnerBlockEntity> getBlockEntityClass() {
      return BlazeBurnerBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends BlazeBurnerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends BlazeBurnerBlockEntity>)AllBlockEntityTypes.HEATER.get();
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return state.getValue(HEAT_LEVEL) == BlazeBurnerBlock.HeatLevel.NONE ? null : IBE.super.newBlockEntity(pos, state);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      BlazeBurnerBlock.HeatLevel heat = (BlazeBurnerBlock.HeatLevel)state.getValue(HEAT_LEVEL);
      if (AllItems.GOGGLES.isIn(stack) && heat != BlazeBurnerBlock.HeatLevel.NONE) {
         return this.onBlockEntityUseItemOn(level, pos, bbte -> {
            if (bbte.goggles) {
               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            } else {
               bbte.goggles = true;
               bbte.notifyUpdate();
               return ItemInteractionResult.SUCCESS;
            }
         });
      } else {
         BlazeBurnerBlockEntity be = this.getBlockEntity(level, pos);
         if (be != null && be.stockKeeper) {
            StockTickerBlockEntity stockTicker = BlazeBurnerBlockEntity.getStockTicker(level, pos);
            if (stockTicker != null) {
               StockTickerInteractionHandler.interactWithLogisticsManagerAt(player, level, stockTicker.getBlockPos());
            }

            return ItemInteractionResult.SUCCESS;
         } else if (stack.isEmpty() && heat != BlazeBurnerBlock.HeatLevel.NONE) {
            return this.onBlockEntityUseItemOn(level, pos, bbte -> {
               if (!bbte.goggles) {
                  return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
               } else {
                  bbte.goggles = false;
                  bbte.notifyUpdate();
                  return ItemInteractionResult.SUCCESS;
               }
            });
         } else if (heat == BlazeBurnerBlock.HeatLevel.NONE) {
            if (stack.getItem() instanceof FlintAndSteelItem) {
               level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 0.8F);
               if (level.isClientSide) {
                  return ItemInteractionResult.SUCCESS;
               } else {
                  stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                  level.setBlockAndUpdate(pos, AllBlocks.LIT_BLAZE_BURNER.getDefaultState());
                  return ItemInteractionResult.SUCCESS;
               }
            } else {
               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
         } else {
            boolean doNotConsume = player.isCreative();
            boolean forceOverflow = !(player instanceof FakePlayer);
            InteractionResultHolder<ItemStack> res = tryInsert(state, level, pos, stack, doNotConsume, forceOverflow, false);
            ItemStack leftover = (ItemStack)res.getObject();
            if (!level.isClientSide && !doNotConsume && !leftover.isEmpty()) {
               if (stack.isEmpty()) {
                  player.setItemInHand(hand, leftover);
               } else if (!player.getInventory().add(leftover)) {
                  player.drop(leftover, false);
               }
            }

            return res.getResult() == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         }
      }
   }

   public static InteractionResultHolder<ItemStack> tryInsert(
      BlockState state, Level world, BlockPos pos, ItemStack stack, boolean doNotConsume, boolean forceOverflow, boolean simulate
   ) {
      if (!state.hasBlockEntity()) {
         return InteractionResultHolder.fail(ItemStack.EMPTY);
      } else if (world.getBlockEntity(pos) instanceof BlazeBurnerBlockEntity burnerBE) {
         if (burnerBE.isCreativeFuel(stack)) {
            if (!simulate) {
               burnerBE.applyCreativeFuel();
            }

            return InteractionResultHolder.success(ItemStack.EMPTY);
         } else if (!burnerBE.tryUpdateFuel(stack, forceOverflow, simulate)) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
         } else if (!doNotConsume) {
            ItemStack container = stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
            if (!world.isClientSide) {
               stack.shrink(1);
            }

            return InteractionResultHolder.success(container);
         } else {
            return InteractionResultHolder.success(ItemStack.EMPTY);
         }
      } else {
         return InteractionResultHolder.fail(ItemStack.EMPTY);
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      ItemStack stack = context.getItemInHand();
      Item item = stack.getItem();
      BlockState defaultState = this.defaultBlockState();
      if (!(item instanceof BlazeBurnerBlockItem)) {
         return defaultState;
      } else {
         BlazeBurnerBlock.HeatLevel initialHeat = ((BlazeBurnerBlockItem)item).hasCapturedBlaze()
            ? BlazeBurnerBlock.HeatLevel.SMOULDERING
            : BlazeBurnerBlock.HeatLevel.NONE;
         return (BlockState)((BlockState)defaultState.setValue(HEAT_LEVEL, initialHeat)).setValue(FACING, context.getHorizontalDirection().getOpposite());
      }
   }

   public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
      return AllShapes.HEATER_BLOCK_SHAPE;
   }

   public VoxelShape getCollisionShape(BlockState p_220071_1_, BlockGetter p_220071_2_, BlockPos p_220071_3_, CollisionContext p_220071_4_) {
      return p_220071_4_ == CollisionContext.empty()
         ? AllShapes.HEATER_BLOCK_SPECIAL_COLLISION_SHAPE
         : this.getShape(p_220071_1_, p_220071_2_, p_220071_3_, p_220071_4_);
   }

   public boolean hasAnalogOutputSignal(BlockState p_149740_1_) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState state, Level p_180641_2_, BlockPos p_180641_3_) {
      return Math.max(0, ((BlazeBurnerBlock.HeatLevel)state.getValue(HEAT_LEVEL)).ordinal() - 1);
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
      if (random.nextInt(10) == 0) {
         if (((BlazeBurnerBlock.HeatLevel)state.getValue(HEAT_LEVEL)).isAtLeast(BlazeBurnerBlock.HeatLevel.SMOULDERING)) {
            world.playLocalSound(
               (double)((float)pos.getX() + 0.5F),
               (double)((float)pos.getY() + 0.5F),
               (double)((float)pos.getZ() + 0.5F),
               SoundEvents.CAMPFIRE_CRACKLE,
               SoundSource.BLOCKS,
               0.5F + random.nextFloat(),
               random.nextFloat() * 0.7F + 0.6F,
               false
            );
         }
      }
   }

   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   public static BlazeBurnerBlock.HeatLevel getHeatLevelOf(BlockState blockState) {
      return blockState.hasProperty(HEAT_LEVEL) ? (BlazeBurnerBlock.HeatLevel)blockState.getValue(HEAT_LEVEL) : BlazeBurnerBlock.HeatLevel.NONE;
   }

   public static int getLight(BlockState state) {
      BlazeBurnerBlock.HeatLevel level = (BlazeBurnerBlock.HeatLevel)state.getValue(HEAT_LEVEL);

      return switch (level) {
         case NONE -> 0;
         case SMOULDERING -> 8;
         default -> 15;
      };
   }

   public static net.minecraft.world.level.storage.loot.LootTable.Builder buildLootTable() {
      net.minecraft.world.level.storage.loot.predicates.LootItemCondition.Builder survivesExplosion = ExplosionCondition.survivesExplosion();
      BlazeBurnerBlock block = (BlazeBurnerBlock)AllBlocks.BLAZE_BURNER.get();
      net.minecraft.world.level.storage.loot.LootTable.Builder builder = LootTable.lootTable();
      net.minecraft.world.level.storage.loot.LootPool.Builder poolBuilder = LootPool.lootPool();

      for (BlazeBurnerBlock.HeatLevel level : BlazeBurnerBlock.HeatLevel.values()) {
         ItemLike drop = level == BlazeBurnerBlock.HeatLevel.NONE ? (ItemLike)AllItems.EMPTY_BLAZE_BURNER.get() : (ItemLike)AllBlocks.BLAZE_BURNER.get();
         poolBuilder.add(
            ((net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer.Builder)LootItem.lootTableItem(drop).when(survivesExplosion))
               .when(
                  LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                     .setProperties(net.minecraft.advancements.critereon.StatePropertiesPredicate.Builder.properties().hasProperty(HEAT_LEVEL, level))
               )
         );
      }

      builder.withPool(poolBuilder.setRolls(ConstantValue.exactly(1.0F)));
      return builder;
   }

   @Override
   public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
      return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, getLitOrUnlitStack(state));
   }

   private static ItemStack getLitOrUnlitStack(BlockState state) {
      boolean isLit = state.getValue(HEAT_LEVEL) != BlazeBurnerBlock.HeatLevel.NONE;
      return (isLit ? AllBlocks.BLAZE_BURNER : AllItems.EMPTY_BLAZE_BURNER).asStack();
   }

   public static enum HeatLevel implements StringRepresentable {
      NONE,
      SMOULDERING,
      FADING,
      KINDLED,
      SEETHING;

      public static final Codec<BlazeBurnerBlock.HeatLevel> CODEC = StringRepresentable.fromEnum(BlazeBurnerBlock.HeatLevel::values);

      public static BlazeBurnerBlock.HeatLevel byIndex(int index) {
         return values()[index];
      }

      public BlazeBurnerBlock.HeatLevel nextActiveLevel() {
         return byIndex(this.ordinal() % (values().length - 1) + 1);
      }

      public boolean isAtLeast(BlazeBurnerBlock.HeatLevel heatLevel) {
         return this.ordinal() >= heatLevel.ordinal();
      }

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}
