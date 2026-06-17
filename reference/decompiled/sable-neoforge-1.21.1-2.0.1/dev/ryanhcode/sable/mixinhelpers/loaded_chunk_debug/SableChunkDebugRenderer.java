package dev.ryanhcode.sable.mixinhelpers.loaded_chunk_debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.mixinterface.loaded_chunk_debug.DebugChunkProviderAttachments;
import dev.ryanhcode.sable.mixinterface.loaded_chunk_debug.DebugLevelChunkExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4f;

@Internal
public class SableChunkDebugRenderer {
   public static void render(PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ) {
      long time = System.currentTimeMillis();
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.gameRenderer.getMainCamera().getEntity();
      VertexConsumer builder = bufferSource.getBuffer(RenderType.debugLineStrip(1.0));
      Matrix4f pose = poseStack.last().pose();
      ClientLevel level = minecraft.level;
      int minBuildHeight = level.getMinBuildHeight();
      int maxBuildHeight = level.getMaxBuildHeight();
      DebugChunkProviderAttachments attachments = (DebugChunkProviderAttachments)level.getChunkSource();

      for (LevelChunk chunk : attachments.sable$loadedChunks()) {
         ChunkPos pos = chunk.getPos();
         float diff = (float)Mth.clamp(time - ((DebugLevelChunkExtension)chunk).sable$getLastUpdate(), 0L, 1000L) / 1000.0F;
         float red = 1.0F - diff;
         float blue = 0.0F;
         float x = (float)((double)pos.getMinBlockX() - camX);
         float z = (float)((double)pos.getMinBlockZ() - camZ);
         float y = (float)((double)minBuildHeight - camY);
         if (camY > (double)minBuildHeight) {
            y += 10.0F * ((1.0F - diff) / 100.0F);
         } else {
            y -= 10.0F * ((1.0F - diff) / 100.0F);
         }

         float y1 = (float)((double)maxBuildHeight - camY);
         if (camY < (double)maxBuildHeight) {
            y1 -= 10.0F * ((1.0F - diff) / 100.0F);
         } else {
            y1 += 10.0F * ((1.0F - diff) / 100.0F);
         }

         builder.addVertex(pose, x, y, z).setColor(red, diff, 0.0F, 0.0F);
         builder.addVertex(pose, x, y, z).setColor(red, diff, 0.0F, 1.0F);
         builder.addVertex(pose, x + 16.0F, y, z).setColor(red, diff, 0.0F, 1.0F);
         builder.addVertex(pose, x + 16.0F, y, z + 16.0F).setColor(red, diff, 0.0F, 1.0F);
         builder.addVertex(pose, x, y, z + 16.0F).setColor(red, diff, 0.0F, 1.0F);
         builder.addVertex(pose, x, y, z).setColor(red, diff, 0.0F, 0.0F);
         builder.addVertex(pose, x, y1, z).setColor(red, diff, 0.0F, 0.0F);
         builder.addVertex(pose, x, y1, z).setColor(red, diff, 0.0F, 1.0F);
         builder.addVertex(pose, x + 16.0F, y1, z).setColor(red, diff, 0.0F, 1.0F);
         builder.addVertex(pose, x + 16.0F, y1, z + 16.0F).setColor(red, diff, 0.0F, 1.0F);
         builder.addVertex(pose, x, y1, z + 16.0F).setColor(red, diff, 0.0F, 1.0F);
         builder.addVertex(pose, x, y1, z).setColor(red, diff, 0.0F, 0.0F);
      }

      ChunkPos ckPos = entity.chunkPosition();
      float xx = (float)((double)(ckPos.x * 16) - camX);
      float yx = (float)((double)minBuildHeight - camY);
      float y1 = (float)((double)maxBuildHeight - camY);
      float zx = (float)((double)(ckPos.z * 16) - camZ);

      for (int xO = 0; xO < 2; xO++) {
         for (int zO = 0; zO < 2; zO++) {
            builder.addVertex(pose, xx + (float)(xO * 16), yx, zx + (float)(zO * 16)).setColor(1.0F, 1.0F, 0.0F, 0.0F);
            builder.addVertex(pose, xx + (float)(xO * 16), yx, zx + (float)(zO * 16)).setColor(1.0F, 1.0F, 0.0F, 1.0F);
            builder.addVertex(pose, xx + (float)(xO * 16), y1, zx + (float)(zO * 16)).setColor(1.0F, 1.0F, 0.0F, 1.0F);
            builder.addVertex(pose, xx + (float)(xO * 16), y1, zx + (float)(zO * 16)).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         }
      }

      yx = (float)minBuildHeight;
      yx = (float)((int)(yx / 16.0F) * 16);
      y1 = (float)maxBuildHeight;

      for (int yO = (int)yx; (float)yO <= y1 + 1.0F; yO += 16) {
         builder.addVertex(pose, xx, (float)((double)yO - camY), zx).setColor(0.0F, 0.0F, 1.0F, 0.0F);
         builder.addVertex(pose, xx, (float)((double)yO - camY), zx).setColor(0.0F, 0.0F, 1.0F, 1.0F);
         builder.addVertex(pose, xx + 16.0F, (float)((double)yO - camY), zx).setColor(0.0F, 0.0F, 1.0F, 1.0F);
         builder.addVertex(pose, xx + 16.0F, (float)((double)yO - camY), zx + 16.0F).setColor(0.0F, 0.0F, 1.0F, 1.0F);
         builder.addVertex(pose, xx, (float)((double)yO - camY), zx + 16.0F).setColor(0.0F, 0.0F, 1.0F, 1.0F);
         builder.addVertex(pose, xx, (float)((double)yO - camY), zx).setColor(0.0F, 0.0F, 1.0F, 1.0F);
         builder.addVertex(pose, xx, (float)((double)yO - camY), zx).setColor(0.0F, 0.0F, 1.0F, 0.0F);
      }
   }
}
