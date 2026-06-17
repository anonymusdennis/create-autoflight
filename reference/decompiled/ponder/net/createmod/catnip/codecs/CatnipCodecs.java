package net.createmod.catnip.codecs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import java.util.Set;

public interface CatnipCodecs {
   PrimitiveCodec<Character> CHAR = new PrimitiveCodec<Character>() {
      public <T> DataResult<Character> read(DynamicOps<T> ops, T input) {
         return ops.getNumberValue(input).map(n -> (char)n.intValue());
      }

      public <T> T write(DynamicOps<T> ops, Character value) {
         return (T)ops.createInt(value);
      }

      @Override
      public String toString() {
         return "Char";
      }
   };

   static <E> Codec<Set<E>> set(Codec<E> codec) {
      return Codec.list(codec).xmap(ImmutableSet::copyOf, ImmutableList::copyOf);
   }
}
