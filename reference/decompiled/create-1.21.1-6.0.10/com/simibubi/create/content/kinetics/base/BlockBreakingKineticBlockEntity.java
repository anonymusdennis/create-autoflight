package com.simibubi.create.content.kinetics.base;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.utility.BlockHelper;
import java.util.concurrent.atomic.AtomicInteger;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class BlockBreakingKineticBlockEntity extends KineticBlockEntity {
   public static final AtomicInteger NEXT_BREAKER_ID = new AtomicInteger();
   protected int ticksUntilNextProgress;
   protected int destroyProgress;
   protected int breakerId = -NEXT_BREAKER_ID.incrementAndGet();
   protected BlockPos breakingPos;

   public BlockBreakingKineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void onSpeedChanged(float prevSpeed) {
      super.onSpeedChanged(prevSpeed);
      if (this.destroyProgress == -1) {
         this.destroyNextTick();
      }
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      if (this.ticksUntilNextProgress == -1) {
         this.destroyNextTick();
      }
   }

   public void destroyNextTick() {
      this.ticksUntilNextProgress = 1;
   }

   protected abstract BlockPos getBreakingPos();

   protected boolean shouldRun() {
      return true;
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putInt("Progress", this.destroyProgress);
      compound.putInt("NextTick", this.ticksUntilNextProgress);
      if (this.breakingPos != null) {
         compound.put("Breaking", NbtUtils.writeBlockPos(this.breakingPos));
      }

      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.destroyProgress = compound.getInt("Progress");
      this.ticksUntilNextProgress = compound.getInt("NextTick");
      this.breakingPos = null;
      if (compound.contains("Breaking")) {
         this.breakingPos = NBTHelper.readBlockPos(compound, "Breaking");
      }

      super.read(compound, registries, clientPacket);
   }

   @Override
   public void invalidate() {
      super.invalidate();
      if (!this.level.isClientSide && this.destroyProgress != 0) {
         this.level.destroyBlockProgress(this.breakerId, this.breakingPos, -1);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level.isClientSide) {
         if (this.shouldRun()) {
            if (this.getSpeed() != 0.0F) {
               this.breakingPos = this.getBreakingPos();
               if (this.ticksUntilNextProgress >= 0) {
                  if (this.ticksUntilNextProgress-- <= 0) {
                     BlockState stateToBreak = this.level.getBlockState(this.breakingPos);
                     float blockHardness = stateToBreak.getDestroySpeed(this.level, this.breakingPos);
                     if (!this.canBreak(stateToBreak, blockHardness)) {
                        if (this.destroyProgress != 0) {
                           this.destroyProgress = 0;
                           this.level.destroyBlockProgress(this.breakerId, this.breakingPos, -1);
                        }
                     } else {
                        float breakSpeed = this.getBreakSpeed();
                        this.destroyProgress = this.destroyProgress + Mth.clamp((int)(breakSpeed / blockHardness), 1, 10 - this.destroyProgress);
                        this.level.playSound(null, this.worldPosition, stateToBreak.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.25F, 1.0F);
                        if (this.destroyProgress >= 10) {
                           this.onBlockBroken(stateToBreak);
                           this.destroyProgress = 0;
                           this.ticksUntilNextProgress = -1;
                           this.level.destroyBlockProgress(this.breakerId, this.breakingPos, -1);
                        } else {
                           this.ticksUntilNextProgress = (int)(blockHardness / breakSpeed);
                           this.level.destroyBlockProgress(this.breakerId, this.breakingPos, this.destroyProgress);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public boolean canBreak(BlockState stateToBreak, float blockHardness) {
      return isBreakable(stateToBreak, blockHardness);
   }

   public static boolean isBreakable(BlockState stateToBreak, float blockHardness) {
      return !stateToBreak.liquid()
         && !(stateToBreak.getBlock() instanceof AirBlock)
         && blockHardness != -1.0F
         && !AllTags.AllBlockTags.NON_BREAKABLE.matches(stateToBreak);
   }

   public void onBlockBroken(BlockState stateToBreak) {
      Vec3 vec = VecHelper.offsetRandomly(VecHelper.getCenterOf(this.breakingPos), this.level.random, 0.125F);
      BlockHelper.destroyBlock(this.level, this.breakingPos, 1.0F, stack -> {
         if (!stack.isEmpty()) {
            if (this.level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
               if (!this.level.restoringBlockSnapshots) {
                  ItemEntity itementity = new ItemEntity(this.level, vec.x, vec.y, vec.z, stack);
                  itementity.setDefaultPickUpDelay();
                  itementity.setDeltaMovement(Vec3.ZERO);
                  this.level.addFreshEntity(itementity);
               }
            }
         }
      });
   }

   protected float getBreakSpeed() {
      return Math.abs(this.getSpeed() / 100.0F);
   }
}
