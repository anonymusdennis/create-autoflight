package dev.ryanhcode.sable.sublevel.render.fancy.task;

import dev.ryanhcode.sable.sublevel.render.fancy.BucketRenderBuffer;
import dev.ryanhcode.sable.sublevel.render.fancy.SubLevelMeshBuilder;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.renderer.SectionBufferBuilderPack;

@FunctionalInterface
public interface SubLevelTask {
   void process(SectionBufferBuilderPack var1, SubLevelTask.MeshUploader var2);

   public interface MeshUploader {
      CompletableFuture<BucketRenderBuffer.Slice[]> upload(SubLevelMeshBuilder.QuadMesh var1);

      SubLevelMeshBuilder getMeshBuilder();
   }
}
