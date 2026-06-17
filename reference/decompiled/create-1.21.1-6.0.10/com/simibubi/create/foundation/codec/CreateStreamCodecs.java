package com.simibubi.create.foundation.codec;

import io.netty.buffer.ByteBuf;
import java.util.Vector;
import java.util.function.BiFunction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamCodec.CodecOperation;

public interface CreateStreamCodecs {
   @Deprecated(
      forRemoval = true
   )
   static <B extends ByteBuf, V> CodecOperation<B, V, Vector<V>> vector() {
      return codec -> ByteBufCodecs.collection(Vector::new, codec);
   }

   @Deprecated(
      forRemoval = true
   )
   static <C> StreamCodec<RegistryFriendlyByteBuf, C> ofLegacyNbtWithRegistries(
      final BiFunction<C, Provider, CompoundTag> writer, final BiFunction<Provider, CompoundTag, C> reader
   ) {
      return new StreamCodec<RegistryFriendlyByteBuf, C>() {
         public C decode(RegistryFriendlyByteBuf buffer) {
            return reader.apply(buffer.registryAccess(), buffer.readNbt());
         }

         public void encode(RegistryFriendlyByteBuf buffer, C value) {
            buffer.writeNbt((Tag)writer.apply(value, buffer.registryAccess()));
         }
      };
   }
}
