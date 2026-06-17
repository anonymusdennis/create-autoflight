package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;

public class DepotBlockEntity extends SmartBlockEntity implements Clearable {
   DepotBehaviour depotBehaviour;

   public DepotBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.DEPOT.get(), (be, context) -> be.depotBehaviour.itemHandler);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.depotBehaviour = new DepotBehaviour(this));
      this.depotBehaviour.addSubBehaviours(behaviours);
   }

   public void clearContent() {
      this.depotBehaviour.clearContent();
   }

   public ItemStack getHeldItem() {
      return this.depotBehaviour.getHeldItemStack();
   }

   public void setHeldItem(ItemStack item) {
      TransportedItemStack newStack = new TransportedItemStack(item);
      if (this.depotBehaviour.heldItem != null) {
         newStack.angle = this.depotBehaviour.heldItem.angle;
      }

      this.depotBehaviour.setHeldItem(newStack);
   }
}
