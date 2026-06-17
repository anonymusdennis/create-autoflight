package dev.engine_room.flywheel.lib.visual.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.QuadMesh;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import dev.engine_room.flywheel.lib.visual.util.SmartRecycler;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class FireComponent implements EntityComponent {
   private static final Material FIRE_MATERIAL = SimpleMaterial.builderOf(Materials.CUTOUT_UNSHADED_BLOCK).backfaceCulling(false).build();
   private static final RendererReloadCache<net.minecraft.client.resources.model.Material, Model> FIRE_MODELS = new RendererReloadCache<>(
      texture -> new SingleMeshModel(new FireComponent.FireMesh(texture.sprite()), FIRE_MATERIAL)
   );
   private final VisualizationContext context;
   private final Entity entity;
   private final PoseStack stack = new PoseStack();
   private final SmartRecycler<Model, TransformedInstance> recycler;

   public FireComponent(VisualizationContext context, Entity entity) {
      this.context = context;
      this.entity = entity;
      this.recycler = new SmartRecycler<>(this::createInstance);
   }

   private TransformedInstance createInstance(Model model) {
      TransformedInstance instance = this.context.instancerProvider().instancer(InstanceTypes.TRANSFORMED, model).createInstance();
      instance.light(240);
      instance.setChanged();
      return instance;
   }

   @Override
   public void beginFrame(DynamicVisual.Context context) {
      this.recycler.resetCount();
      if (this.entity.displayFireAnimation()) {
         this.setupInstances(context);
      }

      this.recycler.discardExtra();
   }

   private void setupInstances(DynamicVisual.Context context) {
      double entityX = Mth.lerp((double)context.partialTick(), this.entity.xOld, this.entity.getX());
      double entityY = Mth.lerp((double)context.partialTick(), this.entity.yOld, this.entity.getY());
      double entityZ = Mth.lerp((double)context.partialTick(), this.entity.zOld, this.entity.getZ());
      Vec3i renderOrigin = this.context.renderOrigin();
      float scale = this.entity.getBbWidth() * 1.4F;
      float maxHeight = this.entity.getBbHeight() / scale;
      float width = 1.0F;
      float y = 0.0F;
      float z = 0.0F;
      this.stack.setIdentity();
      this.stack.translate(entityX - (double)renderOrigin.getX(), entityY - (double)renderOrigin.getY(), entityZ - (double)renderOrigin.getZ());
      this.stack.scale(scale, scale, scale);
      this.stack.mulPose(Axis.YP.rotationDegrees(-context.camera().getYRot()));
      this.stack.translate(0.0F, 0.0F, -0.3F + (float)((int)maxHeight) * 0.02F);

      for (int i = 0; y < maxHeight; i++) {
         TransformedInstance instance = this.recycler
            .get(FIRE_MODELS.get(i % 2 == 0 ? ModelBakery.FIRE_0 : ModelBakery.FIRE_1))
            .setTransform(this.stack)
            .scaleX(width)
            .translate(0.0F, y, z);
         if (i / 2 % 2 == 0) {
            instance.scaleX(-1.0F);
         }

         instance.setChanged();
         y += 0.45F;
         width *= 0.9F;
         z += 0.03F;
      }
   }

   @Override
   public void delete() {
      this.recycler.delete();
   }

   private static record FireMesh(TextureAtlasSprite sprite) implements QuadMesh {
      private static final Vector4fc BOUNDING_SPHERE = new Vector4f(0.0F, 0.5F, 0.0F, Mth.SQRT_OF_TWO * 0.5F);

      @Override
      public int vertexCount() {
         return 4;
      }

      @Override
      public void write(MutableVertexList vertexList) {
         float u0 = this.sprite.getU0();
         float v0 = this.sprite.getV0();
         float u1 = this.sprite.getU1();
         float v1 = this.sprite.getV1();
         writeVertex(vertexList, 0, 0.5F, 0.0F, u1, v1);
         writeVertex(vertexList, 1, -0.5F, 0.0F, u0, v1);
         writeVertex(vertexList, 2, -0.5F, 1.4F, u0, v0);
         writeVertex(vertexList, 3, 0.5F, 1.4F, u1, v0);
      }

      private static void writeVertex(MutableVertexList vertexList, int i, float x, float y, float u, float v) {
         vertexList.x(i, x);
         vertexList.y(i, y);
         vertexList.z(i, 0.0F);
         vertexList.r(i, 1.0F);
         vertexList.g(i, 1.0F);
         vertexList.b(i, 1.0F);
         vertexList.u(i, u);
         vertexList.v(i, v);
         vertexList.light(i, 240);
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
