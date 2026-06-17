package com.simibubi.create.content.kinetics.chainConveyor;

import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ChainConveyorConnectionPacket extends BlockEntityConfigurationPacket<ChainConveyorBlockEntity> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ChainConveyorConnectionPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      packet -> packet.pos,
      BlockPos.STREAM_CODEC,
      packet -> packet.targetPos,
      ItemStack.STREAM_CODEC,
      packet -> packet.chain,
      ByteBufCodecs.BOOL,
      packet -> packet.connect,
      ChainConveyorConnectionPacket::new
   );
   private final BlockPos targetPos;
   private final ItemStack chain;
   private final boolean connect;

   public ChainConveyorConnectionPacket(BlockPos pos, BlockPos targetPos, ItemStack chain, boolean connect) {
      super(pos);
      this.targetPos = targetPos;
      this.chain = chain;
      this.connect = connect;
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.CHAIN_CONVEYOR_CONNECT;
   }

   @Override
   protected int maxRange() {
      return (Integer)AllConfigs.server().kinetics.maxChainConveyorLength.get() + 16;
   }

   protected void applySettings(ServerPlayer player, ChainConveyorBlockEntity be) {
      if (be.getBlockPos().closerThan(this.targetPos, (double)(this.maxRange() - 16 + 1))) {
         if (be.getLevel().getBlockEntity(this.targetPos) instanceof ChainConveyorBlockEntity clbe) {
            if (this.connect && !player.isCreative()) {
               int chainCost = ChainConveyorBlockEntity.getChainCost(this.targetPos.subtract(be.getBlockPos()));
               boolean hasEnough = ChainConveyorBlockEntity.getChainsFromInventory(player, this.chain, chainCost, true);
               if (!hasEnough) {
                  return;
               }

               ChainConveyorBlockEntity.getChainsFromInventory(player, this.chain, chainCost, false);
            }

            if (!this.connect) {
               if (!player.isCreative()) {
                  for (int chainCost = ChainConveyorBlockEntity.getChainCost(this.targetPos.subtract(this.pos)); chainCost > 0; chainCost -= 64) {
                     player.getInventory().placeItemBackInInventory(new ItemStack(Items.CHAIN, Math.min(chainCost, 64)));
                  }
               }

               be.chainDestroyed(this.targetPos.subtract(be.getBlockPos()), false, true);
               be.getLevel().playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS);
            }

            if (this.connect) {
               if (!clbe.addConnectionTo(be.getBlockPos())) {
                  return;
               }
            } else {
               clbe.removeConnectionTo(be.getBlockPos());
            }

            if (this.connect) {
               if (!be.addConnectionTo(this.targetPos)) {
                  clbe.removeConnectionTo(be.getBlockPos());
               }
            } else {
               be.removeConnectionTo(this.targetPos);
            }
         }
      }
   }
}
