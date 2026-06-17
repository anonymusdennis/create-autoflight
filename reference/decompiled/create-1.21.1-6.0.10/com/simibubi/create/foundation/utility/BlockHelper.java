package com.simibubi.create.foundation.utility;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.schematic.nbt.PartialSafeNBT;
import com.simibubi.create.api.schematic.nbt.SafeNbtWriterRegistry;
import com.simibubi.create.api.schematic.state.SchematicStateFilter;
import com.simibubi.create.api.schematic.state.SchematicStateFilterRegistry;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.framedblocks.FramedBlocksInSchematics;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.IMergeableBE;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.createmod.catnip.nbt.NBTProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.SpecialPlantable;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import org.jetbrains.annotations.Nullable;

public class BlockHelper {
   private static final List<IntegerProperty> COUNT_STATES = List.of(BlockStateProperties.EGGS, BlockStateProperties.PICKLES, BlockStateProperties.CANDLES);
   private static final List<Block> VINELIKE_BLOCKS = List.of(Blocks.VINE, Blocks.GLOW_LICHEN);
   private static final List<BooleanProperty> VINELIKE_STATES = List.of(
      BlockStateProperties.UP,
      BlockStateProperties.NORTH,
      BlockStateProperties.EAST,
      BlockStateProperties.SOUTH,
      BlockStateProperties.WEST,
      BlockStateProperties.DOWN
   );

   public static BlockState setZeroAge(BlockState blockState) {
      if (blockState.hasProperty(BlockStateProperties.AGE_1)) {
         return (BlockState)blockState.setValue(BlockStateProperties.AGE_1, 0);
      } else if (blockState.hasProperty(BlockStateProperties.AGE_2)) {
         return (BlockState)blockState.setValue(BlockStateProperties.AGE_2, 0);
      } else if (blockState.hasProperty(BlockStateProperties.AGE_3)) {
         return (BlockState)blockState.setValue(BlockStateProperties.AGE_3, 0);
      } else if (blockState.hasProperty(BlockStateProperties.AGE_5)) {
         return (BlockState)blockState.setValue(BlockStateProperties.AGE_5, 0);
      } else if (blockState.hasProperty(BlockStateProperties.AGE_7)) {
         return (BlockState)blockState.setValue(BlockStateProperties.AGE_7, 0);
      } else if (blockState.hasProperty(BlockStateProperties.AGE_15)) {
         return (BlockState)blockState.setValue(BlockStateProperties.AGE_15, 0);
      } else if (blockState.hasProperty(BlockStateProperties.AGE_25)) {
         return (BlockState)blockState.setValue(BlockStateProperties.AGE_25, 0);
      } else if (blockState.hasProperty(BlockStateProperties.LEVEL_HONEY)) {
         return (BlockState)blockState.setValue(BlockStateProperties.LEVEL_HONEY, 0);
      } else if (blockState.hasProperty(BlockStateProperties.HATCH)) {
         return (BlockState)blockState.setValue(BlockStateProperties.HATCH, 0);
      } else if (blockState.hasProperty(BlockStateProperties.STAGE)) {
         return (BlockState)blockState.setValue(BlockStateProperties.STAGE, 0);
      } else if (blockState.is(BlockTags.CAULDRONS)) {
         return Blocks.CAULDRON.defaultBlockState();
      } else if (blockState.hasProperty(BlockStateProperties.LEVEL_COMPOSTER)) {
         return (BlockState)blockState.setValue(BlockStateProperties.LEVEL_COMPOSTER, 0);
      } else {
         return blockState.hasProperty(BlockStateProperties.EXTENDED) ? (BlockState)blockState.setValue(BlockStateProperties.EXTENDED, false) : blockState;
      }
   }

