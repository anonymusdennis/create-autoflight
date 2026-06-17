package com.simibubi.create.content.processing.burner;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.data.datamaps.BlazeBurnerFuel;
import com.simibubi.create.api.registry.CreateDataMaps;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class BlazeBurnerBlockEntity extends SmartBlockEntity {
   public static final int MAX_HEAT_CAPACITY = 10000;
   public static final int INSERTION_THRESHOLD = 500;
   public LerpedFloat headAnimation;
   public boolean stockKeeper;
   public boolean isCreative;
   public boolean goggles;
   public boolean hat;
   protected BlazeBurnerBlockEntity.FuelType activeFuel = BlazeBurnerBlockEntity.FuelType.NONE;
   protected int remainingBurnTime = 0;
   protected LerpedFloat headAngle;

   public BlazeBurnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.headAnimation = LerpedFloat.linear();
      this.headAngle = LerpedFloat.angular();
      this.isCreative = false;
      this.goggles = false;
      this.stockKeeper = false;
      this.headAngle
         .startWithValue((double)((AngleHelper.horizontalAngle(state.getOptionalValue(BlazeBurnerBlock.FACING).orElse(Direction.SOUTH)) + 180.0F) % 360.0F));
   }

   public BlazeBurnerBlockEntity.FuelType getActiveFuel() {
      return this.activeFuel;
   }

   public int getRemainingBurnTime() {
      return this.remainingBurnTime;
   }

   public boolean isCreative() {
      return this.isCreative;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level.isClientSide) {
         if (this.shouldTickAnimation()) {
            this.tickAnimation();
         }

         if (!this.isVirtual()) {
            this.spawnParticles(this.getHeatLevelFromBlock(), 1.0);
         }
      } else if (!this.isCreative) {
         if (this.remainingBurnTime > 0) {
            this.remainingBurnTime--;
         }

         if (this.activeFuel == BlazeBurnerBlockEntity.FuelType.NORMAL) {
            this.updateBlockState();
         }

         if (this.remainingBurnTime <= 0) {
            if (this.activeFuel == BlazeBurnerBlockEntity.FuelType.SPECIAL) {
               this.activeFuel = BlazeBurnerBlockEntity.FuelType.NORMAL;
               this.remainingBurnTime = 5000;
            } else {
               this.activeFuel = BlazeBurnerBlockEntity.FuelType.NONE;
            }

            this.updateBlockState();
         }
      }
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      this.stockKeeper = getStockTicker(this.level, this.worldPosition) != null;
   }

   @Nullable
   public static StockTickerBlockEntity getStockTicker(LevelAccessor level, BlockPos pos) {
      for (Direction direction : Iterate.horizontalDirections) {
         if (level instanceof Level l && !l.isLoaded(pos)) {
            return null;
         }

         BlockState blockState = level.getBlockState(pos.relative(direction));
         if (AllBlocks.STOCK_TICKER.has(blockState)) {
            BlockEntity var8 = level.getBlockEntity(pos.relative(direction));
            if (var8 instanceof StockTickerBlockEntity) {
               return (StockTickerBlockEntity)var8;
            }
         }
      }

      return null;
   }

   @OnlyIn(Dist.CLIENT)
   private boolean shouldTickAnimation() {
      return !VisualizationManager.supportsVisualization(this.level);
   }

   @OnlyIn(Dist.CLIENT)
   void tickAnimation() {
      boolean active = this.getHeatLevelFromBlock().isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) && this.isValidBlockAbove();
      if (!active) {
         float target = 0.0F;
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null && !player.isInvisible()) {
            double x;
            double z;
            if (this.isVirtual()) {
               x = -4.0;
               z = -10.0;
            } else {
               x = player.getX();
               z = player.getZ();
            }

            double dx = x - ((double)this.getBlockPos().getX() + 0.5);
            double dz = z - ((double)this.getBlockPos().getZ() + 0.5);
            target = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90.0F;
         }

         target = this.headAngle.getValue() + AngleHelper.getShortestAngleDiff((double)this.headAngle.getValue(), (double)target);
         this.headAngle.chase((double)target, 0.25, Chaser.exp(5.0));
         this.headAngle.tickChaser();
      } else {
         this.headAngle
            .chase(
               (double)((AngleHelper.horizontalAngle(this.getBlockState().getOptionalValue(BlazeBurnerBlock.FACING).orElse(Direction.SOUTH)) + 180.0F) % 360.0F),
               0.125,
               Chaser.EXP
            );
         this.headAngle.tickChaser();
      }

      this.headAnimation.chase(active ? 1.0 : 0.0, 0.25, Chaser.exp(0.25));
      this.headAnimation.tickChaser();
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      if (!this.isCreative) {
         compound.putInt("fuelLevel", this.activeFuel.ordinal());
         compound.putInt("burnTimeRemaining", this.remainingBurnTime);
      } else {
         compound.putBoolean("isCreative", true);
      }

      if (this.goggles) {
         compound.putBoolean("Goggles", true);
      }

      if (this.hat) {
         compound.putBoolean("TrainHat", true);
      }

      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.activeFuel = BlazeBurnerBlockEntity.FuelType.values()[compound.getInt("fuelLevel")];
      this.remainingBurnTime = compound.getInt("burnTimeRemaining");
      this.isCreative = compound.getBoolean("isCreative");
      this.goggles = compound.contains("Goggles");
      this.hat = compound.contains("TrainHat");
      super.read(compound, registries, clientPacket);
   }

   public BlazeBurnerBlock.HeatLevel getHeatLevelFromBlock() {
      return BlazeBurnerBlock.getHeatLevelOf(this.getBlockState());
   }

   public BlazeBurnerBlock.HeatLevel getHeatLevelForRender() {
      BlazeBurnerBlock.HeatLevel heatLevel = this.getHeatLevelFromBlock();
      return !heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) && this.stockKeeper ? BlazeBurnerBlock.HeatLevel.FADING : heatLevel;
   }

   public void updateBlockState() {
      this.setBlockHeat(this.getHeatLevel());
   }

   protected void setBlockHeat(BlazeBurnerBlock.HeatLevel heat) {
      BlazeBurnerBlock.HeatLevel inBlockState = this.getHeatLevelFromBlock();
      if (inBlockState != heat) {
         this.level.setBlockAndUpdate(this.worldPosition, (BlockState)this.getBlockState().setValue(BlazeBurnerBlock.HEAT_LEVEL, heat));
         this.notifyUpdate();
      }
   }

   protected boolean tryUpdateFuel(ItemStack itemStack, boolean forceOverflow, boolean simulate) {
      if (this.isCreative) {
         return false;
      } else {
         BlazeBurnerBlockEntity.FuelType newFuel = BlazeBurnerBlockEntity.FuelType.NONE;
         Holder<Item> holder = itemStack.getItem().builtInRegistryHolder();
         BlazeBurnerFuel superheatedFuel = (BlazeBurnerFuel)holder.getData(CreateDataMaps.SUPERHEATED_BLAZE_BURNER_FUELS);
         BlazeBurnerFuel normalFuel = (BlazeBurnerFuel)holder.getData(CreateDataMaps.REGULAR_BLAZE_BURNER_FUELS);
         int newBurnTime;
         if (superheatedFuel != null) {
            newBurnTime = superheatedFuel.burnTime();
            newFuel = BlazeBurnerBlockEntity.FuelType.SPECIAL;
         } else if (normalFuel != null) {
            newBurnTime = normalFuel.burnTime();
            newFuel = BlazeBurnerBlockEntity.FuelType.NORMAL;
         } else if (AllTags.AllItemTags.BLAZE_BURNER_FUEL_SPECIAL.matches(itemStack)) {
            newBurnTime = 3200;
            newFuel = BlazeBurnerBlockEntity.FuelType.SPECIAL;
         } else {
            newBurnTime = itemStack.getBurnTime(null);
            if (newBurnTime > 0) {
               newFuel = BlazeBurnerBlockEntity.FuelType.NORMAL;
            } else if (AllTags.AllItemTags.BLAZE_BURNER_FUEL_REGULAR.matches(itemStack)) {
               newBurnTime = 1600;
               newFuel = BlazeBurnerBlockEntity.FuelType.NORMAL;
            }
         }

         if (newFuel == BlazeBurnerBlockEntity.FuelType.NONE) {
            return false;
         } else if (newFuel.ordinal() < this.activeFuel.ordinal()) {
            return false;
         } else {
            if (newFuel == this.activeFuel) {
               if (this.remainingBurnTime <= 500) {
                  newBurnTime += this.remainingBurnTime;
               } else {
                  if (!forceOverflow || newFuel != BlazeBurnerBlockEntity.FuelType.NORMAL) {
                     return false;
                  }

                  if (this.remainingBurnTime < 10000) {
                     newBurnTime = Math.min(this.remainingBurnTime + newBurnTime, 10000);
                  } else {
                     newBurnTime = this.remainingBurnTime;
                  }
               }
            }

            if (simulate) {
               return true;
            } else {
               this.activeFuel = newFuel;
               this.remainingBurnTime = newBurnTime;
               if (this.level.isClientSide) {
                  this.spawnParticleBurst(this.activeFuel == BlazeBurnerBlockEntity.FuelType.SPECIAL);
                  return true;
               } else {
                  BlazeBurnerBlock.HeatLevel prev = this.getHeatLevelFromBlock();
                  this.playSound();
                  this.updateBlockState();
                  if (prev != this.getHeatLevelFromBlock()) {
                     this.level
                        .playSound(
                           null,
                           this.worldPosition,
                           SoundEvents.BLAZE_AMBIENT,
                           SoundSource.BLOCKS,
                           0.125F + this.level.random.nextFloat() * 0.125F,
                           1.15F - this.level.random.nextFloat() * 0.25F
                        );
                  }

                  return true;
               }
            }
         }
      }
   }

   protected void applyCreativeFuel() {
      this.activeFuel = BlazeBurnerBlockEntity.FuelType.NONE;
      this.remainingBurnTime = 0;
      this.isCreative = true;
      BlazeBurnerBlock.HeatLevel next = this.getHeatLevelFromBlock().nextActiveLevel();
      if (this.level.isClientSide) {
         this.spawnParticleBurst(next.isAtLeast(BlazeBurnerBlock.HeatLevel.SEETHING));
      } else {
         this.playSound();
         if (next == BlazeBurnerBlock.HeatLevel.FADING) {
            next = next.nextActiveLevel();
         }

         this.setBlockHeat(next);
      }
   }

   public boolean isCreativeFuel(ItemStack stack) {
      return AllItems.CREATIVE_BLAZE_CAKE.isIn(stack);
   }

   public boolean isValidBlockAbove() {
      if (this.isVirtual()) {
         return false;
      } else {
         BlockState blockState = this.level.getBlockState(this.worldPosition.above());
         return BasinBlock.isBasin(this.level, this.worldPosition.above()) || blockState.getBlock() instanceof FluidTankBlock;
      }
   }

   protected void playSound() {
      this.level
         .playSound(
            null,
            this.worldPosition,
            SoundEvents.BLAZE_SHOOT,
            SoundSource.BLOCKS,
            0.125F + this.level.random.nextFloat() * 0.125F,
            0.75F - this.level.random.nextFloat() * 0.25F
         );
   }

   protected BlazeBurnerBlock.HeatLevel getHeatLevel() {
      BlazeBurnerBlock.HeatLevel level = BlazeBurnerBlock.HeatLevel.SMOULDERING;
      switch (this.activeFuel) {
         case NORMAL:
            boolean lowPercent = (double)this.remainingBurnTime / 10000.0 < 0.0125;
            level = lowPercent ? BlazeBurnerBlock.HeatLevel.FADING : BlazeBurnerBlock.HeatLevel.KINDLED;
            break;
         case SPECIAL:
            level = BlazeBurnerBlock.HeatLevel.SEETHING;
      }

      return level;
   }

   protected void spawnParticles(BlazeBurnerBlock.HeatLevel heatLevel, double burstMult) {
      if (this.level != null) {
         if (heatLevel != BlazeBurnerBlock.HeatLevel.NONE) {
            RandomSource r = this.level.getRandom();
            Vec3 c = VecHelper.getCenterOf(this.worldPosition);
            Vec3 v = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, 0.125F).multiply(1.0, 0.0, 1.0));
            if (r.nextInt(4) == 0) {
               boolean empty = this.level.getBlockState(this.worldPosition.above()).getCollisionShape(this.level, this.worldPosition.above()).isEmpty();
               if (empty || r.nextInt(8) == 0) {
                  this.level.addParticle(ParticleTypes.LARGE_SMOKE, v.x, v.y, v.z, 0.0, 0.0, 0.0);
               }

               double yMotion = empty ? 0.0625 : r.nextDouble() * 0.0125F;
               Vec3 v2 = c.add(
                     VecHelper.offsetRandomly(Vec3.ZERO, r, 0.5F).multiply(1.0, 0.25, 1.0).normalize().scale((empty ? 0.25 : 0.5) + r.nextDouble() * 0.125)
                  )
                  .add(0.0, 0.5, 0.0);
               if (heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.SEETHING)) {
                  this.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, v2.x, v2.y, v2.z, 0.0, yMotion, 0.0);
               } else if (heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)) {
                  this.level.addParticle(ParticleTypes.FLAME, v2.x, v2.y, v2.z, 0.0, yMotion, 0.0);
               }
            }
         }
      }
   }

   public void spawnParticleBurst(boolean soulFlame) {
      Vec3 c = VecHelper.getCenterOf(this.worldPosition);
      RandomSource r = this.level.random;

      for (int i = 0; i < 20; i++) {
         Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, r, 0.5F).multiply(1.0, 0.25, 1.0).normalize();
         Vec3 v = c.add(offset.scale(0.5 + r.nextDouble() * 0.125)).add(0.0, 0.125, 0.0);
         Vec3 m = offset.scale(0.03125);
         this.level.addParticle(soulFlame ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, v.x, v.y, v.z, m.x, m.y, m.z);
      }
   }

   public static enum FuelType {
      NONE,
      NORMAL,
      SPECIAL;
   }
}
