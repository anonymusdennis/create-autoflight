package com.simibubi.create.content.schematics.packet;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record SchematicPlacePacket(ItemStack stack) implements ServerboundPacketPayload {
   public static final StreamCodec<RegistryFriendlyByteBuf, SchematicPlacePacket> STREAM_CODEC = ItemStack.STREAM_CODEC
      .map(SchematicPlacePacket::new, SchematicPlacePacket::stack);

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.PLACE_SCHEMATIC;
   }

   public void handle(ServerPlayer player) {
      if (player != null) {
         if (player.isCreative()) {
            Level level = player.level();
            SchematicPrinter printer = new SchematicPrinter();
            printer.loadSchematic(this.stack, level, !player.canUseGameMasterBlocks());
            if (printer.isLoaded() && !printer.isErrored()) {
               boolean includeAir = (Boolean)AllConfigs.server().schematics.creativePrintIncludesAir.get();

               while (printer.advanceCurrentPos()) {
                  if (printer.shouldPlaceCurrent(level)) {
                     printer.handleCurrentTarget((pos, state, blockEntity) -> {
                        boolean placingAir = state.isAir();
                        if (!placingAir || includeAir) {
                           CompoundTag data = BlockHelper.prepareBlockEntityData(level, state, blockEntity);
                           BlockHelper.placeSchematicBlock(level, state, pos, null, data);
                        }
                     }, (pos, entity) -> level.addFreshEntity(entity));
                  }
               }
            }
         }
      }
   }
}
