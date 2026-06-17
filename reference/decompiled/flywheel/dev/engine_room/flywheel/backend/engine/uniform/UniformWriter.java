package dev.engine_room.flywheel.backend.engine.uniform;

import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryUtil;

class UniformWriter {
   static long writeInt(long ptr, int value) {
      MemoryUtil.memPutInt(ptr, value);
      return ptr + 4L;
   }

   static long writeFloat(long ptr, float value) {
      MemoryUtil.memPutFloat(ptr, value);
      return ptr + 4L;
   }

   static long writeVec2(long ptr, float x, float y) {
      MemoryUtil.memPutFloat(ptr, x);
      MemoryUtil.memPutFloat(ptr + 4L, y);
      return ptr + 8L;
   }

   static long writeVec3(long ptr, float x, float y, float z) {
      MemoryUtil.memPutFloat(ptr, x);
      MemoryUtil.memPutFloat(ptr + 4L, y);
      MemoryUtil.memPutFloat(ptr + 8L, z);
      MemoryUtil.memPutFloat(ptr + 12L, 0.0F);
      return ptr + 16L;
   }

   static long writeVec3(long ptr, Vector3fc vec) {
      return writeVec3(ptr, vec.x(), vec.y(), vec.z());
   }

   static long writeVec4(long ptr, float x, float y, float z, float w) {
      MemoryUtil.memPutFloat(ptr, x);
      MemoryUtil.memPutFloat(ptr + 4L, y);
      MemoryUtil.memPutFloat(ptr + 8L, z);
      MemoryUtil.memPutFloat(ptr + 12L, w);
      return ptr + 16L;
   }

   static long writeIVec2(long ptr, int x, int y) {
      MemoryUtil.memPutInt(ptr, x);
      MemoryUtil.memPutInt(ptr + 4L, y);
      return ptr + 8L;
   }

   static long writeIVec3(long ptr, int x, int y, int z) {
      MemoryUtil.memPutInt(ptr, x);
      MemoryUtil.memPutInt(ptr + 4L, y);
      MemoryUtil.memPutInt(ptr + 8L, z);
      MemoryUtil.memPutInt(ptr + 12L, 0);
      return ptr + 16L;
   }

   static long writeIVec4(long ptr, int x, int y, int z, int w) {
      MemoryUtil.memPutInt(ptr, x);
      MemoryUtil.memPutInt(ptr + 4L, y);
      MemoryUtil.memPutInt(ptr + 8L, z);
      MemoryUtil.memPutInt(ptr + 12L, w);
      return ptr + 16L;
   }

   static long writeMat4(long ptr, Matrix4f mat) {
      ExtraMemoryOps.putMatrix4f(ptr, mat);
      return ptr + 64L;
   }

   static long writeInFluidAndBlock(long ptr, Level level, BlockPos blockPos, Vec3 pos) {
      FluidState fluidState = level.getFluidState(blockPos);
      BlockState blockState = level.getBlockState(blockPos);
      float height = fluidState.getHeight(level, blockPos);
      if (fluidState.isEmpty()) {
         MemoryUtil.memPutInt(ptr, 0);
      } else if (pos.y < (double)((float)blockPos.getY() + height)) {
         if (fluidState.is(FluidTags.WATER)) {
            MemoryUtil.memPutInt(ptr, 1);
         } else if (fluidState.is(FluidTags.LAVA)) {
            MemoryUtil.memPutInt(ptr, 2);
         } else {
            MemoryUtil.memPutInt(ptr, -1);
         }
      }

      if (blockState.isAir()) {
         MemoryUtil.memPutInt(ptr + 4L, 0);
      } else if (blockState.is(Blocks.POWDER_SNOW)) {
         MemoryUtil.memPutInt(ptr + 4L, 0);
      } else {
         MemoryUtil.memPutInt(ptr + 4L, -1);
      }

      return ptr + 8L;
   }
}
