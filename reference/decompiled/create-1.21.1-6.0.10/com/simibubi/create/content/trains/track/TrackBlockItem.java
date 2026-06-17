package com.simibubi.create.content.trains.track;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

@EventBusSubscriber({Dist.CLIENT})
public class TrackBlockItem extends BlockItem {
   public TrackBlockItem(Block pBlock, Properties pProperties) {
      super(pBlock, pProperties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
      ItemStack stack = player.getItemInHand(usedHand);
      return player.isShiftKeyDown() && this.isFoil(stack) ? clearSelection(stack, level, player) : super.use(level, player, usedHand);
   }

   public InteractionResult useOn(UseOnContext pContext) {
      ItemStack stack = pContext.getItemInHand();
      BlockPos pos = pContext.getClickedPos();
      Level level = pContext.getLevel();
      BlockState state = level.getBlockState(pos);
      Player player = pContext.getPlayer();
      if (player == null) {
         return super.useOn(pContext);
      } else if (pContext.getHand() == InteractionHand.OFF_HAND) {
         return super.useOn(pContext);
      } else {
         Vec3 lookAngle = player.getLookAngle();
         if (!this.isFoil(stack)) {
            if (state.getBlock() instanceof TrackBlock track && track.getTrackAxes(level, pos, state).size() > 1) {
               if (!level.isClientSide) {
                  player.displayClientMessage(CreateLang.translateDirect("track.junction_start").withStyle(ChatFormatting.RED), true);
               }

               return InteractionResult.SUCCESS;
            }

            if (level.getBlockEntity(pos) instanceof TrackBlockEntity tbe && tbe.isTilted()) {
               if (!level.isClientSide) {
                  player.displayClientMessage(CreateLang.translateDirect("track.turn_start").withStyle(ChatFormatting.RED), true);
               }

               return InteractionResult.SUCCESS;
            }

            if (select(level, pos, lookAngle, stack)) {
               level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75F, 1.0F);
               return InteractionResult.SUCCESS;
            } else {
               return super.useOn(pContext);
            }
         } else if (player.isShiftKeyDown()) {
            return clearSelection(stack, level, player).getResult();
         } else {
            boolean placing = !(state.getBlock() instanceof ITrackBlock);
            boolean extend = (Boolean)stack.getOrDefault(AllDataComponents.TRACK_EXTENDED_CURVE, false);
            stack.remove(AllDataComponents.TRACK_EXTENDED_CURVE);
            if (placing) {
               if (!state.canBeReplaced()) {
                  pos = pos.relative(pContext.getClickedFace());
               }

               state = this.getPlacementState(pContext);
               if (state == null) {
                  return InteractionResult.FAIL;
               }
            }

            ItemStack offhandItem = player.getOffhandItem();
            boolean hasGirder = AllBlocks.METAL_GIRDER.isIn(offhandItem);
            TrackPlacement.PlacementInfo info = TrackPlacement.tryConnect(level, player, pos, state, stack, hasGirder, extend);
            if (info.message != null && !level.isClientSide) {
               player.displayClientMessage(CreateLang.translateDirect(info.message), true);
            }

            if (!info.valid) {
               AllSoundEvents.DENY.playFrom(player, 1.0F, 1.0F);
               return InteractionResult.FAIL;
            } else if (level.isClientSide) {
               return InteractionResult.SUCCESS;
            } else {
               stack = player.getMainHandItem();
               if (AllTags.AllBlockTags.TRACKS.matches(stack)) {
                  stack.remove(AllDataComponents.TRACK_CONNECTING_FROM);
                  stack.remove(AllDataComponents.TRACK_EXTENDED_CURVE);
                  player.setItemInHand(pContext.getHand(), stack);
               }

               SoundType soundtype = state.getSoundType();
               if (soundtype != null) {
                  level.playSound(null, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
               }

               return InteractionResult.SUCCESS;
            }
         }
      }
   }

   public static InteractionResultHolder<ItemStack> clearSelection(ItemStack stack, Level level, Player player) {
      if (level.isClientSide) {
         level.playSound(player, player.blockPosition(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.75F, 1.0F);
      } else {
         player.displayClientMessage(CreateLang.translateDirect("track.selection_cleared"), true);
         stack.remove(AllDataComponents.TRACK_CONNECTING_FROM);
      }

      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }

   public BlockState getPlacementState(UseOnContext pContext) {
      return this.getPlacementState(this.updatePlacementContext(new BlockPlaceContext(pContext)));
   }

   public static boolean select(LevelAccessor world, BlockPos pos, Vec3 lookVec, ItemStack heldItem) {
      BlockState blockState = world.getBlockState(pos);
      if (blockState.getBlock() instanceof ITrackBlock track) {
         Pair<Vec3, AxisDirection> nearestTrackAxis = track.getNearestTrackAxis(world, pos, blockState, lookVec);
         Vec3 axis = ((Vec3)nearestTrackAxis.getFirst()).scale(nearestTrackAxis.getSecond() == AxisDirection.POSITIVE ? -1.0 : 1.0);
         Vec3 end = track.getCurveStart(world, pos, blockState, axis);
         Vec3 normal = track.getUpNormal(world, pos, blockState).normalize();
         heldItem.set(AllDataComponents.TRACK_CONNECTING_FROM, new TrackPlacement.ConnectingFrom(pos, axis, normal, end));
         return true;
      } else {
         return false;
      }
   }

   @SubscribeEvent
   @OnlyIn(Dist.CLIENT)
   public static void sendExtenderPacket(RightClickBlock event) {
      ItemStack stack = event.getItemStack();
      if (event.getLevel().isClientSide) {
         if (AllTags.AllBlockTags.TRACKS.matches(stack)) {
            if (Minecraft.getInstance().options.keySprint.isDown()) {
               CatnipServices.NETWORK.sendToServer(new PlaceExtendedCurvePacket(event.getHand() == InteractionHand.MAIN_HAND, true));
            }
         }
      }
   }

   public boolean isFoil(ItemStack stack) {
      return stack.has(AllDataComponents.TRACK_CONNECTING_FROM);
   }
}
