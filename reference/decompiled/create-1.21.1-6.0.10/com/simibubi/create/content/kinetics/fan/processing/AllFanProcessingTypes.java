package com.simibubi.create.content.kinetics.fan.processing;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.LitBlazeBurnerBlock;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector3f;

public class AllFanProcessingTypes {
   public static final AllFanProcessingTypes.BlastingType BLASTING = register("blasting", new AllFanProcessingTypes.BlastingType());
   public static final AllFanProcessingTypes.HauntingType HAUNTING = register("haunting", new AllFanProcessingTypes.HauntingType());
   public static final AllFanProcessingTypes.SmokingType SMOKING = register("smoking", new AllFanProcessingTypes.SmokingType());
   public static final AllFanProcessingTypes.SplashingType SPLASHING = register("splashing", new AllFanProcessingTypes.SplashingType());
   private static final Map<String, FanProcessingType> LEGACY_NAME_MAP;

   private static <T extends FanProcessingType> T register(String name, T type) {
      return (T)Registry.register(CreateBuiltInRegistries.FAN_PROCESSING_TYPE, Create.asResource(name), type);
   }

   @Internal
   public static void init() {
   }

   @Nullable
   public static FanProcessingType ofLegacyName(String name) {
      return LEGACY_NAME_MAP.get(name);
   }

   @Nullable
   public static FanProcessingType parseLegacy(String str) {
      FanProcessingType type = ofLegacyName(str);
      return type != null ? type : FanProcessingType.parse(str);
   }

   static {
      Object2ReferenceOpenHashMap<String, FanProcessingType> map = new Object2ReferenceOpenHashMap();
      map.put("BLASTING", BLASTING);
      map.put("HAUNTING", HAUNTING);
      map.put("SMOKING", SMOKING);
      map.put("SPLASHING", SPLASHING);
      map.trim();
      LEGACY_NAME_MAP = map;
   }

   public static class BlastingType implements FanProcessingType {
      @Override
      public boolean isValidAt(Level level, BlockPos pos) {
         FluidState fluidState = level.getFluidState(pos);
         if (AllTags.AllFluidTags.FAN_PROCESSING_CATALYSTS_BLASTING.matches(fluidState)) {
            return true;
         } else {
            BlockState blockState = level.getBlockState(pos);
            return !AllTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_BLASTING.matches(blockState)
               ? false
               : !blockState.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)
                  || ((BlazeBurnerBlock.HeatLevel)blockState.getValue(BlazeBurnerBlock.HEAT_LEVEL)).isAtLeast(BlazeBurnerBlock.HeatLevel.FADING);
         }
      }

      @Override
      public int getPriority() {
         return 100;
      }

      @Override
      public boolean canProcess(ItemStack stack, Level level) {
         Optional<RecipeHolder<SmeltingRecipe>> smeltingRecipe = level.getRecipeManager()
            .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
            .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
         if (smeltingRecipe.isPresent()) {
            return true;
         } else {
            Optional<RecipeHolder<BlastingRecipe>> blastingRecipe = level.getRecipeManager()
               .getRecipeFor(RecipeType.BLASTING, new SingleRecipeInput(stack), level)
               .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
            return blastingRecipe.isPresent() ? true : !stack.has(DataComponents.FIRE_RESISTANT);
         }
      }

      @Nullable
      @Override
      public List<ItemStack> process(ItemStack stack, Level level) {
         Optional<RecipeHolder<SmokingRecipe>> smokingRecipe = level.getRecipeManager()
            .getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(stack), level)
            .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
         Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> smeltingRecipe = level.getRecipeManager()
            .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
            .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
         if (smeltingRecipe.isEmpty()) {
            smeltingRecipe = level.getRecipeManager()
               .getRecipeFor(RecipeType.BLASTING, new SingleRecipeInput(stack), level)
               .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
         }

         if (smeltingRecipe.isPresent()) {
            RegistryAccess registryAccess = level.registryAccess();
            if (smokingRecipe.isEmpty()
               || !ItemStack.isSameItem(
                  ((SmokingRecipe)smokingRecipe.get().value()).getResultItem(registryAccess),
                  ((AbstractCookingRecipe)smeltingRecipe.get().value()).getResultItem(registryAccess)
               )) {
               return RecipeApplier.applyRecipeOn(level, stack, smeltingRecipe.get().value(), false);
            }
         }

