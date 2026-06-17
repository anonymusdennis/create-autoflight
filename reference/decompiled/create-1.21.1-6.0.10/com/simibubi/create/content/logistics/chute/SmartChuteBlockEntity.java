package com.simibubi.create.content.logistics.chute;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;

public class SmartChuteBlockEntity extends ChuteBlockEntity implements Clearable {
   FilteringBehaviour filtering;

   public SmartChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.SMART_CHUTE.get(), (be, context) -> be.itemHandler);
   }

   @Override
   protected boolean canAcceptItem(ItemStack stack) {
      return super.canAcceptItem(stack) && this.canActivate() && this.filtering.test(stack);
   }

   @Override
   protected int getExtractionAmount() {
      return this.filtering.isCountVisible() && !this.filtering.anyAmount() ? this.filtering.getAmount() : 64;
   }

   @Override
   protected ItemHelper.ExtractionCountMode getExtractionMode() {
      return this.filtering.isCountVisible() && !this.filtering.anyAmount() && !this.filtering.upTo
         ? ItemHelper.ExtractionCountMode.EXACTLY
         : ItemHelper.ExtractionCountMode.UPTO;
   }

   @Override
   protected boolean canActivate() {
      BlockState blockState = this.getBlockState();
      return blockState.hasProperty(SmartChuteBlock.POWERED) && !(Boolean)blockState.getValue(SmartChuteBlock.POWERED);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(
         this.filtering = new FilteringBehaviour(this, new SmartChuteFilterSlotPositioning())
            .showCountWhen(this::isExtracting)
            .withCallback($ -> this.invVersionTracker.reset())
      );
      super.addBehaviours(behaviours);
   }

   @Override
   public void clearContent() {
      super.clearContent();
      this.filtering.setFilter(ItemStack.EMPTY);
   }

   private boolean isExtracting() {
      boolean up = this.getItemMotion() < 0.0F;
      BlockPos chutePos = this.worldPosition.relative(up ? Direction.UP : Direction.DOWN);
      BlockState blockState = this.level.getBlockState(chutePos);
      return !AbstractChuteBlock.isChute(blockState) && !blockState.canBeReplaced();
   }
}
