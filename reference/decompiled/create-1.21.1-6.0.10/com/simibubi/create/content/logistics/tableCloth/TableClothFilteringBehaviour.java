package com.simibubi.create.content.logistics.tableCloth;

import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

public class TableClothFilteringBehaviour extends FilteringBehaviour {
   public TableClothFilteringBehaviour(TableClothBlockEntity be) {
      super(be, new TableClothFilterSlot(be));
      this.withPredicate(is -> !(is.getItem() instanceof FilterItem) && !(is.getItem() instanceof ShoppingListItem));
      this.count = 1;
   }

   @Override
   public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
      super.onShortInteract(player, hand, side, hitResult);
   }

   @Override
   public float getRenderDistance() {
      return 32.0F;
   }

   private TableClothBlockEntity dbe() {
      return (TableClothBlockEntity)this.blockEntity;
   }

   @Override
   public boolean mayInteract(Player player) {
      return this.dbe().owner == null || player.getUUID().equals(this.dbe().owner);
   }

   @Override
   public boolean isSafeNBT() {
      return false;
   }

   @Override
   public MutableComponent getLabel() {
      return CreateLang.translateDirect("table_cloth.price_per_order");
   }

   @Override
   public boolean isCountVisible() {
      return !this.filter.isEmpty();
   }

   @Override
   public boolean setFilter(ItemStack stack) {
      int before = this.count;
      boolean result = super.setFilter(stack);
      this.count = before;
      return result;
   }

   @Override
   public void setValueSettings(Player player, ValueSettingsBehaviour.ValueSettings settings, boolean ctrlDown) {
      if (!this.getValueSettings().equals(settings)) {
         this.count = Math.max(1, settings.value());
         this.blockEntity.setChanged();
         this.blockEntity.sendData();
         this.playFeedbackSound(this);
      }
   }

   @Override
   public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
      return new ValueSettingsBoard(
         this.getLabel(), 100, 10, CreateLang.translatedOptions("table_cloth", "amount"), new ValueSettingsFormatter(this::formatValue)
      );
   }

   @Override
   public MutableComponent formatValue(ValueSettingsBehaviour.ValueSettings value) {
      return Component.literal(String.valueOf(Math.max(1, value.value())));
   }

   @Override
   public MutableComponent getCountLabelForValueBox() {
      return Component.literal(this.isCountVisible() ? String.valueOf(this.count) : "");
   }

   @Override
   public boolean isActive() {
      return this.dbe().isShop();
   }
}
