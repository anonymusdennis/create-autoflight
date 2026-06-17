package com.simibubi.create.content.logistics.filter;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllKeys;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.recipe.ItemCopyingRecipe;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public abstract class FilterItem extends Item implements MenuProvider, ItemCopyingRecipe.SupportsItemCopying {
   public static ListFilterItem regular(Properties properties) {
      return new ListFilterItem(properties);
   }

   public static AttributeFilterItem attribute(Properties properties) {
      return new AttributeFilterItem(properties);
   }

   public static PackageFilterItem address(Properties properties) {
      return new PackageFilterItem(properties);
   }

   protected FilterItem(Properties properties) {
      super(properties);
   }

   @NotNull
   public InteractionResult useOn(UseOnContext context) {
      return context.getPlayer() == null ? InteractionResult.PASS : this.use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
   }

   @OnlyIn(Dist.CLIENT)
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
      if (!AllKeys.shiftDown()) {
         List<Component> makeSummary = this.makeSummary(stack);
         if (!makeSummary.isEmpty()) {
            tooltip.add(CommonComponents.SPACE);
            tooltip.addAll(makeSummary);
         }
      }
   }

   public abstract List<Component> makeSummary(ItemStack var1);

   public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
      ItemStack heldItem = player.getItemInHand(hand);
      if (!player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
         if (!world.isClientSide && player instanceof ServerPlayer) {
            player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, heldItem));
         }

         return InteractionResultHolder.success(heldItem);
      } else {
         return InteractionResultHolder.pass(heldItem);
      }
   }

   public abstract AbstractContainerMenu createMenu(int var1, Inventory var2, Player var3);

   public Component getDisplayName() {
      return this.getDescription();
   }

   public static boolean testDirect(ItemStack filter, ItemStack stack, boolean matchNBT) {
      if (matchNBT) {
         return PackageItem.isPackage(filter) && PackageItem.isPackage(stack)
            ? doPackagesHaveSameData(filter, stack)
            : ItemStack.isSameItemSameComponents(filter, stack);
      } else {
         return PackageItem.isPackage(filter) && PackageItem.isPackage(stack) ? true : ItemHelper.sameItem(filter, stack);
      }
   }

   public static boolean doPackagesHaveSameData(@NotNull ItemStack a, @NotNull ItemStack b) {
      if (a.isEmpty()) {
         return false;
      } else if (!ItemStack.isSameItemSameComponents(a, b)) {
         return false;
      } else {
         for (TypedDataComponent<?> component : a.getComponents()) {
            DataComponentType<?> type = component.type();
            if (!type.equals(AllDataComponents.PACKAGE_ORDER_DATA)
               && !type.equals(AllDataComponents.PACKAGE_ORDER_CONTEXT)
               && !Objects.equals(a.get(type), b.get(type))) {
               return false;
            }
         }

         return true;
      }
   }

   @Override
   public abstract DataComponentType<?> getComponentType();

   public abstract FilterItemStack makeStackWrapper(ItemStack var1);

   public abstract ItemStack[] getFilterItems(ItemStack var1);
}
