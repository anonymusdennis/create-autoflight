package com.simibubi.create.content.equipment.clipboard;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.recipe.ItemCopyingRecipe;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class ClipboardBlockItem extends BlockItem implements ItemCopyingRecipe.SupportsItemCopying {
   public ClipboardBlockItem(Block pBlock, Properties pProperties) {
      super(pBlock, pProperties);
   }

   @NotNull
   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      } else {
         return player.isShiftKeyDown() ? super.useOn(context) : this.use(context.getLevel(), player, context.getHand()).getResult();
      }
   }

   protected boolean updateCustomBlockEntityTag(BlockPos pPos, Level pLevel, Player pPlayer, ItemStack pStack, BlockState pState) {
      if (pLevel.isClientSide()) {
         return false;
      } else if (pLevel.getBlockEntity(pPos) instanceof ClipboardBlockEntity cbe) {
         cbe.notifyUpdate();
         return true;
      } else {
         return false;
      }
   }

   public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
      ItemStack heldItem = player.getItemInHand(hand);
      if (hand == InteractionHand.OFF_HAND) {
         return InteractionResultHolder.pass(heldItem);
      } else {
         player.getCooldowns().addCooldown(heldItem.getItem(), 10);
         if (world.isClientSide) {
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> this.openScreen(player, heldItem.getComponents()));
         }

         ClipboardContent content = (ClipboardContent)heldItem.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY);
         heldItem.set(AllDataComponents.CLIPBOARD_CONTENT, content.setType(ClipboardOverrides.ClipboardType.EDITING));
         return InteractionResultHolder.success(heldItem);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void openScreen(Player player, DataComponentMap components) {
      if (Minecraft.getInstance().player == player) {
         ScreenOpener.open(new ClipboardScreen(player.getInventory().selected, components, null));
      }
   }

   public void registerModelOverrides() {
      CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> ClipboardOverrides.registerModelOverridesClient(this));
   }

   @Override
   public DataComponentType<?> getComponentType() {
      return AllDataComponents.CLIPBOARD_CONTENT;
   }
}
