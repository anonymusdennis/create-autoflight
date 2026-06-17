package dev.engine_room.flywheel.lib.util;

import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryUtil;

public final class ExtraMemoryOps {
   private ExtraMemoryOps() {
   }

   public static void put4x8(long ptr, int value) {
      MemoryUtil.memPutByte(ptr, (byte)(value & 0xFF));
      MemoryUtil.memPutByte(ptr + 1L, (byte)(value >>> 8 & 0xFF));
      MemoryUtil.memPutByte(ptr + 2L, (byte)(value >>> 16 & 0xFF));
      MemoryUtil.memPutByte(ptr + 3L, (byte)(value >>> 24 & 0xFF));
   }

   public static void put2x16(long ptr, int value) {
      MemoryUtil.memPutShort(ptr, (short)(value & 65535));
      MemoryUtil.memPutShort(ptr + 2L, (short)(value >>> 16 & 65535));
   }

   public static void putVector2f(long ptr, Vector2fc vector) {
      MemoryUtil.memPutFloat(ptr, vector.x());
      MemoryUtil.memPutFloat(ptr + 4L, vector.y());
   }

   public static void putVector3f(long ptr, Vector3fc vector) {
      MemoryUtil.memPutFloat(ptr, vector.x());
      MemoryUtil.memPutFloat(ptr + 4L, vector.y());
      MemoryUtil.memPutFloat(ptr + 8L, vector.z());
   }

   public static void putVector4f(long ptr, Vector4fc vector) {
      MemoryUtil.memPutFloat(ptr, vector.x());
      MemoryUtil.memPutFloat(ptr + 4L, vector.y());
      MemoryUtil.memPutFloat(ptr + 8L, vector.z());
      MemoryUtil.memPutFloat(ptr + 12L, vector.w());
   }

   public static void putQuaternionf(long ptr, Quaternionfc quaternion) {
      MemoryUtil.memPutFloat(ptr, quaternion.x());
      MemoryUtil.memPutFloat(ptr + 4L, quaternion.y());
      MemoryUtil.memPutFloat(ptr + 8L, quaternion.z());
      MemoryUtil.memPutFloat(ptr + 12L, quaternion.w());
   }

   public static void putMatrix3f(long ptr, Matrix3fc matrix) {
      MemoryUtil.memPutFloat(ptr, matrix.m00());
      MemoryUtil.memPutFloat(ptr + 4L, matrix.m01());
      MemoryUtil.memPutFloat(ptr + 8L, matrix.m02());
      MemoryUtil.memPutFloat(ptr + 12L, matrix.m10());
      MemoryUtil.memPutFloat(ptr + 16L, matrix.m11());
      MemoryUtil.memPutFloat(ptr + 20L, matrix.m12());
      MemoryUtil.memPutFloat(ptr + 24L, matrix.m20());
      MemoryUtil.memPutFloat(ptr + 28L, matrix.m21());
      MemoryUtil.memPutFloat(ptr + 32L, matrix.m22());
   }

   public static void putMatrix3fPadded(long ptr, Matrix3fc matrix) {
      MemoryUtil.memPutFloat(ptr, matrix.m00());
      MemoryUtil.memPutFloat(ptr + 4L, matrix.m01());
      MemoryUtil.memPutFloat(ptr + 8L, matrix.m02());
      MemoryUtil.memPutFloat(ptr + 12L, 0.0F);
      MemoryUtil.memPutFloat(ptr + 16L, matrix.m10());
      MemoryUtil.memPutFloat(ptr + 20L, matrix.m11());
      MemoryUtil.memPutFloat(ptr + 24L, matrix.m12());
      MemoryUtil.memPutFloat(ptr + 28L, 0.0F);
      MemoryUtil.memPutFloat(ptr + 32L, matrix.m20());
      MemoryUtil.memPutFloat(ptr + 36L, matrix.m21());
      MemoryUtil.memPutFloat(ptr + 40L, matrix.m22());
      MemoryUtil.memPutFloat(ptr + 44L, 0.0F);
   }

   public static void putMatrix4f(long ptr, Matrix4fc matrix) {
      MemoryUtil.memPutFloat(ptr, matrix.m00());
      MemoryUtil.memPutFloat(ptr + 4L, matrix.m01());
      MemoryUtil.memPutFloat(ptr + 8L, matrix.m02());
      MemoryUtil.memPutFloat(ptr + 12L, matrix.m03());
      MemoryUtil.memPutFloat(ptr + 16L, matrix.m10());
      MemoryUtil.memPutFloat(ptr + 20L, matrix.m11());
      MemoryUtil.memPutFloat(ptr + 24L, matrix.m12());
      MemoryUtil.memPutFloat(ptr + 28L, matrix.m13());
      MemoryUtil.memPutFloat(ptr + 32L, matrix.m20());
      MemoryUtil.memPutFloat(ptr + 36L, matrix.m21());
      MemoryUtil.memPutFloat(ptr + 40L, matrix.m22());
      MemoryUtil.memPutFloat(ptr + 44L, matrix.m23());
      MemoryUtil.memPutFloat(ptr + 48L, matrix.m30());
      MemoryUtil.memPutFloat(ptr + 52L, matrix.m31());
      MemoryUtil.memPutFloat(ptr + 56L, matrix.m32());
      MemoryUtil.memPutFloat(ptr + 60L, matrix.m33());
   }
}
