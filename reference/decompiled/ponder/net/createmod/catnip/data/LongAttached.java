package net.createmod.catnip.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Comparator;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class LongAttached<V> extends Pair<Long, V> {
   protected LongAttached(Long first, V second) {
      super(first, second);
   }

   public static <V> LongAttached<V> with(long number, V value) {
      return new LongAttached<>(number, value);
   }

   public static <V> LongAttached<V> withZero(V value) {
      return new LongAttached<>(0L, value);
   }

   public boolean isZero() {
      return this.first == 0L;
   }

   public boolean exceeds(long value) {
      return this.first > value;
   }

   public boolean isOrBelowZero() {
      return this.first <= 0L;
   }

   public void increment() {
      Long var1 = this.first;
      this.first = this.first + 1L;
   }

   public void decrement() {
      Long var1 = this.first;
      this.first = this.first - 1L;
   }

   public V getValue() {
      return this.getSecond();
   }

   public CompoundTag serializeNBT(Function<V, CompoundTag> serializer) {
      CompoundTag nbt = new CompoundTag();
      nbt.put("Item", (Tag)serializer.apply(this.getValue()));
      nbt.putLong("Location", this.getFirst());
      return nbt;
   }

   public static Comparator<? super LongAttached<?>> comparator() {
      return (i1, i2) -> Long.compare(i2.getFirst(), i1.getFirst());
   }

   public static <T> LongAttached<T> read(CompoundTag nbt, Function<CompoundTag, T> deserializer) {
      return with(nbt.getLong("Location"), deserializer.apply(nbt.getCompound("Item")));
   }

   public static <T> Codec<LongAttached<T>> codec(Codec<T> codec) {
      return RecordCodecBuilder.create(
         instance -> instance.group(Codec.LONG.fieldOf("first").forGetter(Pair::getFirst), codec.fieldOf("second").forGetter(Pair::getSecond))
               .apply(instance, LongAttached::new)
      );
   }

   public static <B extends ByteBuf, T> StreamCodec<B, LongAttached<T>> streamCodec(StreamCodec<? super B, T> codec) {
      return StreamCodec.composite(ByteBufCodecs.VAR_LONG, Pair::getFirst, codec, Pair::getSecond, LongAttached::new);
   }
}
