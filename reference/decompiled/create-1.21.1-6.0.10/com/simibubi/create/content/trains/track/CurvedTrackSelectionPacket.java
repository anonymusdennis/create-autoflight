package com.simibubi.create.content.trains.track;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableObject;

public class CurvedTrackSelectionPacket extends BlockEntityConfigurationPacket<TrackBlockEntity> {
   public static final StreamCodec<ByteBuf, CurvedTrackSelectionPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      packet -> packet.pos,
      BlockPos.STREAM_CODEC,
      packet -> packet.targetPos,
      ByteBufCodecs.BOOL,
      packet -> packet.front,
      ByteBufCodecs.VAR_INT,
      packet -> packet.segment,
      ByteBufCodecs.VAR_INT,
      packet -> packet.slot,
      CurvedTrackSelectionPacket::new
   );
   private final BlockPos targetPos;
   private final boolean front;
   private final int segment;
   private final int slot;

   public CurvedTrackSelectionPacket(BlockPos pos, BlockPos targetPos, boolean front, int segment, int slot) {
      super(pos);
      this.targetPos = targetPos;
      this.front = front;
      this.segment = segment;
      this.slot = slot;
   }

   protected void applySettings(ServerPlayer player, TrackBlockEntity be) {
      if (player.getInventory().selected == this.slot) {
         ItemStack stack = player.getInventory().getItem(this.slot);
         if (stack.getItem() instanceof TrackTargetingBlockItem) {
            if (player.isShiftKeyDown() && stack.has(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS)) {
               player.displayClientMessage(CreateLang.translateDirect("track_target.clear"), true);
               stack.remove(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS);
               stack.remove(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_DIRECTION);
               stack.remove(AllDataComponents.TRACK_TARGETING_ITEM_BEZIER);
               AllSoundEvents.CONTROLLER_CLICK.play(player.level(), null, this.pos, 1.0F, 0.5F);
            } else {
               EdgePointType<?> type = AllBlocks.TRACK_SIGNAL.isIn(stack) ? EdgePointType.SIGNAL : EdgePointType.STATION;
               MutableObject<TrackTargetingBlockItem.OverlapResult> result = new MutableObject(null);
               BezierTrackPointLocation bezierTrackPointLocation = new BezierTrackPointLocation(this.targetPos, this.segment);
               TrackTargetingBlockItem.withGraphLocation(
                  player.level(), this.pos, this.front, bezierTrackPointLocation, type, (overlap, location) -> result.setValue(overlap)
               );
               if (((TrackTargetingBlockItem.OverlapResult)result.getValue()).feedback != null) {
                  player.displayClientMessage(
                     CreateLang.translateDirect(((TrackTargetingBlockItem.OverlapResult)result.getValue()).feedback).withStyle(ChatFormatting.RED), true
                  );
                  AllSoundEvents.DENY.play(player.level(), null, this.pos, 0.5F, 1.0F);
               } else {
                  stack.set(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS, this.pos);
                  stack.set(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_DIRECTION, this.front);
                  stack.set(AllDataComponents.TRACK_TARGETING_ITEM_BEZIER, bezierTrackPointLocation);
                  player.displayClientMessage(CreateLang.translateDirect("track_target.set"), true);
                  AllSoundEvents.CONTROLLER_CLICK.play(player.level(), null, this.pos, 1.0F, 1.0F);
               }
            }
         }
      }
   }

   @Override
   protected int maxRange() {
      return (Integer)AllConfigs.server().trains.maxTrackPlacementLength.get() + 16;
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.SELECT_CURVED_TRACK;
   }
}
