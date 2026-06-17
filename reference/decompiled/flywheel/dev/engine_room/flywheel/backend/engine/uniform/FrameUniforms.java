package dev.engine_room.flywheel.backend.engine.uniform;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.backend.engine.indirect.DepthPyramid;
import dev.engine_room.flywheel.backend.mixin.LevelRendererAccessor;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public final class FrameUniforms extends UniformWriter {
   private static final int SIZE = 852;
   static final UniformBuffer BUFFER = new UniformBuffer(0, 852);
   private static final Matrix4f VIEW = new Matrix4f();
   private static final Matrix4f VIEW_INVERSE = new Matrix4f();
   private static final Matrix4f VIEW_PREV = new Matrix4f();
   private static final Matrix4f PROJECTION = new Matrix4f();
   private static final Matrix4f PROJECTION_INVERSE = new Matrix4f();
   private static final Matrix4f PROJECTION_PREV = new Matrix4f();
   private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
   private static final Matrix4f VIEW_PROJECTION_INVERSE = new Matrix4f();
   private static final Matrix4f VIEW_PROJECTION_PREV = new Matrix4f();
   private static final Vector3f CAMERA_POS = new Vector3f();
   private static final Vector3f CAMERA_POS_PREV = new Vector3f();
   private static final Vector3f CAMERA_LOOK = new Vector3f();
   private static final Vector3f CAMERA_LOOK_PREV = new Vector3f();
   private static final Vector2f CAMERA_ROT = new Vector2f();
   private static final Vector2f CAMERA_ROT_PREV = new Vector2f();
   private static boolean firstWrite = true;
   private static int debugMode = DebugMode.OFF.ordinal();
   private static boolean frustumPaused = false;
   private static boolean frustumCapture = false;

   private FrameUniforms() {
   }

   public static void debugMode(DebugMode mode) {
      debugMode = mode.ordinal();
   }

   public static void captureFrustum() {
      frustumPaused = true;
      frustumCapture = true;
   }

   public static void unpauseFrustum() {
      frustumPaused = false;
   }

   public static void update(RenderContext context) {
      long ptr = BUFFER.ptr();
      setPrev();
      Vec3i renderOrigin = VisualizationManager.getOrThrow(context.level()).renderOrigin();
      Camera camera = context.camera();
      Vec3 cameraPos = camera.getPosition();
      float camX = (float)(cameraPos.x - (double)renderOrigin.getX());
      float camY = (float)(cameraPos.y - (double)renderOrigin.getY());
      float camZ = (float)(cameraPos.z - (double)renderOrigin.getZ());
      VIEW.set(context.modelView());
      VIEW.translate(-camX, -camY, -camZ);
      PROJECTION.set(context.projection());
      VIEW_PROJECTION.set(context.viewProjection());
      VIEW_PROJECTION.translate(-camX, -camY, -camZ);
      CAMERA_POS.set(camX, camY, camZ);
      CAMERA_LOOK.set(camera.getLookVector());
      CAMERA_ROT.set(camera.getXRot(), camera.getYRot());
      if (firstWrite) {
         setPrev();
      }

      if (firstWrite || !frustumPaused || frustumCapture) {
         writePackedFrustumPlanes(ptr, VIEW_PROJECTION);
         frustumCapture = false;
      }

      ptr += 96L;
      ptr = writeCullData(ptr);
      ptr = writeMatrices(ptr);
      ptr = writeRenderOrigin(ptr, renderOrigin);
      ptr = writeCamera(ptr);
      Window window = Minecraft.getInstance().getWindow();
      ptr = writeVec2(ptr, (float)window.getWidth(), (float)window.getHeight());
      ptr = writeFloat(ptr, (float)window.getWidth() / (float)window.getHeight());
      ptr = writeFloat(ptr, Math.max(2.5F, (float)window.getWidth() / 1920.0F * 2.5F));
      ptr = writeFloat(ptr, Minecraft.getInstance().gameRenderer.getDepthFar());
      ptr = writeTime(ptr, context);
      ptr = writeCameraIn(ptr, camera);
      ptr = writeInt(ptr, debugMode);
      ptr = writeFloat(ptr, 0.07F);
      firstWrite = false;
      BUFFER.markDirty();
   }

   private static long writeRenderOrigin(long ptr, Vec3i renderOrigin) {
      return writeIVec3(ptr, renderOrigin.getX(), renderOrigin.getY(), renderOrigin.getZ());
   }

   private static void setPrev() {
      VIEW_PREV.set(VIEW);
      PROJECTION_PREV.set(PROJECTION);
      VIEW_PROJECTION_PREV.set(VIEW_PROJECTION);
      CAMERA_POS_PREV.set(CAMERA_POS);
      CAMERA_LOOK_PREV.set(CAMERA_LOOK);
      CAMERA_ROT_PREV.set(CAMERA_ROT);
   }

   private static long writeMatrices(long ptr) {
      ptr = writeMat4(ptr, VIEW);
      ptr = writeMat4(ptr, VIEW.invert(VIEW_INVERSE));
      ptr = writeMat4(ptr, VIEW_PREV);
      ptr = writeMat4(ptr, PROJECTION);
      ptr = writeMat4(ptr, PROJECTION.invert(PROJECTION_INVERSE));
      ptr = writeMat4(ptr, PROJECTION_PREV);
      ptr = writeMat4(ptr, VIEW_PROJECTION);
      ptr = writeMat4(ptr, VIEW_PROJECTION.invert(VIEW_PROJECTION_INVERSE));
      return writeMat4(ptr, VIEW_PROJECTION_PREV);
   }

   private static long writeCamera(long ptr) {
      ptr = writeVec3(ptr, CAMERA_POS.x, CAMERA_POS.y, CAMERA_POS.z);
      ptr = writeVec3(ptr, CAMERA_POS_PREV.x, CAMERA_POS_PREV.y, CAMERA_POS_PREV.z);
      ptr = writeVec3(ptr, CAMERA_LOOK.x, CAMERA_LOOK.y, CAMERA_LOOK.z);
      ptr = writeVec3(ptr, CAMERA_LOOK_PREV.x, CAMERA_LOOK_PREV.y, CAMERA_LOOK_PREV.z);
      ptr = writeVec2(ptr, CAMERA_ROT.x, CAMERA_ROT.y);
      return writeVec2(ptr, CAMERA_ROT_PREV.x, CAMERA_ROT_PREV.y);
   }

   private static long writeTime(long ptr, RenderContext context) {
      int ticks = ((LevelRendererAccessor)context.renderer()).flywheel$getTicks();
      float partialTick = context.partialTick();
      float renderTicks = (float)ticks + partialTick;
      float renderSeconds = renderTicks / 20.0F;
      float systemSeconds = (float)Util.getMillis() / 1000.0F;
      int systemMillis = (int)(Util.getMillis() % 2147483647L);
      ptr = writeInt(ptr, ticks);
      ptr = writeFloat(ptr, partialTick);
      ptr = writeFloat(ptr, renderTicks);
      ptr = writeFloat(ptr, renderSeconds);
      ptr = writeFloat(ptr, systemSeconds);
      return writeInt(ptr, systemMillis);
   }

   private static long writeCameraIn(long ptr, Camera camera) {
      if (!camera.isInitialized()) {
         ptr = writeInt(ptr, 0);
         return writeInt(ptr, 0);
      } else {
         Level level = camera.getEntity().level();
         BlockPos blockPos = camera.getBlockPosition();
         Vec3 cameraPos = camera.getPosition();
         return writeInFluidAndBlock(ptr, level, blockPos, cameraPos);
      }
   }

   private static long writeCullData(long ptr) {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget mainRenderTarget = mc.getMainRenderTarget();
      int pyramidWidth = DepthPyramid.mip0Size(mainRenderTarget.width);
      int pyramidHeight = DepthPyramid.mip0Size(mainRenderTarget.height);
      int pyramidDepth = DepthPyramid.getImageMipLevels(pyramidWidth, pyramidHeight);
      ptr = writeFloat(ptr, 0.05F);
      ptr = writeFloat(ptr, mc.gameRenderer.getDepthFar());
      ptr = writeFloat(ptr, PROJECTION.m00());
      ptr = writeFloat(ptr, PROJECTION.m11());
      ptr = writeFloat(ptr, (float)pyramidWidth);
      ptr = writeFloat(ptr, (float)pyramidHeight);
      ptr = writeInt(ptr, pyramidDepth - 1);
      return writeInt(ptr, 0);
   }

   private static void writePackedFrustumPlanes(long ptr, Matrix4f m) {
      float nxX = m.m03() + m.m00();
      float nxY = m.m13() + m.m10();
      float nxZ = m.m23() + m.m20();
      float nxW = m.m33() + m.m30();
      float invl = Math.invsqrt(nxX * nxX + nxY * nxY + nxZ * nxZ);
      nxX *= invl;
      nxY *= invl;
      nxZ *= invl;
      nxW *= invl;
      float pxX = m.m03() - m.m00();
      float pxY = m.m13() - m.m10();
      float pxZ = m.m23() - m.m20();
      float pxW = m.m33() - m.m30();
      invl = Math.invsqrt(pxX * pxX + pxY * pxY + pxZ * pxZ);
      pxX *= invl;
      pxY *= invl;
      pxZ *= invl;
      pxW *= invl;
      float nyX = m.m03() + m.m01();
      float nyY = m.m13() + m.m11();
      float nyZ = m.m23() + m.m21();
      float nyW = m.m33() + m.m31();
      invl = Math.invsqrt(nyX * nyX + nyY * nyY + nyZ * nyZ);
      nyX *= invl;
      nyY *= invl;
      nyZ *= invl;
      nyW *= invl;
      float pyX = m.m03() - m.m01();
      float pyY = m.m13() - m.m11();
      float pyZ = m.m23() - m.m21();
      float pyW = m.m33() - m.m31();
      invl = Math.invsqrt(pyX * pyX + pyY * pyY + pyZ * pyZ);
      pyX *= invl;
      pyY *= invl;
      pyZ *= invl;
      pyW *= invl;
      float nzX = m.m03() + m.m02();
      float nzY = m.m13() + m.m12();
      float nzZ = m.m23() + m.m22();
      float nzW = m.m33() + m.m32();
      invl = Math.invsqrt(nzX * nzX + nzY * nzY + nzZ * nzZ);
      nzX *= invl;
      nzY *= invl;
      nzZ *= invl;
      nzW *= invl;
      float pzX = m.m03() - m.m02();
      float pzY = m.m13() - m.m12();
      float pzZ = m.m23() - m.m22();
      float pzW = m.m33() - m.m32();
      invl = Math.invsqrt(pzX * pzX + pzY * pzY + pzZ * pzZ);
      pzX *= invl;
      pzY *= invl;
      pzZ *= invl;
      pzW *= invl;
      MemoryUtil.memPutFloat(ptr, nxX);
      MemoryUtil.memPutFloat(ptr + 4L, pxX);
      MemoryUtil.memPutFloat(ptr + 8L, nyX);
      MemoryUtil.memPutFloat(ptr + 12L, pyX);
      MemoryUtil.memPutFloat(ptr + 16L, nxY);
      MemoryUtil.memPutFloat(ptr + 20L, pxY);
      MemoryUtil.memPutFloat(ptr + 24L, nyY);
      MemoryUtil.memPutFloat(ptr + 28L, pyY);
      MemoryUtil.memPutFloat(ptr + 32L, nxZ);
      MemoryUtil.memPutFloat(ptr + 36L, pxZ);
      MemoryUtil.memPutFloat(ptr + 40L, nyZ);
      MemoryUtil.memPutFloat(ptr + 44L, pyZ);
      MemoryUtil.memPutFloat(ptr + 48L, nxW);
      MemoryUtil.memPutFloat(ptr + 52L, pxW);
      MemoryUtil.memPutFloat(ptr + 56L, nyW);
      MemoryUtil.memPutFloat(ptr + 60L, pyW);
      MemoryUtil.memPutFloat(ptr + 64L, nzX);
      MemoryUtil.memPutFloat(ptr + 68L, pzX);
      MemoryUtil.memPutFloat(ptr + 72L, nzY);
      MemoryUtil.memPutFloat(ptr + 76L, pzY);
      MemoryUtil.memPutFloat(ptr + 80L, nzZ);
      MemoryUtil.memPutFloat(ptr + 84L, pzZ);
      MemoryUtil.memPutFloat(ptr + 88L, nzW);
      MemoryUtil.memPutFloat(ptr + 92L, pzW);
   }

   public static boolean debugOn() {
      return debugMode != DebugMode.OFF.ordinal();
   }
}
