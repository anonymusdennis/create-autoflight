package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.utility.RaycastHelper;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.mixin.accessor.RedstoneLinkBlockEntityAccessor;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

public class LinkedTypewriterItem extends BlockItem {
   public LinkedTypewriterItem(Block block, Properties properties) {
      super(block, properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      BlockPos clickedPos = context.getClickedPos();
      Couple<Frequency> frequency = null;
      BlockEntity be = level.getBlockEntity(clickedPos);
      if (be instanceof AbstractLinkedReceiverBlockEntity abe) {
         frequency = abe.getFrequency();
      } else if (be instanceof RedstoneLinkBlockEntity lbe) {
         frequency = ((RedstoneLinkBlockEntityAccessor)lbe).getLink().getNetworkKey();
      }

      if (frequency != null) {
         if (!level.isClientSide) {
            return InteractionResult.CONSUME;
         } else if (LinkedTypewriterInteractionHandler.getMode() == LinkedTypewriterInteractionHandler.Mode.BINDING_FROM_ITEM) {
            LinkedTypewriterItemBindHandler.reset();
            return InteractionResult.CONSUME;
         } else {
            LinkedTypewriterInteractionHandler.setMode(LinkedTypewriterInteractionHandler.Mode.BINDING_FROM_ITEM);
            LinkedTypewriterItemBindHandler.setClickedPos(clickedPos);
            return InteractionResult.SUCCESS;
         }
      } else {
         if (level.isClientSide) {
            LinkedTypewriterItemBindHandler.reset();
         }

         return super.useOn(context);
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
      BlockHitResult blockHitResult = RaycastHelper.rayTraceRange(level, player, player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE));
      if (blockHitResult.getType() == Type.MISS && level.isClientSide) {
         LinkedTypewriterItemBindHandler.reset();
      }

      return super.use(level, player, usedHand);
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
      if (stack.has(DataComponents.BLOCK_ENTITY_DATA)) {
         CompoundTag tag = ((CustomData)stack.get(DataComponents.BLOCK_ENTITY_DATA)).copyTag();
         if (tag.contains("Keys", 9)) {
            int keyCount = tag.getList("Keys", 10).size();
            tooltipComponents.add(Component.translatable("simulated.linked_typewriter.key_count", new Object[]{keyCount}).withStyle(ChatFormatting.GOLD));
         }
      }
   }
}
