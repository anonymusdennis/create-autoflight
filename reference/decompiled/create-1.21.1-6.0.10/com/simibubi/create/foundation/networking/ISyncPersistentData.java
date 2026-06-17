package com.simibubi.create.foundation.networking;

import com.simibubi.create.AllPackets;
import java.util.HashSet;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface ISyncPersistentData {
   void onPersistentDataUpdated();

   default void syncPersistentDataWithTracking(Entity self) {
      CatnipServices.NETWORK.sendToClientsTrackingEntity(self, new ISyncPersistentData.PersistentDataPacket(self));
   }

   public static record PersistentDataPacket(int entityId, CompoundTag readData) implements ClientboundPacketPayload {
      public static final StreamCodec<FriendlyByteBuf, ISyncPersistentData.PersistentDataPacket> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         ISyncPersistentData.PersistentDataPacket::entityId,
         ByteBufCodecs.COMPOUND_TAG,
         ISyncPersistentData.PersistentDataPacket::readData,
         ISyncPersistentData.PersistentDataPacket::new
      );

      public PersistentDataPacket(Entity entity) {
         this(entity.getId(), entity.getPersistentData());
      }

      @OnlyIn(Dist.CLIENT)
      public void handle(LocalPlayer player) {
         Entity entityByID = player.clientLevel.getEntity(this.entityId);
         CompoundTag data = entityByID.getPersistentData();
         new HashSet(data.getAllKeys()).forEach(data::remove);
         data.merge(this.readData);
         if (entityByID instanceof ISyncPersistentData) {
            ((ISyncPersistentData)entityByID).onPersistentDataUpdated();
         }
      }

      public PacketTypeProvider getTypeProvider() {
         return AllPackets.PERSISTENT_DATA;
      }
   }
}
