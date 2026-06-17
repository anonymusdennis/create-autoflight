package com.simibubi.create.foundation.blockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class SyncedBlockEntity extends BlockEntity {
   public SyncedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.writeClient(new CompoundTag(), registries);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public void handleUpdateTag(CompoundTag tag, Provider registries) {
      this.readClient(tag, registries);
   }

   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, Provider registries) {
      CompoundTag tag = pkt.getTag();
      this.readClient(tag == null ? new CompoundTag() : tag, registries);
   }

   public void readClient(CompoundTag tag, Provider registries) {
      this.loadAdditional(tag, registries);
   }

   public CompoundTag writeClient(CompoundTag tag, Provider registries) {
      this.saveAdditional(tag, registries);
      return tag;
   }

   public void sendData() {
      if (this.level instanceof ServerLevel serverLevel) {
         serverLevel.getChunkSource().blockChanged(this.getBlockPos());
      }
   }

   public void notifyUpdate() {
      this.setChanged();
      this.sendData();
   }

   public HolderGetter<Block> blockHolderGetter() {
      return (HolderGetter<Block>)(this.level != null ? this.level.holderLookup(Registries.BLOCK) : BuiltInRegistries.BLOCK.asLookup());
   }
}
