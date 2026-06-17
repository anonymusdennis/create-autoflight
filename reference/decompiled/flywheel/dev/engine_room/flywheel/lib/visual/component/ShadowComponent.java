package dev.engine_room.flywheel.lib.visual.component;

import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.ShadowInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.QuadMesh;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.util.InstanceRecycler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class ShadowComponent implements EntityComponent {
   private static final ResourceLocation SHADOW_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/shadow.png");
   private static final Material SHADOW_MATERIAL = SimpleMaterial.builder()
      .texture(SHADOW_TEXTURE)
      .mipmap(false)
      .polygonOffset(true)
      .transparency(Transparency.TRANSLUCENT)
      .writeMask(WriteMask.COLOR)
      .build();
   private static final Model SHADOW_MODEL = new SingleMeshModel(ShadowComponent.ShadowMesh.INSTANCE, SHADOW_MATERIAL);
   private final VisualizationContext context;
   private final Entity entity;
   private final Level level;
   private final MutableBlockPos pos = new MutableBlockPos();
   private final InstanceRecycler<ShadowInstance> instances = new InstanceRecycler<>(this::createInstance);
   private float radius = 0.0F;
   private float strength = 1.0F;

   public ShadowComponent(VisualizationContext context, Entity entity) {
      this.context = context;
      this.entity = entity;
      this.level = entity.level();
   }

   private ShadowInstance createInstance() {
      return this.context.instancerProvider().instancer(InstanceTypes.SHADOW, SHADOW_MODEL).createInstance();
   }

   public float radius() {
      return this.radius;
   }

   public float strength() {
      return this.strength;
   }

   public ShadowComponent radius(float radius) {
      this.radius = Math.min(radius, 32.0F);
      return this;
   }

   public ShadowComponent strength(float strength) {
      this.strength = strength;
      return this;
   }

   @Override
   public void beginFrame(DynamicVisual.Context context) {
      this.instances.resetCount();
      boolean shadowsEnabled = (Boolean)Minecraft.getInstance().options.entityShadows().get();
      if (shadowsEnabled && this.radius > 0.0F && !this.entity.isInvisible()) {
         this.setupInstances(context);
      }

      this.instances.discardExtra();
   }

   private void setupInstances(DynamicVisual.Context context) {
      double entityX = Mth.lerp((double)context.partialTick(), this.entity.xOld, this.entity.getX());
      double entityY = Mth.lerp((double)context.partialTick(), this.entity.yOld, this.entity.getY());
      double entityZ = Mth.lerp((double)context.partialTick(), this.entity.zOld, this.entity.getZ());
      float castDistance = Math.min(this.strength * 2.0F, this.radius);
      int minXPos = Mth.floor(entityX - (double)this.radius);
      int maxXPos = Mth.floor(entityX + (double)this.radius);
      int minYPos = Mth.floor(entityY - (double)castDistance);
      int maxYPos = Mth.floor(entityY);
      int minZPos = Mth.floor(entityZ - (double)this.radius);
      int maxZPos = Mth.floor(entityZ + (double)this.radius);

      for (int z = minZPos; z <= maxZPos; z++) {
         for (int x = minXPos; x <= maxXPos; x++) {
            this.pos.set(x, 0, z);
            ChunkAccess chunk = this.level.getChunk(this.pos);

            for (int y = minYPos; y <= maxYPos; y++) {
               this.pos.setY(y);
               float strengthGivenYFalloff = this.strength - (float)(entityY - (double)this.pos.getY()) * 0.5F;
               this.setupInstance(chunk, this.pos, (float)entityX, (float)entityZ, strengthGivenYFalloff);
            }
         }
      }
   }

   private void setupInstance(ChunkAccess chunk, MutableBlockPos pos, float entityX, float entityZ, float strength) {
      int maxLocalRawBrightness = this.level.getMaxLocalRawBrightness(pos);
      if (maxLocalRawBrightness > 3) {
         float blockBrightness = LightTexture.getBrightness(this.level.dimensionType(), maxLocalRawBrightness);
         float alpha = strength * 0.5F * blockBrightness;
         if (!(alpha < 0.0F)) {
            if (alpha > 1.0F) {
               alpha = 1.0F;
            }

            pos.setY(pos.getY() - 1);
            VoxelShape shape = this.getShapeAt(chunk, pos);
            if (shape != null) {
               Vec3i renderOrigin = this.context.renderOrigin();
               int x = pos.getX() - renderOrigin.getX();
               int y = pos.getY() - renderOrigin.getY() + 1;
               int z = pos.getZ() - renderOrigin.getZ();
               double minX = (double)x + shape.min(Axis.X);
               double minY = (double)y + shape.min(Axis.Y);
               double minZ = (double)z + shape.min(Axis.Z);
               double maxX = (double)x + shape.max(Axis.X);
               double maxZ = (double)z + shape.max(Axis.Z);
               ShadowInstance instance = this.instances.get();
               instance.x = (float)minX;
               instance.y = (float)minY;
               instance.z = (float)minZ;
               instance.entityX = entityX - (float)renderOrigin.getX();
               instance.entityZ = entityZ - (float)renderOrigin.getZ();
               instance.sizeX = (float)(maxX - minX);
               instance.sizeZ = (float)(maxZ - minZ);
               instance.alpha = alpha;
               instance.radius = this.radius;
               instance.setChanged();
            }
         }
      }
   }

   @Nullable
   private VoxelShape getShapeAt(ChunkAccess chunk, BlockPos pos) {
      BlockState state = chunk.getBlockState(pos);
      if (state.getRenderShape() == RenderShape.INVISIBLE) {
         return null;
      } else if (!state.isCollisionShapeFullBlock(chunk, pos)) {
         return null;
      } else {
         VoxelShape shape = state.getShape(chunk, pos);
         return shape.isEmpty() ? null : shape;
      }
   }

   @Override
   public void delete() {
      this.instances.delete();
   }

   private static class ShadowMesh implements QuadMesh {
      private static final Vector4fc BOUNDING_SPHERE = new Vector4f(0.5F, 0.0F, 0.5F, (float)(Math.sqrt(2.0) * 0.5));
      private static final ShadowComponent.ShadowMesh INSTANCE = new ShadowComponent.ShadowMesh();

      @Override
      public int vertexCount() {
         return 4;
      }

      @Override
      public void write(MutableVertexList vertexList) {
         writeVertex(vertexList, 0, 0.0F, 0.0F);
         writeVertex(vertexList, 1, 0.0F, 1.0F);
         writeVertex(vertexList, 2, 1.0F, 1.0F);
         writeVertex(vertexList, 3, 1.0F, 0.0F);
      }

      private static void writeVertex(MutableVertexList vertexList, int i, float x, float z) {
         vertexList.x(i, x);
         vertexList.y(i, 0.0F);
         vertexList.z(i, z);
         vertexList.r(i, 1.0F);
         vertexList.g(i, 1.0F);
         vertexList.b(i, 1.0F);
         vertexList.u(i, 0.0F);
         vertexList.v(i, 0.0F);
         vertexList.light(i, 15728880);
         vertexList.overlay(i, OverlayTexture.NO_OVERLAY);
         vertexList.normalX(i, 0.0F);
         vertexList.normalY(i, 1.0F);
         vertexList.normalZ(i, 0.0F);
      }

      @Override
      public Vector4fc boundingSphere() {
         return BOUNDING_SPHERE;
      }
   }
}
