package com.simibubi.create.content.logistics.tableCloth;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlock;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.block.Block;

public class TableClothBlockItem extends BlockItem {
   public TableClothBlockItem(Block pBlock, Properties pProperties) {
      super(pBlock, pProperties);
   }

   public boolean isFoil(ItemStack pStack) {
      return pStack.has(AllDataComponents.AUTO_REQUEST_DATA);
   }

   public void appendHoverText(ItemStack stack, TooltipContext tooltipContext, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      super.appendHoverText(stack, tooltipContext, tooltipComponents, tooltipFlag);
      if (this.isFoil(stack)) {
         CreateLang.translate("table_cloth.shop_configured").style(ChatFormatting.GOLD).addTo(tooltipComponents);
         RedstoneRequesterBlock.appendRequesterTooltip(stack, tooltipComponents);
      }
   }
}
