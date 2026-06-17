package dev.simulated_team.simulated.content.blocks.absorber;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionRegion;
import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import dev.ryanhcode.sable.util.LevelAccelerator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.List;
import java.util.Set;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class AbsorberBlockEntity extends SmartBlockEntity {
   private static final Direction[] DIRECTION_PRIORITY = new Direction[]{
      Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN
   };
   @Nullable
   private WaterOcclusionRegion currentRegion;
   public LerpedFloat animationTimer = LerpedFloat.linear();

   public AbsorberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.animationTimer.chase(0.0, 0.04, Chaser.LINEAR);
   }

   private static boolean dfs(LevelAccelerator accelerator, BlockPos pos, Set<BlockPos> visited, Set<BlockPos> enclosed) {
      boolean safe = pos.getY() <= accelerator.getMaxBuildHeight();
      visited.add(pos);
      BlockState state = accelerator.getBlockState(pos);
      boolean isEnclosed = !VoxelNeighborhoodState.isSolid(accelerator, pos, state);
      if (!isEnclosed) {
         return safe;
      } else {
         enclosed.add(pos);

         for (Direction dir : DIRECTION_PRIORITY) {
            BlockPos relative = pos.relative(dir);
            enclosed.add(relative);
            if (!visited.contains(relative)) {
               safe = safe && dfs(accelerator, relative, visited, enclosed);
            }
         }

         return safe;
      }
   }

   public void tick() {
      super.tick();
      boolean powered = (Boolean)this.getBlockState().getValue(AbsorberBlock.POWERED);
      if (this.currentRegion != null && this.currentRegion.isDirty()) {
         this.removeRegionIfExists();
      }

      this.animationTimer.tickChaser();
      if (this.animationTimer.settled()) {
         this.animationTimer.updateChaseTarget(powered ? 1.0F : 0.0F);
      }

      if (this.animationTimer.settled() && !this.level.isClientSide) {
         if (powered) {
            if (this.currentRegion == null) {
               this.buildRegion();
            }

            boolean doWet = false;

            for (Direction dir : Direction.values()) {
               BlockPos newPos = this.getBlockPos().relative(dir);
               BlockState blockstate = this.level.getBlockState(newPos);
               FluidState fluidstate = this.level.getFluidState(newPos);
               if (fluidstate.is(FluidTags.WATER) && blockstate.getBlock() instanceof LiquidBlock) {
                  this.level.setBlock(newPos, Blocks.AIR.defaultBlockState(), 3);
                  doWet = true;
               }
            }

            if (doWet && !(Boolean)this.getBlockState().getValue(AbsorberBlock.WET)) {
               this.level.setBlock(this.getBlockPos(), (BlockState)this.getBlockState().cycle(AbsorberBlock.WET), 2);
            }
         } else {
            this.removeRegionIfExists();
            if ((Boolean)this.getBlockState().getValue(AbsorberBlock.WET)) {
               this.level.setBlock(this.getBlockPos(), (BlockState)this.getBlockState().cycle(AbsorberBlock.WET), 2);
            }
         }
      }

      if (this.level.isClientSide
         && this.animationTimer.getChaseTarget() < this.animationTimer.getValue()
         && (Boolean)this.getBlockState().getValue(AbsorberBlock.WET)) {
         BlockPos pos = this.getBlockPos();
         float t = this.animationTimer.getValue();
         float offset = 0.5F + t * t * 0.5F;

         for (int i = 0; i < 2; i++) {
            this.level
               .addParticle(
                  ParticleTypes.SPLASH,
                  (double)((float)pos.getX() + this.level.random.nextFloat()),
                  (double)((float)pos.getY() + offset),
                  (double)((float)pos.getZ() + this.level.random.nextFloat()),
                  0.0,
                  0.0,
                  0.0
               );
         }
      }
   }

   private void buildRegion() {
      if (this.currentRegion != null) {
         throw new IllegalStateException("EvaporatorBlockEntity already has a region assigned.");
      } else {
         WaterOcclusionContainer<?> container = WaterOcclusionContainer.getContainer(this.level);
         ObjectOpenHashSet<BlockPos> visited = new ObjectOpenHashSet();
         ObjectOpenHashSet<BlockPos> enclosed = new ObjectOpenHashSet();
         if (dfs(new LevelAccelerator(this.level), this.worldPosition.above(), visited, enclosed) && !enclosed.isEmpty()) {
            this.currentRegion = container.addRegion(BoundedBitVolume3i.fromBlocks(enclosed));
         }
      }
   }

   public void invalidate() {
      super.invalidate();
      this.removeRegionIfExists();
   }

   private void removeRegionIfExists() {
      if (this.currentRegion != null) {
         WaterOcclusionContainer container = WaterOcclusionContainer.getContainer(this.level);
         if (container != null) {
            container.removeRegion(this.currentRegion);
         }

         this.currentRegion = null;
      }
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }
}
