package dev.engine_room.flywheel.lib.visual;

import dev.engine_room.flywheel.api.visual.EntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.FrustumIntersection;
import org.joml.Vector3f;

public abstract class AbstractEntityVisual<T extends Entity> extends AbstractVisual implements EntityVisual<T> {
   protected final T entity;
   protected final EntityVisibilityTester visibilityTester;

   public AbstractEntityVisual(VisualizationContext ctx, T entity, float partialTick) {
      super(ctx, entity.level(), partialTick);
      this.entity = entity;
      this.visibilityTester = new EntityVisibilityTester(entity, ctx.renderOrigin(), 1.5F);
   }

   public double distanceSquared(double x, double y, double z) {
      return this.entity.distanceToSqr(x, y, z);
   }

   public Vector3f getVisualPosition() {
      Vec3 pos = this.entity.position();
      Vec3i renderOrigin = this.renderOrigin();
      return new Vector3f(
         (float)(pos.x - (double)renderOrigin.getX()), (float)(pos.y - (double)renderOrigin.getY()), (float)(pos.z - (double)renderOrigin.getZ())
      );
   }

   public Vector3f getVisualPosition(float partialTick) {
      Vec3 pos = this.entity.position();
      Vec3i renderOrigin = this.renderOrigin();
      return new Vector3f(
         (float)(Mth.lerp((double)partialTick, this.entity.xOld, pos.x) - (double)renderOrigin.getX()),
         (float)(Mth.lerp((double)partialTick, this.entity.yOld, pos.y) - (double)renderOrigin.getY()),
         (float)(Mth.lerp((double)partialTick, this.entity.zOld, pos.z) - (double)renderOrigin.getZ())
      );
   }

   public boolean isVisible(FrustumIntersection frustum) {
      return this.entity.noCulling || this.visibilityTester.check(frustum);
   }

   protected int computePackedLight(float partialTick) {
      BlockPos pos = BlockPos.containing(this.entity.getLightProbePosition(partialTick));
      int blockLight = this.entity.isOnFire() ? 15 : this.level.getBrightness(LightLayer.BLOCK, pos);
      int skyLight = this.level.getBrightness(LightLayer.SKY, pos);
      return LightTexture.pack(blockLight, skyLight);
   }

   protected void relight(float partialTick, @Nullable FlatLit... instances) {
      FlatLit.relight(this.computePackedLight(partialTick), instances);
   }
}
