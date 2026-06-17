package dev.eriksonn.aeronautics.content.blocks.levitite;

import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.CrystalPropagationContext;
import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeBlendHelper;
import dev.eriksonn.aeronautics.index.AeroAdvancements;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.eriksonn.aeronautics.index.AeroLevititeBlendPropagationContexts;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import dev.eriksonn.aeronautics.index.AeroTags;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class LevititeCrystalPropagationContext implements CrystalPropagationContext {
   @Override
   public void onCrystallizationInitialize(Level level, BlockPos pos, boolean isDormant) {
      level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 1.5F);
      if (!isDormant) {
         LevititeBlendHelper.spawnParticles(level, pos, ParticleTypes.FLAME, 20);
      }

      LevititeBlendHelper.spawnParticles(level, pos, ParticleTypes.SMOKE, 15);
   }

   @Override
   public void onCrystallize(Level level, BlockPos pos) {
      this.onDefaultCrystallize(level, pos);
      if (!level.isClientSide) {
         AeroSoundEvents.LEVITITE_BLEND_CRYSTALLIZE.play(level, null, pos, 1.0F, 1.0F);
         LevititeBlendHelper.spawnParticles(level, pos, ParticleTypes.FLAME, 30);
         AeroAdvancements.UNIDENTIFIED_FLOATING_OBJECT.awardToNearby(pos, level);
      }
   }

   @Override
   public void onCrystallizationFail(Level level, BlockPos pos, int attempts, boolean isDormant) {
      LevititeBlendHelper.spawnParticles(level, pos, ParticleTypes.SMOKE, 15);
   }

   @Override
   public BlockState getCrystalBlockState(Level level, BlockPos pos) {
      return AeroBlocks.LEVITITE.getDefaultState();
   }

   @Override
   public boolean canSpreadTo(FluidState state) {
      return state.is(LevititeBlendHelper.getFluid());
   }

   public static int[] getWeights(Level level, BlockPos pos) {
      int[] weights = new int[2];

      for (Direction dir : Direction.values()) {
         if (level.getBlockState(pos.relative(dir)).is(AeroTags.BlockTags.LEVITITE_ADJACENT_CATALYZER)) {
            weights[0]++;
         }

         if (level.getBlockState(pos.relative(dir)).is(AeroTags.BlockTags.LEVITITE_ADJACENT_SOUL_CATALYZER)) {
            weights[1]++;
         }
      }

      return weights;
   }

   public static CrystalPropagationContext getRandomContext(CrystalPropagationContext self, Level level, BlockPos pos) {
      int[] weights = getWeights(level, pos);
      int sum = Arrays.stream(weights).sum();
      if (sum == 0) {
         return self;
      } else {
         return weights[0] <= 0 || weights[1] != 0 && level.getRandom().nextInt(sum) >= weights[0]
            ? (CrystalPropagationContext)AeroLevititeBlendPropagationContexts.SOUL_CONTEXT.get()
            : (CrystalPropagationContext)AeroLevititeBlendPropagationContexts.STANDARD_CONTEXT.get();
      }
   }

   @Override
   public CrystalPropagationContext getContextForSpread(Level level, BlockPos pos) {
      return getRandomContext(this, level, pos);
   }

   @Override
   public TagKey<Block> getCatalyzerTag() {
      return AeroTags.BlockTags.LEVITITE_CATALYZER;
   }
}
