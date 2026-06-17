package com.simibubi.create.foundation.networking;

import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import com.simibubi.create.foundation.utility.AdventureUtil;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class BlockEntityConfigurationPacket<BE extends SyncedBlockEntity> implements ServerboundPacketPayload {
   protected final BlockPos pos;

   public BlockEntityConfigurationPacket(BlockPos pos) {
      this.pos = pos;
   }

   public void handle(ServerPlayer player) {
      if (player != null && !player.isSpectator() && !AdventureUtil.isAdventure(player)) {
         Level world = player.level();
         if (world.isLoaded(this.pos)) {
            if (player.canInteractWithBlock(this.pos, (double)this.maxRange())) {
               BlockEntity blockEntity = world.getBlockEntity(this.pos);
               if (blockEntity instanceof SyncedBlockEntity) {
                  this.applySettings(player, (BE)blockEntity);
                  if (!this.causeUpdate()) {
                     return;
                  }

                  ((SyncedBlockEntity)blockEntity).sendData();
                  blockEntity.setChanged();
               }
            }
         }
      }
   }

   protected int maxRange() {
      return 20;
   }

   protected boolean causeUpdate() {
      return true;
   }

   protected abstract void applySettings(ServerPlayer var1, BE var2);
}
