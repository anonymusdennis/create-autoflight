package net.createmod.catnip.codecs;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;

public interface CatnipCodecUtils {
   static <T> Optional<T> decode(Codec<T> codec, Tag tag) {
      return decode(codec, NbtOps.INSTANCE, tag);
   }

   static <T> Optional<T> decode(Codec<T> codec, Provider registries, Tag tag) {
      return decode(codec, RegistryOps.create(NbtOps.INSTANCE, registries), tag);
   }

   static <T, S> Optional<T> decode(Codec<T> codec, DynamicOps<S> ops, S s) {
      return codec.decode(ops, s).result().map(Pair::getFirst);
   }

   @Nullable
   static <T> T decodeOrNull(Codec<T> codec, Tag tag) {
      return decodeOrNull(codec, NbtOps.INSTANCE, tag);
   }

   @Nullable
   static <T> T decodeOrNull(Codec<T> codec, Provider registries, Tag tag) {
      return decodeOrNull(codec, RegistryOps.create(NbtOps.INSTANCE, registries), tag);
   }

   @Nullable
   static <T, S> T decodeOrNull(Codec<T> codec, DynamicOps<S> ops, S s) {
      return (T)codec.decode(ops, s).mapOrElse(Pair::getFirst, e -> null);
   }

   static <T> Optional<Tag> encode(Codec<T> codec, T t) {
      return encode(codec, NbtOps.INSTANCE, t);
   }

   static <T> Optional<Tag> encode(Codec<T> codec, Provider registries, T t) {
      return encode(codec, RegistryOps.create(NbtOps.INSTANCE, registries), t);
   }

   static <T, S> Optional<S> encode(Codec<T> codec, DynamicOps<S> ops, T t) {
      return codec.encodeStart(ops, t).result();
   }
}
