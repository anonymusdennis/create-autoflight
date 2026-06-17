package com.simibubi.create.content.logistics.tableCloth;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.foundation.utility.CreateLang;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.IntAttached;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public class ShoppingListItem extends Item {
   public ShoppingListItem(Properties pProperties) {
      super(pProperties);
   }

   public static ShoppingListItem.ShoppingList getList(ItemStack stack) {
      return (ShoppingListItem.ShoppingList)stack.get(AllDataComponents.SHOPPING_LIST);
   }

   public static ItemStack saveList(ItemStack stack, ShoppingListItem.ShoppingList list, String address) {
      stack.set(AllDataComponents.SHOPPING_LIST, list);
      stack.set(AllDataComponents.SHOPPING_LIST_ADDRESS, address);
      return stack;
   }

   public static String getAddress(ItemStack stack) {
      return (String)stack.getOrDefault(AllDataComponents.SHOPPING_LIST_ADDRESS, "");
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      ShoppingListItem.ShoppingList list = getList(stack);
      if (list != null) {
         Couple<InventorySummary> lists = list.bakeEntries(context.level(), null);
         if (lists != null) {
            for (InventorySummary items : lists) {
               List<BigItemStack> entries = items.getStacksByCount();
               boolean cost = items == lists.getSecond();
               if (cost) {
                  tooltipComponents.add(Component.empty());
               }

               if (entries.size() == 1) {
                  BigItemStack entry = entries.get(0);
                  (cost ? CreateLang.translate("table_cloth.total_cost") : CreateLang.text(""))
                     .style(ChatFormatting.GOLD)
                     .add(
                        CreateLang.builder()
                           .add(entry.stack.getHoverName().plainCopy())
                           .text(" x")
                           .text(String.valueOf(entry.count))
                           .style(cost ? ChatFormatting.YELLOW : ChatFormatting.GRAY)
                     )
                     .addTo(tooltipComponents);
               } else {
                  if (cost) {
                     CreateLang.translate("table_cloth.total_cost").style(ChatFormatting.GOLD).addTo(tooltipComponents);
                  }

                  for (BigItemStack entry : entries) {
                     CreateLang.builder()
                        .add(entry.stack.getHoverName().plainCopy())
                        .text(" x")
                        .text(String.valueOf(entry.count))
                        .style(cost ? ChatFormatting.YELLOW : ChatFormatting.GRAY)
                        .addTo(tooltipComponents);
                  }
               }
            }
         }
      }

      CreateLang.translate("table_cloth.hand_to_shop_keeper").style(ChatFormatting.GRAY).addTo(tooltipComponents);
      CreateLang.translate("table_cloth.sneak_click_discard").style(ChatFormatting.DARK_GRAY).addTo(tooltipComponents);
   }

   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
      if (pUsedHand != InteractionHand.OFF_HAND && pPlayer != null && pPlayer.isShiftKeyDown()) {
         CreateLang.translate("table_cloth.shopping_list_discarded").sendStatus(pPlayer);
         pPlayer.playSound(SoundEvents.BOOK_PAGE_TURN);
         return new InteractionResultHolder(InteractionResult.SUCCESS, ItemStack.EMPTY);
      } else {
         return new InteractionResultHolder(InteractionResult.PASS, pPlayer.getItemInHand(pUsedHand));
      }
   }

   public InteractionResult useOn(UseOnContext pContext) {
      InteractionHand pUsedHand = pContext.getHand();
      Player pPlayer = pContext.getPlayer();
      if (pUsedHand != InteractionHand.OFF_HAND && pPlayer != null && pPlayer.isShiftKeyDown()) {
         pPlayer.setItemInHand(pUsedHand, ItemStack.EMPTY);
         CreateLang.translate("table_cloth.shopping_list_discarded").sendStatus(pPlayer);
         pPlayer.playSound(SoundEvents.BOOK_PAGE_TURN);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   public static record ShoppingList(@Unmodifiable List<IntAttached<BlockPos>> purchases, UUID shopOwner, UUID shopNetwork) {
      public static final Codec<ShoppingListItem.ShoppingList> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  Codec.list(IntAttached.codec(BlockPos.CODEC)).fieldOf("purchases").forGetter(ShoppingListItem.ShoppingList::purchases),
                  UUIDUtil.CODEC.fieldOf("shop_owner").forGetter(ShoppingListItem.ShoppingList::shopOwner),
                  UUIDUtil.CODEC.fieldOf("shop_network").forGetter(ShoppingListItem.ShoppingList::shopNetwork)
               )
               .apply(instance, ShoppingListItem.ShoppingList::new)
      );
      public static final StreamCodec<ByteBuf, ShoppingListItem.ShoppingList> STREAM_CODEC = StreamCodec.composite(
         CatnipStreamCodecBuilders.list(IntAttached.streamCodec(BlockPos.STREAM_CODEC)),
         ShoppingListItem.ShoppingList::purchases,
         UUIDUtil.STREAM_CODEC,
         ShoppingListItem.ShoppingList::shopOwner,
         UUIDUtil.STREAM_CODEC,
         ShoppingListItem.ShoppingList::shopNetwork,
         ShoppingListItem.ShoppingList::new
      );

      public ShoppingListItem.ShoppingList duplicate() {
         return new ShoppingListItem.ShoppingList(
            new ArrayList<>(this.purchases.stream().map(ia -> IntAttached.with((Integer)ia.getFirst(), (BlockPos)ia.getSecond())).toList()),
            this.shopOwner,
            this.shopNetwork
         );
      }

      public int getPurchases(BlockPos clothPos) {
         for (IntAttached<BlockPos> entry : this.purchases) {
            if (clothPos.equals(entry.getValue())) {
               return (Integer)entry.getFirst();
            }
         }

         return 0;
      }

      public Couple<InventorySummary> bakeEntries(LevelAccessor level, @Nullable BlockPos clothPosToIgnore) {
         InventorySummary input = new InventorySummary();
         InventorySummary output = new InventorySummary();

         for (IntAttached<BlockPos> entry : this.purchases) {
            if (clothPosToIgnore == null || !clothPosToIgnore.equals(entry.getValue())) {
               BlockEntity var8 = level.getBlockEntity((BlockPos)entry.getValue());
               if (var8 instanceof TableClothBlockEntity) {
                  TableClothBlockEntity dcbe = (TableClothBlockEntity)var8;
                  input.add(dcbe.getPaymentItem(), dcbe.getPaymentAmount() * (Integer)entry.getFirst());

                  for (BigItemStack stackEntry : dcbe.requestData.encodedRequest().stacks()) {
                     output.add(stackEntry.stack, stackEntry.count * (Integer)entry.getFirst());
                  }
               }
            }
         }

         return Couple.create(output, input);
      }

      public static class Mutable {
         private final List<IntAttached<BlockPos>> purchases = new ArrayList<>();
         private final UUID shopOwner;
         private final UUID shopNetwork;

         public Mutable(ShoppingListItem.ShoppingList list) {
            this.purchases.addAll(list.purchases);
            this.shopOwner = list.shopOwner;
            this.shopNetwork = list.shopNetwork;
         }

         public void addPurchases(BlockPos clothPos, int amount) {
            for (IntAttached<BlockPos> entry : this.purchases) {
               if (clothPos.equals(entry.getValue())) {
                  entry.setFirst((Integer)entry.getFirst() + amount);
                  return;
               }
            }

            this.purchases.add(IntAttached.with(amount, clothPos));
         }

         public ShoppingListItem.ShoppingList toImmutable() {
            return new ShoppingListItem.ShoppingList(this.purchases, this.shopOwner, this.shopNetwork);
         }
      }
   }
}
