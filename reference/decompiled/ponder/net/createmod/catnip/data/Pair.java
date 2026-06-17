package net.createmod.catnip.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.network.codec.StreamCodec;

public class Pair<F, S> {
   F first;
   S second;

   protected Pair(F first, S second) {
      this.first = first;
      this.second = second;
   }

   public static <F, S> Pair<F, S> of(F first, S second) {
      return new Pair<>(first, second);
   }

   public F getFirst() {
      return this.first;
   }

   public S getSecond() {
      return this.second;
   }

   public void setFirst(F first) {
      this.first = first;
   }

   public void setSecond(S second) {
      this.second = second;
   }

   public Pair<F, S> copy() {
      return of(this.first, this.second);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else {
         return !(obj instanceof Pair<?, ?> other) ? false : Objects.equals(this.first, other.first) && Objects.equals(this.second, other.second);
      }
   }

   @Override
   public int hashCode() {
      return this.nullHash(this.first) * 31 ^ this.nullHash(this.second);
   }

   int nullHash(Object o) {
      return o == null ? 0 : o.hashCode();
   }

   @Override
   public String toString() {
      return "(" + this.first + ", " + this.second + ")";
   }

   public Pair<S, F> swap() {
      return of(this.second, this.first);
   }

   public static <F, S> Codec<Pair<F, S>> codec(Codec<F> firstCodec, Codec<S> secondCodec) {
      return RecordCodecBuilder.create(
         instance -> instance.group(firstCodec.fieldOf("first").forGetter(Pair::getFirst), secondCodec.fieldOf("second").forGetter(Pair::getSecond))
               .apply(instance, Pair::new)
      );
   }

   public static <B, F, S> StreamCodec<B, Pair<F, S>> streamCodec(StreamCodec<? super B, F> firstCodec, StreamCodec<? super B, S> secondCodec) {
      return StreamCodec.composite(firstCodec, Pair::getFirst, secondCodec, Pair::getSecond, Pair::new);
   }
}
