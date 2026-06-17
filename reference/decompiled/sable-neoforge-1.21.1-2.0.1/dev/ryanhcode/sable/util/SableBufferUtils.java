package dev.ryanhcode.sable.util;

import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class SableBufferUtils {
   public static final StreamCodec<? super RegistryFriendlyByteBuf, Pose3d> POSE3D_STREAM_CODEC = StreamCodec.of(
      SableBufferUtils::write, buf -> read(buf, new Pose3d())
   );

   public static void write(ByteBuf buf, Vector3dc vec) {
      buf.writeDouble(vec.x());
      buf.writeDouble(vec.y());
      buf.writeDouble(vec.z());
   }

   public static void write(ByteBuf buf, Vector3fc vec) {
      buf.writeFloat(vec.x());
      buf.writeFloat(vec.y());
      buf.writeFloat(vec.z());
   }

   public static Vector3d read(ByteBuf buf, Vector3d dest) {
      return dest.set(buf.readDouble(), buf.readDouble(), buf.readDouble());
   }

   public static Vector3f read(ByteBuf buf, Vector3f dest) {
      return dest.set(buf.readFloat(), buf.readFloat(), buf.readFloat());
   }

   public static void write(ByteBuf buf, Quaterniondc quat) {
      buf.writeFloat((float)quat.x());
      buf.writeFloat((float)quat.y());
      buf.writeFloat((float)quat.z());
      buf.writeFloat((float)quat.w());
   }

   public static Quaterniond read(ByteBuf buf, Quaterniond dest) {
      return dest.set((double)buf.readFloat(), (double)buf.readFloat(), (double)buf.readFloat(), (double)buf.readFloat());
   }

   public static void write(ByteBuf buf, Pose3dc pose) {
      write(buf, pose.position());
      write(buf, pose.orientation());
      write(buf, pose.rotationPoint());
   }

   public static Pose3d read(ByteBuf buf, Pose3d pose) {
      read(buf, pose.position());
      read(buf, pose.orientation());
      read(buf, pose.rotationPoint());
      return pose;
   }

   public static void write(ByteBuf buf, BoundingBox3ic bounds) {
      buf.writeInt(bounds.minX());
      buf.writeInt(bounds.minY());
      buf.writeInt(bounds.minZ());
      buf.writeInt(bounds.maxX());
      buf.writeInt(bounds.maxY());
      buf.writeInt(bounds.maxZ());
   }

   public static BoundingBox3i read(ByteBuf buf, BoundingBox3i dest) {
      return dest.set(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
   }
}
