package dev.ryanhcode.sable.render.region;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3i;

@Internal
public abstract class SimpleCulledRenderRegion {
   private Collection<BlockPos> unbuiltData;
   private boolean built = false;
   private VertexBuffer buffer;
   private Vec3 origin;

   public SimpleCulledRenderRegion(Collection<BlockPos> blocks) {
      this.unbuiltData = blocks;
   }

   public void render(Matrix4f modelView, Matrix4f projectionMatrix) {
      if (!this.built) {
         this.build();
      }

      ShaderInstance shader = RenderSystem.getShader();

      assert shader != null;

      Minecraft client = Minecraft.getInstance();
      SubLevel subLevel = Sable.HELPER.getContaining(client.level, this.origin);
      Vec3 globalOrigin = this.origin;
      Quaternionf globalOrientation = new Quaternionf();
      if (subLevel instanceof ClientSubLevel clientSubLevel) {
         Pose3dc renderPose = clientSubLevel.renderPose();
         globalOrigin = renderPose.transformPosition(globalOrigin);
         globalOrientation.set(renderPose.orientation());
      }

      Vec3 relativePos = globalOrigin.subtract(client.gameRenderer.getMainCamera().getPosition());
      Matrix4f modelViewMatrix = new Matrix4f(modelView)
         .setTranslation(0.0F, 0.0F, 0.0F)
         .translate((float)relativePos.x, (float)relativePos.y, (float)relativePos.z)
         .rotate(globalOrientation);
      shader.setDefaultUniforms(Mode.QUADS, modelViewMatrix, projectionMatrix, client.getWindow());
      shader.apply();
      this.buffer.bind();
      this.buffer.draw();
      VertexBuffer.unbind();
   }

   public void build() {
      BlockPos firstBlock = this.unbuiltData.stream().findFirst().orElseThrow();
      Vector3i minBlock = new Vector3i(firstBlock.getX(), firstBlock.getY(), firstBlock.getZ());
      Vector3i maxBlock = new Vector3i(firstBlock.getX(), firstBlock.getY(), firstBlock.getZ());
      Vector3i currentBlock = new Vector3i();

      for (BlockPos block : this.unbuiltData) {
         currentBlock.set(block.getX(), block.getY(), block.getZ());
         minBlock.min(currentBlock);
         maxBlock.max(currentBlock);
      }

      int gridSize = maxBlock.x() - minBlock.x() + 1;
      gridSize = Math.max(gridSize, maxBlock.y() - minBlock.y() + 1);
      gridSize = Math.max(gridSize, maxBlock.z() - minBlock.z() + 1);
      BlockPos originBlock = new BlockPos(minBlock.x(), minBlock.y(), minBlock.z());
      this.origin = Vec3.atLowerCornerOf(originBlock);
      SimpleCulledRenderRegionBuilder builder = this.createMeshBuilder(gridSize);

      for (BlockPos blockPos : this.unbuiltData) {
         builder.add(blockPos.getX() - originBlock.getX(), blockPos.getY() - originBlock.getY(), blockPos.getZ() - originBlock.getZ());
      }

      builder.buildNoGreedy();
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, this.getVertexFormat());
      builder.render(new Matrix4f(), bufferBuilder);
      this.unbuiltData = null;
      this.buffer = new VertexBuffer(Usage.STATIC);
      this.buffer.bind();
      this.buffer.upload(bufferBuilder.buildOrThrow());
      this.built = true;
   }

   public Vec3 getOrigin() {
      return this.origin;
   }

   public abstract SimpleCulledRenderRegionBuilder createMeshBuilder(int var1);

   public abstract VertexFormat getVertexFormat();

   public void free() {
      if (this.built) {
         this.buffer.close();
      }
   }
}
