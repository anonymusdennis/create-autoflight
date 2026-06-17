package net.createmod.catnip.net.packets;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import net.createmod.catnip.config.ui.ConfigHelper;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.ponder.Ponder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

public class ServerboundConfigPacket<T> implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, ServerboundConfigPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, p -> p.modID, ByteBufCodecs.STRING_UTF8, p -> p.path, ByteBufCodecs.STRING_UTF8, p -> p.value, ServerboundConfigPacket::new
   );
   private final String modID;
   private final String path;
   private final String value;

   public ServerboundConfigPacket(String modID, String path, T value) {
      this.modID = Objects.requireNonNull(modID);
      this.path = path;
      this.value = this.serialize(value);
   }

   public ServerboundConfigPacket(String modID, String path, String serialized) {
      this.modID = Objects.requireNonNull(modID);
      this.path = path;
      this.value = serialized;
   }

   @Override
   public BasePacketPayload.PacketTypeProvider getTypeProvider() {
      return CatnipPackets.SERVERBOUND_CONFIG;
   }

   @Override
   public void handle(ServerPlayer player) {
      try {
         if (!player.hasPermissions(2)) {
            return;
         }

         ModConfigSpec spec = ConfigHelper.findModConfigSpecFor(Type.SERVER, this.modID);
         ValueSpec valueSpec = (ValueSpec)spec.getSpec().getRaw(this.path);
         ConfigValue<T> configValue = (ConfigValue<T>)spec.getValues().get(this.path);
         T v = (T)deserialize(configValue.get(), this.value);
         if (!valueSpec.test(v)) {
            return;
         }

         configValue.set(v);
         configValue.save();
      } catch (Exception var6) {
         Ponder.LOGGER.warn("Unable to handle ConfigureConfig Packet. ", var6);
      }
   }

   public String serialize(T value) {
      if (value instanceof Boolean) {
         return Boolean.toString((Boolean)value);
      } else if (value instanceof Enum) {
         return ((Enum)value).name();
      } else if (value instanceof Integer) {
         return Integer.toString((Integer)value);
      } else if (value instanceof Float) {
         return Float.toString((Float)value);
      } else if (value instanceof Double) {
         return Double.toString((Double)value);
      } else {
         throw new IllegalArgumentException("unknown type " + value + ": " + value.getClass().getSimpleName());
      }
   }

   public static Object deserialize(Object type, String sValue) {
      if (type instanceof Boolean) {
         return Boolean.parseBoolean(sValue);
      } else if (type instanceof Enum) {
         return Enum.valueOf(((Enum)type).getClass(), sValue);
      } else if (type instanceof Integer) {
         return Integer.parseInt(sValue);
      } else if (type instanceof Float) {
         return Float.parseFloat(sValue);
      } else if (type instanceof Double) {
         return Double.parseDouble(sValue);
      } else {
         throw new IllegalArgumentException("unknown type " + type + ": " + type.getClass().getSimpleName());
      }
   }
}
