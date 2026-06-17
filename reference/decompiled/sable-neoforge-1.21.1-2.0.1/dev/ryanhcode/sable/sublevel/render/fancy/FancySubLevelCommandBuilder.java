package dev.ryanhcode.sable.sublevel.render.fancy;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.ryanhcode.sable.sublevel.render.staging.StagingBuffer;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

public class FancySubLevelCommandBuilder implements NativeResource {
   private static final int MAX_SECTIONS = 4096;
   private static final int INDIRECT_COMMAND_SIZE = 20;
   private static final Direction[] DIRECTIONS = Direction.values();
   private final StagingBuffer stagingBuffer;
   private final int commandBuffer;
   private final Deque<FancySubLevelSectionCompiler.RenderSection> sectionQueue;
   private int drawCount;

   public FancySubLevelCommandBuilder(StagingBuffer stagingBuffer) {
      this.stagingBuffer = stagingBuffer;
      this.commandBuffer = GlStateManager._glGenBuffers();
      GL15C.glBindBuffer(36671, this.commandBuffer);
      GL15C.glBufferData(36671, 81920L, 35040);
      GL15C.glBindBuffer(36671, 0);
      this.sectionQueue = new LinkedBlockingDeque<>();
   }

   private void flush() {
      if (this.drawCount > 0) {
         this.stagingBuffer.copy(this.commandBuffer, 0L);
         GL43C.glMultiDrawElementsIndirect(4, 5121, 0L, this.drawCount, 0);
      }

      this.drawCount = 0;
   }

   public void setup() {
      GL15C.glBindBuffer(36671, this.commandBuffer);
   }

   public void clear() {
      this.flush();
      GL15C.glBindBuffer(36671, 0);
   }

   public void free() {
      GlStateManager._glDeleteBuffers(this.commandBuffer);
   }

   public void draw(FancySubLevelRenderData data, RenderType renderType, int sectionX, int sectionY, int sectionZ) {
      for (FancySubLevelSectionCompiler.RenderSection section : data.getOcclusionData().getVisibleSections()) {
         SectionPos pos = section.getPos();
         int dx = pos.getX() - sectionX;
         int dy = pos.getY() - sectionY;
         int dz = pos.getZ() - sectionZ;
         FancySubLevelSectionCompiler.CompiledSection compiledSection = section.getCompiledSection();

         for (Direction direction : DIRECTIONS) {
            BucketRenderBuffer.Slice slice = compiledSection.get(renderType, direction);
            if (slice != null) {
               int dot = direction.getStepX() * dx + direction.getStepY() * dy + direction.getStepZ() * dz;
               if (dot <= 0) {
                  long pointer = this.stagingBuffer.reserve(20L);
                  MemoryUtil.memPutInt(pointer, 6);
                  MemoryUtil.memPutInt(pointer + 4L, slice.length());
                  MemoryUtil.memPutInt(pointer + 8L, 0);
                  MemoryUtil.memPutInt(pointer + 12L, direction.get3DDataValue() * 4);
                  MemoryUtil.memPutInt(pointer + 16L, slice.offset());
                  this.drawCount++;
                  if (this.drawCount >= 4096) {
                     this.flush();
                  }
               }
            }
         }
      }

      this.flush();
      this.sectionQueue.clear();
   }
}
