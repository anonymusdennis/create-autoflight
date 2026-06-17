package com.simibubi.create.foundation.blockEntity.behaviour.filtering;

import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SidedFilteringBehaviour extends FilteringBehaviour {
   Map<Direction, FilteringBehaviour> sidedFilters;
   private BiFunction<Direction, FilteringBehaviour, FilteringBehaviour> filterFactory;
   private Predicate<Direction> validDirections;

   public SidedFilteringBehaviour(
      SmartBlockEntity be,
      ValueBoxTransform.Sided sidedSlot,
      BiFunction<Direction, FilteringBehaviour, FilteringBehaviour> filterFactory,
      Predicate<Direction> validDirections
   ) {
      super(be, sidedSlot);
      this.filterFactory = filterFactory;
      this.validDirections = validDirections;
      this.sidedFilters = new IdentityHashMap<>();
      this.updateFilterPresence();
   }

   @Override
   public void initialize() {
      super.initialize();
   }

   public FilteringBehaviour get(Direction side) {
      return this.sidedFilters.get(side);
   }

   public void updateFilterPresence() {
      Set<Direction> valid = new HashSet<>();

      for (Direction d : Iterate.directions) {
         if (this.validDirections.test(d)) {
            valid.add(d);
         }
      }

      for (Direction dx : Iterate.directions) {
         if (valid.contains(dx)) {
            if (!this.sidedFilters.containsKey(dx)) {
               this.sidedFilters.put(dx, this.filterFactory.apply(dx, new FilteringBehaviour(this.blockEntity, this.slotPositioning)));
            }
         } else if (this.sidedFilters.containsKey(dx)) {
            this.removeFilter(dx);
         }
      }
   }

   @Override
   public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
      nbt.put("Filters", NBTHelper.writeCompoundList(this.sidedFilters.entrySet(), entry -> {
         CompoundTag compound = new CompoundTag();
         compound.putInt("Side", ((Direction)entry.getKey()).get3DDataValue());
         ((FilteringBehaviour)entry.getValue()).write(compound, registries, clientPacket);
         return compound;
      }));
      super.write(nbt, registries, clientPacket);
   }

   @Override
   public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
      NBTHelper.iterateCompoundList(nbt.getList("Filters", 10), compound -> {
         Direction face = Direction.from3DDataValue(compound.getInt("Side"));
         if (this.sidedFilters.containsKey(face)) {
            this.sidedFilters.get(face).read(compound, registries, clientPacket);
         }
      });
      super.read(nbt, registries, clientPacket);
   }

   @Override
   public void tick() {
      super.tick();
      this.sidedFilters.values().forEach(BlockEntityBehaviour::tick);
   }

   @Override
   public boolean setFilter(Direction side, ItemStack stack) {
      if (!this.sidedFilters.containsKey(side)) {
         return true;
      } else {
         this.sidedFilters.get(side).setFilter(stack);
         return true;
      }
   }

   @Override
   public ItemStack getFilter(Direction side) {
      return !this.sidedFilters.containsKey(side) ? ItemStack.EMPTY : this.sidedFilters.get(side).getFilter();
   }

   public boolean test(Direction side, ItemStack stack) {
      return !this.sidedFilters.containsKey(side) ? true : this.sidedFilters.get(side).test(stack);
   }

   @Override
   public void destroy() {
      this.sidedFilters.values().forEach(FilteringBehaviour::destroy);
      super.destroy();
   }

   @Override
   public ItemRequirement getRequiredItems() {
      return this.sidedFilters.values().stream().reduce(ItemRequirement.NONE, (a, b) -> a.union(b.getRequiredItems()), (a, b) -> a.union(b));
   }

   public void removeFilter(Direction side) {
      if (this.sidedFilters.containsKey(side)) {
         this.sidedFilters.remove(side).destroy();
      }
   }

   public boolean testHit(LevelAccessor level, BlockPos pos, Direction direction, Vec3 hit) {
      ValueBoxTransform.Sided sidedPositioning = (ValueBoxTransform.Sided)this.slotPositioning;
      BlockState state = this.blockEntity.getBlockState();
      Vec3 localHit = hit.subtract(Vec3.atLowerCornerOf(this.blockEntity.getBlockPos()));
      return sidedPositioning.fromSide(direction).testHit(level, pos, state, localHit);
   }
}