   public static int findAndRemoveInInventory(BlockState block, Player player, int amount) {
      int amountFound = 0;
      Item required = getRequiredItem(block).getItem();
      boolean needsTwo = block.hasProperty(BlockStateProperties.SLAB_TYPE) && block.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE;
      if (needsTwo) {
         amount *= 2;
      }

      for (IntegerProperty property : COUNT_STATES) {
         if (block.hasProperty(property)) {
            amount *= block.getValue(property);
         }
      }

      if (VINELIKE_BLOCKS.contains(block.getBlock())) {
         int vineCount = 0;

         for (BooleanProperty vineState : VINELIKE_STATES) {
            if (block.hasProperty(vineState) && (Boolean)block.getValue(vineState)) {
               vineCount++;
            }
         }

         amount += vineCount - 1;
      }

      int preferredSlot = player.getInventory().selected;
      ItemStack itemstack = player.getInventory().getItem(preferredSlot);
      int count = itemstack.getCount();
      if (itemstack.getItem() == required && count > 0) {
         int taken = Math.min(count, amount - amountFound);
         player.getInventory().setItem(preferredSlot, new ItemStack(itemstack.getItem(), count - taken));
         amountFound += taken;
      }

      for (int i = 0; i < player.getInventory().getContainerSize() && amountFound != amount; i++) {
         itemstack = player.getInventory().getItem(i);
         count = itemstack.getCount();
         if (itemstack.getItem() == required && count > 0) {
            int taken = Math.min(count, amount - amountFound);
            player.getInventory().setItem(i, new ItemStack(itemstack.getItem(), count - taken));
            amountFound += taken;
         }
      }

      if (needsTwo) {
         if (amountFound % 2 != 0) {
            player.getInventory().add(new ItemStack(required));
         }

         amountFound /= 2;
      }

      return amountFound;
   }

   public static ItemStack getRequiredItem(BlockState state) {
      ItemStack itemStack = new ItemStack(state.getBlock());
      Item item = itemStack.getItem();
      if (item == Items.FARMLAND || item == Items.DIRT_PATH) {
         itemStack = new ItemStack(Items.DIRT);
      }

      return itemStack;
   }

   public static void destroyBlock(Level world, BlockPos pos, float effectChance) {
      destroyBlock(world, pos, effectChance, stack -> Block.popResource(world, pos, stack));
   }

   public static void destroyBlock(Level world, BlockPos pos, float effectChance, Consumer<ItemStack> droppedItemCallback) {
      destroyBlockAs(world, pos, null, ItemStack.EMPTY, effectChance, droppedItemCallback);
   }

   public static void destroyBlockAs(
      Level level, BlockPos pos, @Nullable Player player, ItemStack usedTool, float effectChance, Consumer<ItemStack> droppedItemCallback
   ) {
      FluidState fluidState = level.getFluidState(pos);
      BlockState state = level.getBlockState(pos);
      if (level.random.nextFloat() < effectChance) {
         level.levelEvent(2001, pos, Block.getId(state));
      }

      BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
      if (player != null) {
         BreakEvent event = new BreakEvent(level, pos, state, player);
         NeoForge.EVENT_BUS.post(event);
         if (event.isCanceled()) {
            return;
         }

         usedTool.mineBlock(level, state, pos, player);
         player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
      }

      if (level instanceof ServerLevel serverLevel
         && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)
         && !level.restoringBlockSnapshots
         && (player == null || !player.isCreative())) {
         List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, blockEntity, player, usedTool);
         BlockDropsEvent event = new BlockDropsEvent(serverLevel, pos, state, blockEntity, new ArrayList(), player, usedTool);
         NeoForge.EVENT_BUS.post(event);
         if (!event.isCanceled() && event.getDroppedExperience() > 0) {
            state.getBlock().popExperience(serverLevel, pos, event.getDroppedExperience());
         }

         for (ItemStack itemStack : drops) {
            droppedItemCallback.accept(itemStack);
         }

         Registry<Enchantment> enchantmentRegistry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
         if (state.getBlock() instanceof IceBlock
            && usedTool.getEnchantmentLevel(enchantmentRegistry.getHolderOrThrow(Enchantments.SILK_TOUCH)) == 0
            && !level.dimensionType().ultraWarm()) {
            BlockState below = level.getBlockState(pos.below());
            if (below.blocksMotion() || below.liquid()) {
               fluidState = IceBlock.meltsInto().getFluidState();
            }
         }