         return Collections.emptyList();
      }

      @Override
      public void spawnProcessingParticles(Level level, Vec3 pos) {
         if (level.random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 0.25, pos.z, 0.0, 0.0625, 0.0);
         }
      }

      @Override
      public void morphAirFlow(FanProcessingType.AirFlowParticleAccess particleAccess, RandomSource random) {
         particleAccess.setColor(Color.mixColors(16729088, 16746581, random.nextFloat()));
         particleAccess.setAlpha(0.5F);
         if (random.nextFloat() < 0.03125F) {
            particleAccess.spawnExtraParticle(ParticleTypes.FLAME, 0.25F);
         }

         if (random.nextFloat() < 0.0625F) {
            particleAccess.spawnExtraParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LAVA.defaultBlockState()), 0.25F);
         }
      }

      @Override
      public void affectEntity(Entity entity, Level level) {
         if (!level.isClientSide) {
            if (!entity.fireImmune()) {
               entity.igniteForSeconds(10.0F);
               entity.hurt(CreateDamageSources.fanLava(level), 4.0F);
            }
         }
      }
   }

   public static class HauntingType implements FanProcessingType {
      @Override
      public boolean isValidAt(Level level, BlockPos pos) {
         FluidState fluidState = level.getFluidState(pos);
         if (AllTags.AllFluidTags.FAN_PROCESSING_CATALYSTS_HAUNTING.matches(fluidState)) {
            return true;
         } else {
            BlockState blockState = level.getBlockState(pos);
            if (AllTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_HAUNTING.matches(blockState)) {
               return blockState.is(BlockTags.CAMPFIRES) && blockState.hasProperty(CampfireBlock.LIT) && !blockState.getValue(CampfireBlock.LIT)
                  ? false
                  : !blockState.hasProperty(LitBlazeBurnerBlock.FLAME_TYPE)
                     || blockState.getValue(LitBlazeBurnerBlock.FLAME_TYPE) == LitBlazeBurnerBlock.FlameType.SOUL;
            } else {
               return false;
            }
         }
      }

      @Override
      public int getPriority() {
         return 300;
      }

      @Override
      public boolean canProcess(ItemStack stack, Level level) {
         return AllRecipeTypes.HAUNTING.find(new SingleRecipeInput(stack), level).isPresent();
      }

      @Nullable
      @Override
      public List<ItemStack> process(ItemStack stack, Level level) {
         return AllRecipeTypes.HAUNTING
            .find(new SingleRecipeInput(stack), level)
            .<Recipe>map(RecipeHolder::value)
            .map(r -> RecipeApplier.applyRecipeOn(level, stack, (Recipe<?>)r, true))
            .orElse(null);
      }

      @Override
      public void spawnProcessingParticles(Level level, Vec3 pos) {
         if (level.random.nextInt(8) == 0) {
            pos = pos.add(VecHelper.offsetRandomly(Vec3.ZERO, level.random, 1.0F).multiply(1.0, 0.05F, 1.0).normalize().scale(0.15F));
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.45F, pos.z, 0.0, 0.0, 0.0);
            if (level.random.nextInt(2) == 0) {
               level.addParticle(ParticleTypes.SMOKE, pos.x, pos.y + 0.25, pos.z, 0.0, 0.0, 0.0);
            }
         }
      }

      @Override
      public void morphAirFlow(FanProcessingType.AirFlowParticleAccess particleAccess, RandomSource random) {
         particleAccess.setColor(Color.mixColors(0, 1205608, random.nextFloat()));
         particleAccess.setAlpha(1.0F);
         if (random.nextFloat() < 0.0078125F) {
            particleAccess.spawnExtraParticle(ParticleTypes.SOUL_FIRE_FLAME, 0.125F);
         }

         if (random.nextFloat() < 0.03125F) {
            particleAccess.spawnExtraParticle(ParticleTypes.SMOKE, 0.125F);
         }
      }

      @Override
      public void affectEntity(Entity entity, Level level) {
         if (level.isClientSide) {
            if (entity instanceof Horse) {
               Vec3 p = entity.getPosition(0.0F);
               Vec3 v = p.add(0.0, 0.5, 0.0).add(VecHelper.offsetRandomly(Vec3.ZERO, level.random, 1.0F).multiply(1.0, 0.2F, 1.0).normalize().scale(1.0));
               level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, v.x, v.y, v.z, 0.0, 0.1F, 0.0);
               if (level.random.nextInt(3) == 0) {
                  level.addParticle(
                     ParticleTypes.LARGE_SMOKE,
                     p.x,
                     p.y + 0.5,
                     p.z,
                     (double)((level.random.nextFloat() - 0.5F) * 0.5F),
                     0.1F,
                     (double)((level.random.nextFloat() - 0.5F) * 0.5F)
                  );
               }
            }
         } else {
            if (entity instanceof LivingEntity livingEntity) {
               livingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
               livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
            }

            if (entity instanceof Horse horse) {
               int progress = horse.getPersistentData().getInt("CreateHaunting");
               if (progress < 100) {
                  if (progress % 10 == 0) {
                     level.playSound(
                        null, entity.blockPosition(), (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.NEUTRAL, 1.0F, 1.5F * (float)progress / 100.0F
                     );
                  }

                  horse.getPersistentData().putInt("CreateHaunting", progress + 1);
                  return;
               }

               level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.NEUTRAL, 1.25F, 0.65F);
               SkeletonHorse skeletonHorse = (SkeletonHorse)EntityType.SKELETON_HORSE.create(level);
               CompoundTag serializeNBT = horse.saveWithoutId(new CompoundTag());
               serializeNBT.remove("UUID");
               if (!horse.getBodyArmorItem().isEmpty()) {
                  horse.spawnAtLocation(horse.getBodyArmorItem());
               }

               skeletonHorse.deserializeNBT(entity.registryAccess(), serializeNBT);
               skeletonHorse.setPos(horse.getPosition(0.0F));
               level.addFreshEntity(skeletonHorse);
               horse.discard();
            }
         }
      }
   }

   public static class SmokingType implements FanProcessingType {
      @Override
      public boolean isValidAt(Level level, BlockPos pos) {
         FluidState fluidState = level.getFluidState(pos);
         if (AllTags.AllFluidTags.FAN_PROCESSING_CATALYSTS_SMOKING.matches(fluidState)) {
            return true;
         } else {
            BlockState blockState = level.getBlockState(pos);
            if (!AllTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_SMOKING.matches(blockState)) {
               return false;
            } else if (blockState.is(BlockTags.CAMPFIRES) && blockState.hasProperty(CampfireBlock.LIT) && !(Boolean)blockState.getValue(CampfireBlock.LIT)) {
               return false;
            } else {
               return blockState.hasProperty(LitBlazeBurnerBlock.FLAME_TYPE)
                     && blockState.getValue(LitBlazeBurnerBlock.FLAME_TYPE) != LitBlazeBurnerBlock.FlameType.REGULAR
                  ? false
                  : !blockState.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)
                     || blockState.getValue(BlazeBurnerBlock.HEAT_LEVEL) == BlazeBurnerBlock.HeatLevel.SMOULDERING;
            }
         }
      }

      @Override
      public int getPriority() {
         return 200;
      }

      @Override
      public boolean canProcess(ItemStack stack, Level level) {
         return level.getRecipeManager()
            .getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(stack), level)
            .filter(AllRecipeTypes.CAN_BE_AUTOMATED)
            .isPresent();
      }

      @Nullable
      @Override
      public List<ItemStack> process(ItemStack stack, Level level) {
         return level.getRecipeManager()
            .getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(stack), level)
            .filter(AllRecipeTypes.CAN_BE_AUTOMATED)
            .<SmokingRecipe>map(RecipeHolder::value)
            .map(r -> RecipeApplier.applyRecipeOn(level, stack, r, false))
            .orElse(null);
      }

      @Override
      public void spawnProcessingParticles(Level level, Vec3 pos) {
         if (level.random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.POOF, pos.x, pos.y + 0.25, pos.z, 0.0, 0.0625, 0.0);
         }
      }

      @Override
      public void morphAirFlow(FanProcessingType.AirFlowParticleAccess particleAccess, RandomSource random) {
         particleAccess.setColor(Color.mixColors(0, 5592405, random.nextFloat()));
         particleAccess.setAlpha(1.0F);
         if (random.nextFloat() < 0.03125F) {
            particleAccess.spawnExtraParticle(ParticleTypes.SMOKE, 0.125F);
         }

         if (random.nextFloat() < 0.03125F) {
            particleAccess.spawnExtraParticle(ParticleTypes.LARGE_SMOKE, 0.125F);
         }
      }

      @Override
      public void affectEntity(Entity entity, Level level) {
         if (!level.isClientSide) {
            if (!entity.fireImmune()) {
               entity.igniteForSeconds(2.0F);
               entity.hurt(CreateDamageSources.fanFire(level), 2.0F);
            }
         }
      }
   }

   public static class SplashingType implements FanProcessingType {
      @Override
      public boolean isValidAt(Level level, BlockPos pos) {
         FluidState fluidState = level.getFluidState(pos);
         if (AllTags.AllFluidTags.FAN_PROCESSING_CATALYSTS_SPLASHING.matches(fluidState)) {
            return true;
         } else {
            BlockState blockState = level.getBlockState(pos);
            return AllTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_SPLASHING.matches(blockState);
         }
      }

      @Override
      public int getPriority() {
         return 400;
      }

      @Override
      public boolean canProcess(ItemStack stack, Level level) {
         return AllRecipeTypes.SPLASHING.find(new SingleRecipeInput(stack), level).isPresent();
      }

      @Nullable
      @Override
      public List<ItemStack> process(ItemStack stack, Level level) {
         Optional<RecipeHolder<Recipe<SingleRecipeInput>>> recipe = AllRecipeTypes.SPLASHING.find(new SingleRecipeInput(stack), level);
         return AllRecipeTypes.SPLASHING
            .find(new SingleRecipeInput(stack), level)
            .<Recipe>map(RecipeHolder::value)
            .map(r -> RecipeApplier.applyRecipeOn(level, stack, (Recipe<?>)r, true))
            .orElse(null);
      }

      @Override
      public void spawnProcessingParticles(Level level, Vec3 pos) {
         if (level.random.nextInt(8) == 0) {
            Vector3f color = new Color(22015).asVectorF();
            level.addParticle(
               new DustParticleOptions(color, 1.0F),
               pos.x + (double)((level.random.nextFloat() - 0.5F) * 0.5F),
               pos.y + 0.5,
               pos.z + (double)((level.random.nextFloat() - 0.5F) * 0.5F),
               0.0,
               0.125,
               0.0
            );
            level.addParticle(
               ParticleTypes.SPIT,
               pos.x + (double)((level.random.nextFloat() - 0.5F) * 0.5F),
               pos.y + 0.5,
               pos.z + (double)((level.random.nextFloat() - 0.5F) * 0.5F),
               0.0,
               0.125,
               0.0
            );
         }
      }

      @Override
      public void morphAirFlow(FanProcessingType.AirFlowParticleAccess particleAccess, RandomSource random) {
         particleAccess.setColor(Color.mixColors(4495871, 2258943, random.nextFloat()));
         particleAccess.setAlpha(1.0F);
         if (random.nextFloat() < 0.03125F) {
            particleAccess.spawnExtraParticle(ParticleTypes.BUBBLE, 0.125F);
         }

         if (random.nextFloat() < 0.03125F) {
            particleAccess.spawnExtraParticle(ParticleTypes.BUBBLE_POP, 0.125F);
         }
      }

      @Override
      public void affectEntity(Entity entity, Level level) {
         if (!level.isClientSide) {
            if (entity instanceof EnderMan || entity.getType() == EntityType.SNOW_GOLEM || entity.getType() == EntityType.BLAZE) {
               entity.hurt(entity.damageSources().drown(), 2.0F);
            }

            if (entity.isOnFire()) {
               entity.clearFire();
               level.playSound(
                  null,
                  entity.blockPosition(),
                  SoundEvents.GENERIC_EXTINGUISH_FIRE,
                  SoundSource.NEUTRAL,
                  0.7F,
                  1.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.4F
               );
            }
         }
      }
   }
}
