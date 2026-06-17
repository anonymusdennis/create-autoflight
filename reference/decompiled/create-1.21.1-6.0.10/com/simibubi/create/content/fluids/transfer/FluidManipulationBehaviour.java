package com.simibubi.create.content.fluids.transfer;

import com.google.common.base.Predicates;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public abstract class FluidManipulationBehaviour extends BlockEntityBehaviour {
   BoundingBox affectedArea;
   BlockPos rootPos;
   boolean infinite;
   protected boolean counterpartActed;
   static final int searchedPerTick = 1024;
   static final int validationTimerMin = 160;
   List<FluidManipulationBehaviour.BlockPosEntry> frontier;
   Set<BlockPos> visited;
   int revalidateIn;

   public FluidManipulationBehaviour(SmartBlockEntity be) {
      super(be);
      this.setValidationTimer();
      this.infinite = false;
      this.visited = new HashSet<>();
      this.frontier = new ArrayList<>();
   }

   public boolean isInfinite() {
      return this.infinite;
   }

   public void counterpartActed() {
      this.counterpartActed = true;
   }

   protected int validationTimer() {
      int maxBlocks = this.maxBlocks();
      return maxBlocks < 0 ? 160 : Math.max(160, maxBlocks / 1024 + 1);
   }

   protected int setValidationTimer() {
      return this.revalidateIn = this.validationTimer();
   }

   protected int setLongValidationTimer() {
      return this.revalidateIn = this.validationTimer() * 2;
   }

   protected int maxRange() {
      return (Integer)AllConfigs.server().fluids.hosePulleyRange.get();
   }

   protected int maxBlocks() {
      return (Integer)AllConfigs.server().fluids.hosePulleyBlockThreshold.get();
   }

   protected boolean fillInfinite() {
      return (Boolean)AllConfigs.server().fluids.fillInfinite.get();
   }

   public void reset() {
      if (this.affectedArea != null) {
         this.scheduleUpdatesInAffectedArea();
      }

      this.affectedArea = null;
      this.setValidationTimer();
      this.frontier.clear();
      this.visited.clear();
      this.infinite = false;
   }

   @Override
   public void destroy() {
      this.reset();
      super.destroy();
   }

   protected void scheduleUpdatesInAffectedArea() {
      Level world = this.getWorld();
      BlockPos.betweenClosedStream(
            new BlockPos(this.affectedArea.minX() - 1, this.affectedArea.minY() - 1, this.affectedArea.minZ() - 1),
            new BlockPos(this.affectedArea.maxX() + 1, this.affectedArea.maxY() + 1, this.affectedArea.maxZ() + 1)
         )
         .forEach(pos -> {
            FluidState nextFluidState = world.getFluidState(pos);
            if (!nextFluidState.isEmpty()) {
               world.scheduleTick(pos, nextFluidState.getType(), world.getRandom().nextInt(5));
            }
         });
   }

   protected int comparePositions(FluidManipulationBehaviour.BlockPosEntry e1, FluidManipulationBehaviour.BlockPosEntry e2) {
      Vec3 centerOfRoot = VecHelper.getCenterOf(this.rootPos);
      BlockPos pos2 = e2.pos;
      BlockPos pos1 = e1.pos;
      if (pos1.getY() != pos2.getY()) {
         return Integer.compare(pos2.getY(), pos1.getY());
      } else {
         int compareDistance = Integer.compare(e2.distance, e1.distance);
         return compareDistance != 0
            ? compareDistance
            : Double.compare(VecHelper.getCenterOf(pos2).distanceToSqr(centerOfRoot), VecHelper.getCenterOf(pos1).distanceToSqr(centerOfRoot));
      }
   }

   protected Fluid search(
      Fluid fluid, List<FluidManipulationBehaviour.BlockPosEntry> frontier, Set<BlockPos> visited, BiConsumer<BlockPos, Integer> add, boolean searchDownward
   ) throws FluidManipulationBehaviour.ChunkNotLoadedException {
      Level world = this.getWorld();
      int maxBlocks = this.maxBlocks();
      int maxRange = this.maxRange();
      int maxRangeSq = maxRange * maxRange;

      for (int i = 0; i < 1024 && !frontier.isEmpty() && (visited.size() <= maxBlocks || !this.canDrainInfinitely(fluid)); i++) {
         FluidManipulationBehaviour.BlockPosEntry entry = frontier.remove(0);
         BlockPos currentPos = entry.pos;
         if (!visited.contains(currentPos)) {
            visited.add(currentPos);
            if (!world.isLoaded(currentPos)) {
               throw new FluidManipulationBehaviour.ChunkNotLoadedException();
            }

            FluidState fluidState = world.getFluidState(currentPos);
            if (!fluidState.isEmpty()) {
               Fluid currentFluid = FluidHelper.convertToStill(fluidState.getType());
               if (fluid == null) {
                  fluid = currentFluid;
               }

               if (currentFluid.isSame(fluid)) {
                  add.accept(currentPos, entry.distance);

                  for (Direction side : Iterate.directions) {
                     if (searchDownward || side != Direction.DOWN) {
                        BlockPos offsetPos = currentPos.relative(side);
                        if (!world.isLoaded(offsetPos)) {
                           throw new FluidManipulationBehaviour.ChunkNotLoadedException();
                        }

                        if (!visited.contains(offsetPos) && !(offsetPos.distSqr(this.rootPos) > (double)maxRangeSq)) {
                           FluidState nextFluidState = world.getFluidState(offsetPos);
                           if (!nextFluidState.isEmpty()) {
                              Fluid nextFluid = nextFluidState.getType();
                              if (nextFluid != FluidHelper.convertToFlowing(nextFluid)
                                 || side != Direction.UP
                                 || VecHelper.onSameAxis(this.rootPos, offsetPos, Axis.Y)) {
                                 frontier.add(new FluidManipulationBehaviour.BlockPosEntry(offsetPos, entry.distance + 1));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return fluid;
   }

   protected void playEffect(Level world, BlockPos pos, Fluid fluid, boolean fillSound) {
      if (fluid != null) {
         BlockPos splooshPos = pos == null ? this.blockEntity.getBlockPos() : pos;
         FluidStack stack = new FluidStack(fluid, 1);
         SoundEvent soundevent = fillSound ? FluidHelper.getFillSound(stack) : FluidHelper.getEmptySound(stack);
         world.playSound(null, splooshPos, soundevent, SoundSource.BLOCKS, 0.3F, 1.0F);
         if (world instanceof ServerLevel serverLevel) {
            CatnipServices.NETWORK.sendToClientsAround(serverLevel, splooshPos, 10.0, new FluidSplashPacket(splooshPos, stack));
         }
      }
   }

   protected boolean canDrainInfinitely(Fluid fluid) {
      return fluid == null
         ? false
         : this.maxBlocks() != -1 && ((FluidManipulationBehaviour.BottomlessFluidMode)AllConfigs.server().fluids.bottomlessFluidMode.get()).test(fluid);
   }

   @Override
   public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
      if (this.infinite) {
         NBTHelper.putMarker(nbt, "Infinite");
      }

      if (this.rootPos != null) {
         nbt.put("LastPos", NbtUtils.writeBlockPos(this.rootPos));
      }

      if (this.affectedArea != null) {
         nbt.put("AffectedAreaFrom", NbtUtils.writeBlockPos(new BlockPos(this.affectedArea.minX(), this.affectedArea.minY(), this.affectedArea.minZ())));
         nbt.put("AffectedAreaTo", NbtUtils.writeBlockPos(new BlockPos(this.affectedArea.maxX(), this.affectedArea.maxY(), this.affectedArea.maxZ())));
      }

      super.write(nbt, registries, clientPacket);
   }

   @Override
   public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
      this.infinite = nbt.contains("Infinite");
      if (nbt.contains("LastPos")) {
         this.rootPos = NBTHelper.readBlockPos(nbt, "LastPos");
      }

      if (nbt.contains("AffectedAreaFrom") && nbt.contains("AffectedAreaTo")) {
         this.affectedArea = BoundingBox.fromCorners(NBTHelper.readBlockPos(nbt, "AffectedAreaFrom"), NBTHelper.readBlockPos(nbt, "AffectedAreaTo"));
      }

      super.read(nbt, registries, clientPacket);
   }

   public static record BlockPosEntry(BlockPos pos, int distance) {
   }

   public static enum BottomlessFluidMode implements Predicate<Fluid> {
      ALLOW_ALL(Predicates.alwaysTrue()),
      DENY_ALL(Predicates.alwaysFalse()),
      ALLOW_BY_TAG(AllTags.AllFluidTags.BOTTOMLESS_ALLOW::matches),
      DENY_BY_TAG(Predicates.not(AllTags.AllFluidTags.BOTTOMLESS_DENY::matches));

      private final Predicate<Fluid> predicate;

      private BottomlessFluidMode(Predicate<Fluid> predicate) {
         this.predicate = predicate;
      }

      public boolean test(Fluid fluid) {
         return this.predicate.test(fluid);
      }
   }

   public static class ChunkNotLoadedException extends Exception {
      private static final long serialVersionUID = 1L;
   }
}
