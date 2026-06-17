package dev.eriksonn.aeronautics.network.packets;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.CrystalPropagationContext;
import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeBlendHelper;
import dev.eriksonn.aeronautics.index.AeroLevititeBlendPropagationContexts;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.eriksonn.aeronautics.util.CatalyzerHelper;
import foundry.veil.api.network.handler.ServerPacketContext;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public record LevititeCatalystCrystallizationPacket(BlockPos pos, InteractionHand hand) implements CustomPacketPayload {
   public static final Type<LevititeCatalystCrystallizationPacket> TYPE = new Type(Aeronautics.path("levitite_blend_crystallize"));
   public static final StreamCodec<RegistryFriendlyByteBuf, LevititeCatalystCrystallizationPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      LevititeCatalystCrystallizationPacket::pos,
      CatnipStreamCodecs.HAND,
      LevititeCatalystCrystallizationPacket::hand,
      LevititeCatalystCrystallizationPacket::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public void handle(ServerPacketContext context) {
      ServerPlayer player = context.player();
      ItemStack item = player.getItemInHand(this.hand);
      if (CatalyzerHelper.isCatalyzer(item)) {
         if (!item.is(AeroTags.ItemTags.LEVITITE_CATALYZER_NO_CONSUME)) {
            if (item.isDamageableItem()) {
               item.hurtAndBreak(1, player, this.hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            } else if (item.isStackable() && !context.player().hasInfiniteMaterials()) {
               item.shrink(1);
            }
         }

         player.swing(this.hand);
         CrystalPropagationContext itemContext = item.is(AeroTags.ItemTags.LEVITITE_SOUL_CATALYZER)
            ? (CrystalPropagationContext)AeroLevititeBlendPropagationContexts.SOUL_CONTEXT.get()
            : (CrystalPropagationContext)AeroLevititeBlendPropagationContexts.STANDARD_CONTEXT.get();
         LevititeBlendHelper.addLevititeBlendTicker(context.level(), this.pos, false, false, itemContext.getContextForSpread(context.level(), this.pos));
      }
   }
}
