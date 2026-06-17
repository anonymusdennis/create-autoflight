package net.createmod.catnip.codecs.stream;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamCodec.CodecOperation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CatnipStreamCodecBuilders {
   static <T extends ByteBuf, S extends Enum<S>> StreamCodec<T, S> ofEnum(final Class<S> clazz) {
      return new StreamCodec<T, S>() {
         @NotNull
         public S decode(@NotNull T buffer) {
            return clazz.getEnumConstants()[VarInt.read(buffer)];
         }

         public void encode(@NotNull T buffer, @NotNull S value) {
            VarInt.write(buffer, value.ordinal());
         }
      };
   }

   static <B extends ByteBuf, L, R> StreamCodec<B, Pair<L, R>> pair(final StreamCodec<B, L> codecL, final StreamCodec<B, R> codecR) {
      return new StreamCodec<B, Pair<L, R>>() {
         @NotNull
         public Pair<L, R> decode(B buffer) {
            L l = (L)codecL.decode(buffer);
            R r = (R)codecR.decode(buffer);
            return Pair.of(l, r);
         }

         public void encode(B buffer, Pair<L, R> value) {
            codecL.encode(buffer, value.getFirst());
            codecR.encode(buffer, value.getSecond());
         }
      };
   }

   static <B extends ByteBuf, V> CodecOperation<B, V, Optional<V>> optional() {
      return ByteBufCodecs::optional;
   }

   static <B extends ByteBuf, V> StreamCodec<B, V> nullable(final StreamCodec<B, V> base) {
      return new StreamCodec<B, V>() {
         @Nullable
         public V decode(@NotNull B buffer) {
            return (V)FriendlyByteBuf.readNullable(buffer, base);
         }

         public void encode(@NotNull B buffer, @Nullable V value) {
            FriendlyByteBuf.writeNullable(buffer, value, base);
         }
      };
   }

   static <B extends ByteBuf, V> CodecOperation<B, V, V> nullable() {
      return CatnipStreamCodecBuilders::nullable;
   }

   static <B extends ByteBuf, V> StreamCodec<B, List<V>> list(StreamCodec<B, V> base) {
      return base.apply(ByteBufCodecs.list());
   }

   static <B extends ByteBuf, V> StreamCodec<B, List<V>> list(StreamCodec<B, V> base, int maxSize) {
      return base.apply(ByteBufCodecs.list(maxSize));
   }

   static <B extends ByteBuf, V> CodecOperation<B, V, NonNullList<V>> nonNullList() {
      return streamCodec -> ByteBufCodecs.collection(NonNullList::createWithCapacity, streamCodec);
   }

   static <B extends ByteBuf, V> CodecOperation<B, V, NonNullList<V>> nonNullList(int maxSize) {
      return streamCodec -> ByteBufCodecs.collection(NonNullList::createWithCapacity, streamCodec, maxSize);
   }

   static <B extends ByteBuf, V> StreamCodec<B, NonNullList<V>> nonNullList(StreamCodec<B, V> base) {
      return base.apply(nonNullList());
   }

   static <B extends ByteBuf, V> StreamCodec<B, NonNullList<V>> nonNullList(StreamCodec<B, V> base, int maxSize) {
      return base.apply(nonNullList(maxSize));
   }

   static <B extends FriendlyByteBuf, V> StreamCodec<B, V[]> array(final StreamCodec<? super B, V> base, final Class<?> clazz) {
      return new StreamCodec<B, V[]>() {
         @NotNull
         public V[] decode(@NotNull B buffer) {
            int size = buffer.readVarInt();
            V[] array = (V[])((Object[])Array.newInstance(clazz, size));

            for (int i = 0; i < size; i++) {
               array[i] = (V)base.decode(buffer);
            }

            return array;
         }

         public void encode(@NotNull B buffer, @NotNull V[] value) {
            buffer.writeVarInt(value.length);

            for (V v : value) {
               base.encode(buffer, v);
            }
         }
      };
   }

   static <B extends FriendlyByteBuf, V> CodecOperation<B, V, V[]> array(Class<?> clazz) {
      return streamCodec -> array(streamCodec, clazz);
   }

   static <T> StreamCodec<ByteBuf, TagKey<T>> tagKey(ResourceKey<? extends Registry<T>> registry) {
      return ResourceLocation.STREAM_CODEC.map(id -> TagKey.create(registry, id), TagKey::location);
   }
}
