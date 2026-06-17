package com.simibubi.create.content.logistics.itemHatch;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ItemHatchBlockEntity extends SmartBlockEntity implements Clearable {
   public FilteringBehaviour filtering;

   public ItemHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.filtering = new FilteringBehaviour(this, new HatchFilterSlot()));
   }

   public void clearContent() {
      this.filtering.setFilter(ItemStack.EMPTY);
   }
}