         state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, false);
      }

      level.setBlockAndUpdate(pos, fluidState.createLegacyBlock());
   }

   public static boolean isSolidWall(BlockGetter reader, BlockPos fromPos, Direction toDirection) {
      return hasBlockSolidSide(reader.getBlockState(fromPos.relative(toDirection)), reader, fromPos.relative(toDirection), toDirection.getOpposite());
   }

   public static boolean noCollisionInSpace(BlockGetter reader, BlockPos pos) {
      return reader.getBlockState(pos).getCollisionShape(reader, pos).isEmpty();
   }

   private static void placeRailWithoutUpdate(Level world, BlockState state, BlockPos target) {
      LevelChunk chunk = world.getChunkAt(target);
      int idx = chunk.getSectionIndex(target.getY());
      LevelChunkSection chunksection = chunk.getSection(idx);
      if (chunksection == null) {
         chunksection = new LevelChunkSection(world.registryAccess().registryOrThrow(Registries.BIOME));
         chunk.getSections()[idx] = chunksection;
      }

      BlockState old = chunksection.setBlockState(
         SectionPos.sectionRelative(target.getX()), SectionPos.sectionRelative(target.getY()), SectionPos.sectionRelative(target.getZ()), state
      );
      chunk.setUnsaved(true);
      world.markAndNotifyBlock(target, chunk, old, state, 82, 512);
      world.setBlock(target, state, 82);
      world.neighborChanged(target, world.getBlockState(target.below()).getBlock(), target.below());
   }

   public static CompoundTag prepareBlockEntityData(Level level, BlockState blockState, BlockEntity blockEntity) {
      CompoundTag data = null;
      if (blockEntity == null) {
         return null;
      } else {
         RegistryAccess access = level.registryAccess();
         SafeNbtWriterRegistry.SafeNbtWriter writer = SafeNbtWriterRegistry.REGISTRY.get(blockEntity.getType());
         if (AllTags.AllBlockTags.SAFE_NBT.matches(blockState)) {
            data = blockEntity.saveWithFullMetadata(access);
         } else if (writer != null) {
            data = new CompoundTag();
            writer.writeSafe(blockEntity, data, access);
         } else if (blockEntity instanceof PartialSafeNBT safeNbtBE) {
            data = new CompoundTag();
            safeNbtBE.writeSafe(data, access);
         } else if (Mods.FRAMEDBLOCKS.contains(blockState.getBlock())) {
            data = FramedBlocksInSchematics.prepareBlockEntityData(blockState, blockEntity);
         }

         return NBTProcessors.process(blockState, blockEntity, data, true);
      }
   }

   public static void placeSchematicBlock(Level world, BlockState state, BlockPos target, ItemStack stack, @Nullable CompoundTag data) {
      Block block = state.getBlock();
      BlockEntity existingBlockEntity = world.getBlockEntity(target);
      boolean alreadyPlaced = false;
      SchematicStateFilterRegistry.StateFilter filter = SchematicStateFilterRegistry.REGISTRY.get(state);
      if (filter != null) {
         state = filter.filterStates(existingBlockEntity, state);
      } else if (block instanceof SchematicStateFilter schematicStateFilter) {
         state = schematicStateFilter.filterStates(existingBlockEntity, state);
      }

      if (state.hasProperty(BlockStateProperties.EXTENDED)) {
         state = (BlockState)state.setValue(BlockStateProperties.EXTENDED, Boolean.FALSE);
      }

      if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
         state = (BlockState)state.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE);
      }

      if (block == Blocks.COMPOSTER) {
         state = Blocks.COMPOSTER.defaultBlockState();
      } else if (block != Blocks.SEA_PICKLE && block instanceof SpecialPlantable specialPlantable) {
         alreadyPlaced = true;
         if (specialPlantable.canPlacePlantAtPosition(stack, world, target, null)) {
            specialPlantable.spawnPlantAtPosition(stack, world, target, null);
         }
      } else if (state.is(BlockTags.CAULDRONS)) {
         state = Blocks.CAULDRON.defaultBlockState();
      }

      if (world.dimensionType().ultraWarm() && state.getFluidState().is(FluidTags.WATER)) {
         int i = target.getX();
         int j = target.getY();
         int k = target.getZ();
         world.playSound(
            null, target, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F
         );

         for (int l = 0; l < 8; l++) {
            world.addParticle(ParticleTypes.LARGE_SMOKE, (double)i + Math.random(), (double)j + Math.random(), (double)k + Math.random(), 0.0, 0.0, 0.0);
         }

         Block.dropResources(state, world, target);
      } else {
         if (!alreadyPlaced) {
            if (state.getBlock() instanceof BaseRailBlock) {
               placeRailWithoutUpdate(world, state, target);
            } else if (AllBlocks.BELT.has(state)) {
               world.setBlock(target, state, 2);
            } else {
               world.setBlock(target, state, 18);
            }
         }

         if (data != null) {
            if (existingBlockEntity instanceof IMergeableBE mergeable) {
               BlockEntity loaded = BlockEntity.loadStatic(target, state, data, world.registryAccess());
               if (loaded != null && existingBlockEntity.getType().equals(loaded.getType())) {
                  mergeable.accept(loaded);
                  return;
               }
            }

            BlockEntity blockEntity = world.getBlockEntity(target);
            if (blockEntity != null) {
               data.putInt("x", target.getX());
               data.putInt("y", target.getY());
               data.putInt("z", target.getZ());
               if (blockEntity instanceof KineticBlockEntity kbe) {
                  kbe.warnOfMovement();
               }

               if (blockEntity instanceof IMultiBlockEntityContainer imbe && !imbe.isController()) {
                  data.put("Controller", NbtUtils.writeBlockPos(imbe.getController()));
               }

               blockEntity.loadWithComponents(data, world.registryAccess());
            }
         }

         try {
            state.getBlock().setPlacedBy(world, target, state, null, stack);
         } catch (Exception var13) {
         }
      }
   }

   public static double getBounceMultiplier(Block block) {
      if (block instanceof SlimeBlock) {
         return 0.8;
      } else {
         return block instanceof BedBlock ? 0.528 : 0.0;
      }
   }

   public static boolean hasBlockSolidSide(BlockState state, BlockGetter blockGetter, BlockPos pos, Direction dir) {
      return !state.is(BlockTags.LEAVES) && Block.isFaceFull(state.getCollisionShape(blockGetter, pos), dir);
   }

   public static boolean extinguishFire(Level world, @Nullable Player player, BlockPos pos, Direction dir) {
      pos = pos.relative(dir);
      if (world.getBlockState(pos).getBlock() == Blocks.FIRE) {
         world.levelEvent(player, 1009, pos, 0);
         world.removeBlock(pos, false);
         return true;
      } else {
         return false;
      }
   }

   public static BlockState copyProperties(BlockState fromState, BlockState toState) {
      for (Property<?> property : fromState.getProperties()) {
         toState = copyProperty(property, fromState, toState);
      }

      return toState;
   }

   public static <T extends Comparable<T>> BlockState copyProperty(Property<T> property, BlockState fromState, BlockState toState) {
      return fromState.hasProperty(property) && toState.hasProperty(property) ? (BlockState)toState.setValue(property, fromState.getValue(property)) : toState;
   }

   public static boolean isNotUnheated(BlockState state) {
      if (state.is(BlockTags.CAMPFIRES) && state.hasProperty(CampfireBlock.LIT)) {
         return (Boolean)state.getValue(CampfireBlock.LIT);
      } else {
         return state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL) ? state.getValue(BlazeBurnerBlock.HEAT_LEVEL) != BlazeBurnerBlock.HeatLevel.NONE : true;
      }
   }

   public static InteractionResult invokeUse(BlockState state, Level level, Player player, InteractionHand hand, BlockHitResult ray) {
      ItemInteractionResult iteminteractionresult = state.useItemOn(player.getItemInHand(hand), level, player, hand, ray);
      if (iteminteractionresult.consumesAction()) {
         return iteminteractionresult.result();
      } else {
         if (iteminteractionresult == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && hand == InteractionHand.MAIN_HAND) {
            InteractionResult interactionresult = state.useWithoutItem(level, player, ray);
            if (interactionresult.consumesAction()) {
               return interactionresult;
            }
         }

         return InteractionResult.PASS;
      }
   }
}
