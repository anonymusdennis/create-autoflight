package com.simibubi.create.content.redstone.displayLink;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.CreateLang;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

@EventBusSubscriber
public abstract class ClickToLinkBlockItem extends BlockItem {
   private static BlockPos lastShownPos = null;
   private static AABB lastShownAABB = null;

   public ClickToLinkBlockItem(Block pBlock, Properties pProperties) {
      super(pBlock, pProperties);
   }

   @SubscribeEvent
   public static void linkableItemAlwaysPlacesWhenUsed(RightClickBlock event) {
      ItemStack usedItem = event.getItemStack();
      if (usedItem.getItem() instanceof ClickToLinkBlockItem blockItem) {
         if (!event.getLevel().getBlockState(event.getPos()).is(blockItem.getBlock())) {
            event.setUseBlock(TriState.FALSE);
         }
      }
   }

   public InteractionResult useOn(UseOnContext pContext) {
      ItemStack stack = pContext.getItemInHand();
      BlockPos pos = pContext.getClickedPos();
      Level level = pContext.getLevel();
      BlockState state = level.getBlockState(pos);
      Player player = pContext.getPlayer();
      String msgKey = this.getMessageTranslationKey();
      int maxDistance = this.getMaxDistanceFromSelection();
      if (player == null) {
         return InteractionResult.FAIL;
      } else if (!player.isShiftKeyDown() || !stack.has(AllDataComponents.CLICK_TO_LINK_DATA)) {
         ResourceLocation placedDim = level.dimension().location();
         if (!stack.has(AllDataComponents.CLICK_TO_LINK_DATA)) {
            if (!this.isValidTarget(level, pos)) {
               if (this.placeWhenInvalid()) {
                  InteractionResult useOn = super.useOn(pContext);
                  if (!level.isClientSide && useOn != InteractionResult.FAIL) {
                     ItemStack itemInHand = player.getItemInHand(pContext.getHand());
                     if (!itemInHand.isEmpty()) {
                        stack.remove(AllDataComponents.CLICK_TO_LINK_DATA);
                        stack.remove(DataComponents.BLOCK_ENTITY_DATA);
                     }

                     return useOn;
                  } else {
                     return useOn;
                  }
               } else {
                  if (level.isClientSide) {
                     AllSoundEvents.DENY.playFrom(player);
                  }

                  player.displayClientMessage(CreateLang.translateDirect(msgKey + ".invalid"), true);
                  return InteractionResult.FAIL;
               }
            } else if (level.isClientSide) {
               return InteractionResult.SUCCESS;
            } else {
               player.displayClientMessage(CreateLang.translateDirect(msgKey + ".set"), true);
               stack.set(AllDataComponents.CLICK_TO_LINK_DATA, new ClickToLinkBlockItem.ClickToLinkData(pos, placedDim));
               return InteractionResult.SUCCESS;
            }
         } else {
            ClickToLinkBlockItem.ClickToLinkData data = (ClickToLinkBlockItem.ClickToLinkData)stack.get(AllDataComponents.CLICK_TO_LINK_DATA);
            BlockPos selectedPos = data.selectedPos();
            ResourceLocation selectedDim = data.selectedDim();
            BlockPos placedPos = pos.relative(pContext.getClickedFace(), state.canBeReplaced() ? 0 : 1);
            if (maxDistance == -1 || selectedPos.closerThan(placedPos, (double)maxDistance) && selectedDim.equals(placedDim)) {
               CompoundTag beTag = new CompoundTag();
               beTag.put("TargetOffset", NbtUtils.writeBlockPos(selectedPos.subtract(placedPos)));
               NBTHelper.writeResourceLocation(beTag, "TargetDimension", selectedDim);
               BlockEntity.addEntityType(beTag, ((IBE)this.getBlock()).getBlockEntityType());
               stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(beTag));
               InteractionResult useOn = super.useOn(pContext);
               if (!level.isClientSide && useOn != InteractionResult.FAIL) {
                  ItemStack itemInHand = player.getItemInHand(pContext.getHand());
                  if (!itemInHand.isEmpty()) {
                     stack.remove(AllDataComponents.CLICK_TO_LINK_DATA);
                     stack.remove(DataComponents.BLOCK_ENTITY_DATA);
                  }

                  player.displayClientMessage(CreateLang.translateDirect(msgKey + ".success").withStyle(ChatFormatting.GREEN), true);
                  return useOn;
               } else {
                  return useOn;
               }
            } else {
               player.displayClientMessage(CreateLang.translateDirect(msgKey + ".too_far").withStyle(ChatFormatting.RED), true);
               return InteractionResult.FAIL;
            }
         }
      } else if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         player.displayClientMessage(CreateLang.translateDirect(msgKey + ".clear"), true);
         stack.remove(AllDataComponents.CLICK_TO_LINK_DATA);
         stack.remove(DataComponents.BLOCK_ENTITY_DATA);
         return InteractionResult.SUCCESS;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static void clientTick() {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack heldItemMainhand = player.getMainHandItem();
         if (heldItemMainhand.getItem() instanceof ClickToLinkBlockItem blockItem) {
            if (heldItemMainhand.has(AllDataComponents.CLICK_TO_LINK_DATA)) {
               BlockPos selectedPos = ((ClickToLinkBlockItem.ClickToLinkData)heldItemMainhand.get(AllDataComponents.CLICK_TO_LINK_DATA)).selectedPos();
               if (!selectedPos.equals(lastShownPos)) {
                  lastShownAABB = blockItem.getSelectionBounds(selectedPos);
                  lastShownPos = selectedPos;
               }

               Outliner.getInstance().showAABB("target", lastShownAABB).colored(16763764).lineWidth(0.0625F);
            }
         }
      }
   }

   public abstract int getMaxDistanceFromSelection();

   public abstract String getMessageTranslationKey();

   public boolean placeWhenInvalid() {
      return false;
   }

   public boolean isValidTarget(LevelAccessor level, BlockPos pos) {
      return true;
   }

   @OnlyIn(Dist.CLIENT)
   public AABB getSelectionBounds(BlockPos pos) {
      Level world = Minecraft.getInstance().level;
      BlockState state = world.getBlockState(pos);
      VoxelShape shape = state.getShape(world, pos);
      return shape.isEmpty() ? new AABB(BlockPos.ZERO) : shape.bounds().move(pos);
   }

   public static record ClickToLinkData(BlockPos selectedPos, ResourceLocation selectedDim) {
      public static final Codec<ClickToLinkBlockItem.ClickToLinkData> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  BlockPos.CODEC.fieldOf("selected_pos").forGetter(ClickToLinkBlockItem.ClickToLinkData::selectedPos),
                  ResourceLocation.CODEC.fieldOf("selected_dim").forGetter(ClickToLinkBlockItem.ClickToLinkData::selectedDim)
               )
               .apply(instance, ClickToLinkBlockItem.ClickToLinkData::new)
      );
      public static final StreamCodec<ByteBuf, ClickToLinkBlockItem.ClickToLinkData> STREAM_CODEC = StreamCodec.composite(
         BlockPos.STREAM_CODEC,
         ClickToLinkBlockItem.ClickToLinkData::selectedPos,
         ResourceLocation.STREAM_CODEC,
         ClickToLinkBlockItem.ClickToLinkData::selectedDim,
         ClickToLinkBlockItem.ClickToLinkData::new
      );
   }
}
