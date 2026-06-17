package com.simibubi.create.content.schematics.cannon;

import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public record ConfigureSchematicannonPacket(ConfigureSchematicannonPacket.Option option, boolean set) implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, ConfigureSchematicannonPacket> STREAM_CODEC = StreamCodec.composite(
      ConfigureSchematicannonPacket.Option.STREAM_CODEC,
      ConfigureSchematicannonPacket::option,
      ByteBufCodecs.BOOL,
      ConfigureSchematicannonPacket::set,
      ConfigureSchematicannonPacket::new
   );

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.CONFIGURE_SCHEMATICANNON;
   }

   public void handle(ServerPlayer player) {
      if (player != null && player.containerMenu instanceof SchematicannonMenu) {
         SchematicannonBlockEntity be = ((SchematicannonMenu)player.containerMenu).contentHolder;
         switch (this.option) {
            case DONT_REPLACE:
            case REPLACE_SOLID:
            case REPLACE_ANY:
            case REPLACE_EMPTY:
               be.replaceMode = this.option.ordinal();
               break;
            case SKIP_MISSING:
               be.skipMissing = this.set;
               break;
            case SKIP_BLOCK_ENTITIES:
               be.replaceBlockEntities = this.set;
               break;
            case PLAY:
               be.state = SchematicannonBlockEntity.State.RUNNING;
               be.statusMsg = "running";
               break;
            case PAUSE:
               be.state = SchematicannonBlockEntity.State.PAUSED;
               be.statusMsg = "paused";
               break;
            case STOP:
               be.state = SchematicannonBlockEntity.State.STOPPED;
               be.statusMsg = "stopped";
         }

         be.sendUpdate = true;
      }
   }

   public static enum Option {
      DONT_REPLACE,
      REPLACE_SOLID,
      REPLACE_ANY,
      REPLACE_EMPTY,
      SKIP_MISSING,
      SKIP_BLOCK_ENTITIES,
      PLAY,
      PAUSE,
      STOP;

      public static final StreamCodec<ByteBuf, ConfigureSchematicannonPacket.Option> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(
         ConfigureSchematicannonPacket.Option.class
      );
   }
}
