package com.simibubi.create.content.equipment.blueprint;

import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record BlueprintAssignCompleteRecipePacket(ResourceLocation recipeId) implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, BlueprintAssignCompleteRecipePacket> STREAM_CODEC = ResourceLocation.STREAM_CODEC
      .map(BlueprintAssignCompleteRecipePacket::new, BlueprintAssignCompleteRecipePacket::recipeId);

   public void handle(ServerPlayer player) {
      if (player.containerMenu instanceof BlueprintMenu c) {
         player.level()
            .getRecipeManager()
            .byKey(this.recipeId)
            .ifPresent(r -> BlueprintItem.assignCompleteRecipe(c.player.level(), c.ghostInventory, r.value()));
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.BLUEPRINT_COMPLETE_RECIPE;
   }
}
