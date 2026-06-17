package dev.ryanhcode.sable.companion.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Quaterniond;
import org.joml.Vector3d;

@Internal
public final class SableCompanionUtil {
   public static final Codec<Vector3d> VECTOR_3D_CODEC = Codec.DOUBLE
      .listOf()
      .comapFlatMap(
         l -> fixedSize(l, 3).map(list -> new Vector3d((Double)list.getFirst(), (Double)list.get(1), (Double)list.get(2))), vec -> List.of(vec.x, vec.y, vec.z)
      );
   public static final Codec<Quaterniond> QUATERNIOND_CODEC = Codec.DOUBLE
      .listOf()
      .comapFlatMap(
         l -> fixedSize(l, 4).map(list -> new Quaterniond((Double)list.getFirst(), (Double)list.get(1), (Double)list.get(2), (Double)list.get(3))),
         quat -> List.of(quat.x, quat.y, quat.z, quat.w)
      );

   public static <T> DataResult<List<T>> fixedSize(List<T> list, int size) {
      if (list.size() != size) {
         Supplier<String> supplier = () -> "Input is not a list of " + size + " elements";
         return list.size() >= size ? DataResult.error(supplier, list.subList(0, size)) : DataResult.error(supplier);
      } else {
         return DataResult.success(list);
      }
   }
}
