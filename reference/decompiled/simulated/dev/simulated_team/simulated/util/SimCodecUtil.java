package dev.simulated_team.simulated.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup.PointForce;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.util.SableBufferUtils;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SimCodecUtil {
   public static final StreamCodec<ByteBuf, Vector3d> STREAM_VECTOR3D = StreamCodec.of(SableBufferUtils::write, x -> SableBufferUtils.read(x, new Vector3d()));
   public static final StreamCodec<ByteBuf, Vector3dc> STREAM_VECTOR3DC = ByteBufCodecs.DOUBLE
      .apply(ByteBufCodecs.list(3))
      .map(l -> new Vector3d((Double)l.getFirst(), (Double)l.get(1), (Double)l.get(2)), v -> List.of(v.x(), v.y(), v.z()));
   public static final StreamCodec<ByteBuf, BoundingBox3d> BOUNDING_BOX_3D_STREAM_CODEC = ByteBufCodecs.DOUBLE
      .apply(ByteBufCodecs.list(6))
      .map(
         l -> new BoundingBox3d((Double)l.getFirst(), (Double)l.get(1), (Double)l.get(2), (Double)l.get(3), (Double)l.get(4), (Double)l.get(5)),
         bb -> List.of(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ)
      );
   public static final StreamCodec<ByteBuf, ForceGroup> STREAM_FORCE_GROUP = ResourceLocation.STREAM_CODEC
      .map(ForceGroups.REGISTRY::get, ForceGroups.REGISTRY::getKey);
   public static final StreamCodec<ByteBuf, PointForce> STREAM_POINT_FORCE = STREAM_VECTOR3DC.apply(ByteBufCodecs.list(2))
      .map(l -> new PointForce((Vector3dc)l.getFirst(), (Vector3dc)l.get(1)), p -> List.of(p.point(), p.force()));

   public static <T> Codec<T> withAlternative(Codec<T> first, Codec<T> second) {
      return new SimCodecUtil.WithAlternativeButGood<>(first, second);
   }

   private static record WithAlternativeButGood<T>(Codec<T> first, Codec<T> second) implements Codec<T> {
      public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
         DataResult<Pair<T, T1>> result = this.first.decode(ops, input);
         return result.isSuccess() ? result : this.second.decode(ops, input);
      }

      public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
         DataResult<T1> result = this.first.encode(input, ops, prefix);
         return result.isSuccess() ? result : this.second.encode(input, ops, prefix);
      }
   }
}
