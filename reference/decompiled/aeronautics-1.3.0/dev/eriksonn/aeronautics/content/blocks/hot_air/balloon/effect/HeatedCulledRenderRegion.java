package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerGraph;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegionBuilder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.util.LevelAccelerator;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3i;
import org.lwjgl.system.NativeResource;

public class HeatedCulledRenderRegion implements NativeResource {
   private Balloon balloon;
   private boolean built = false;
   private VertexBuffer buffer;
   private Vec3 origin;
   private final LevelAccelerator accelerator;

   public HeatedCulledRenderRegion(LevelAccelerator accelerator, Balloon balloon) {
      this.accelerator = accelerator;
      this.balloon = balloon;
   }

   public void render(Matrix4f modelView, Matrix4f projectionMatrix) {
      if (!this.built) {
         this.build();
      }

      if (this.buffer != null) {
         ShaderInstance shader = RenderSystem.getShader();

         assert shader != null;

         Minecraft client = Minecraft.getInstance();
         ClientSubLevel subLevel = Sable.HELPER.getContainingClient(this.origin);
         Vec3 globalOrigin = this.origin;
         Quaternionf globalOrientation = new Quaternionf();
         if (subLevel != null) {
            Pose3dc renderPose = subLevel.renderPose();
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
   }

   public void build() {
      BoundingBox3ic bounds = this.balloon.getBounds();
      Vector3i minBlock = new Vector3i(bounds.minX(), bounds.minY(), bounds.minZ());
      Vector3i maxBlock = new Vector3i(bounds.maxX(), bounds.maxY(), bounds.maxZ());
      int gridSize = maxBlock.x() - minBlock.x() + 1;
      gridSize = Math.max(gridSize, maxBlock.y() - minBlock.y() + 1);
      gridSize = Math.max(gridSize, maxBlock.z() - minBlock.z() + 1);
      BlockPos originBlock = new BlockPos(minBlock.x(), minBlock.y(), minBlock.z());
      this.origin = Vec3.atLowerCornerOf(originBlock);
      SimpleCulledRenderRegionBuilder builder = this.createMeshBuilder(gridSize);
      BalloonLayerGraph graph = this.balloon.getGraph();

      for (int y = graph.getMinY(); y <= graph.getMaxY(); y++) {
         for (BalloonLayerData layer : graph.getLayersAtY(y)) {
            Iterator<BlockPos> layerBlocks = layer.blockIterator();

            while (layerBlocks.hasNext()) {
               BlockPos blockPos = layerBlocks.next();
               builder.add(blockPos.getX() - originBlock.getX(), blockPos.getY() - originBlock.getY(), blockPos.getZ() - originBlock.getZ());
            }
         }
      }

      builder.buildNoGreedy();
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, this.getVertexFormat());
      builder.render(new Matrix4f(), bufferBuilder);
      this.balloon = null;
      MeshData builtData = bufferBuilder.build();
      if (builtData != null) {
         this.buffer = new VertexBuffer(Usage.STATIC);
         this.buffer.bind();
         this.buffer.upload(builtData);
      } else {
         this.buffer = null;
      }

      this.built = true;
   }

   public Vec3 getOrigin() {
      return this.origin;
   }

   public SimpleCulledRenderRegionBuilder createMeshBuilder(int gridSize) {
      return new HeatedCulledRenderRegionBuilder(BlockPos.containing(this.getOrigin()), this.accelerator, gridSize);
   }

   public VertexFormat getVertexFormat() {
      return DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL;
   }

   public void free() {
      if (this.built && this.buffer != null) {
         this.buffer.close();
      }
   }
}
