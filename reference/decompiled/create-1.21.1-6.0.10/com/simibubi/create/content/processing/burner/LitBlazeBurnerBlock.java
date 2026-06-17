package com.simibubi.create.content.processing.burner;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.ItemAbility;

public class LitBlazeBurnerBlock extends Block implements IWrenchable {
   public static final ItemAbility EXTINGUISH_FLAME_ACTION = ItemAbility.get(Create.asResource("extinguish_flame").toString());
   public static final EnumProperty<LitBlazeBurnerBlock.FlameType> FLAME_TYPE = EnumProperty.create("flame_type", LitBlazeBurnerBlock.FlameType.class);

   public LitBlazeBurnerBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(FLAME_TYPE, LitBlazeBurnerBlock.FlameType.REGULAR));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{FLAME_TYPE});
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (stack.getItem() instanceof ShovelItem || stack.getItem().canPerformAction(stack, EXTINGUISH_FLAME_ACTION)) {
         level.playSound(player, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.5F, 2.0F);
         if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
         } else {
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            level.setBlockAndUpdate(pos, AllBlocks.BLAZE_BURNER.getDefaultState());
            return ItemInteractionResult.SUCCESS;
         }
      } else if (state.getValue(FLAME_TYPE) == LitBlazeBurnerBlock.FlameType.REGULAR && stack.is(ItemTags.SOUL_FIRE_BASE_BLOCKS)) {
         level.playSound(player, pos, SoundEvents.SOUL_SAND_PLACE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 0.8F);
         if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
         } else {
            level.setBlockAndUpdate(pos, (BlockState)this.defaultBlockState().setValue(FLAME_TYPE, LitBlazeBurnerBlock.FlameType.SOUL));
            return ItemInteractionResult.SUCCESS;
         }
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
      return ((BlazeBurnerBlock)AllBlocks.BLAZE_BURNER.get()).getShape(state, reader, pos, context);
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
      return AllItems.EMPTY_BLAZE_BURNER.asStack();
   }

   @OnlyIn(Dist.CLIENT)
   public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
      world.addAlwaysVisibleParticle(
         ParticleTypes.LARGE_SMOKE,
         true,
         (double)pos.getX() + 0.5 + random.nextDouble() / 3.0 * (double)(random.nextBoolean() ? 1 : -1),
         (double)pos.getY() + random.nextDouble() + random.nextDouble(),
         (double)pos.getZ() + 0.5 + random.nextDouble() / 3.0 * (double)(random.nextBoolean() ? 1 : -1),
         0.0,
         0.07,
         0.0
      );
      if (random.nextInt(10) == 0) {
         world.playLocalSound(
            (double)((float)pos.getX() + 0.5F),
            (double)((float)pos.getY() + 0.5F),
            (double)((float)pos.getZ() + 0.5F),
            SoundEvents.CAMPFIRE_CRACKLE,
            SoundSource.BLOCKS,
            0.25F + random.nextFloat() * 0.25F,
            random.nextFloat() * 0.7F + 0.6F,
            false
         );
      }

      if (state.getValue(FLAME_TYPE) == LitBlazeBurnerBlock.FlameType.SOUL) {
         if (random.nextInt(8) == 0) {
            world.addParticle(
               ParticleTypes.SOUL,
               (double)((float)pos.getX() + 0.5F) + random.nextDouble() / 4.0 * (double)(random.nextBoolean() ? 1 : -1),
               (double)((float)pos.getY() + 0.3F) + random.nextDouble() / 2.0,
               (double)((float)pos.getZ() + 0.5F) + random.nextDouble() / 4.0 * (double)(random.nextBoolean() ? 1 : -1),
               0.0,
               random.nextDouble() * 0.04 + 0.04,
               0.0
            );
         }
      } else {
         if (random.nextInt(5) == 0) {
            for (int i = 0; i < random.nextInt(1) + 1; i++) {
               world.addParticle(
                  ParticleTypes.LAVA,
                  (double)((float)pos.getX() + 0.5F),
                  (double)((float)pos.getY() + 0.5F),
                  (double)((float)pos.getZ() + 0.5F),
                  (double)(random.nextFloat() / 2.0F),
                  5.0E-5,
                  (double)(random.nextFloat() / 2.0F)
               );
            }
         }
      }
   }

   public boolean hasAnalogOutputSignal(BlockState p_149740_1_) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState state, Level p_180641_2_, BlockPos p_180641_3_) {
      return state.getValue(FLAME_TYPE) == LitBlazeBurnerBlock.FlameType.REGULAR ? 1 : 2;
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
      return ((BlazeBurnerBlock)AllBlocks.BLAZE_BURNER.get()).getCollisionShape(state, reader, pos, context);
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   public static int getLight(BlockState state) {
      return state.getValue(FLAME_TYPE) == LitBlazeBurnerBlock.FlameType.SOUL ? 9 : 12;
   }

   public static enum FlameType implements StringRepresentable {
      REGULAR,
      SOUL;

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}
