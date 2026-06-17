package dev.engine_room.flywheel.lib.visual;

import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.LightUpdatedVisual;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.math.MoreMath;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Iterator;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.FrustumIntersection;

public abstract class AbstractBlockEntityVisual<T extends BlockEntity> extends AbstractVisual implements BlockEntityVisual<T>, LightUpdatedVisual {
   protected final T blockEntity;
   protected final BlockPos pos;
   protected final BlockPos visualPos;
   protected final BlockState blockState;
   @UnknownNullability
   protected SectionTrackedVisual.SectionCollector lightSections;

   public AbstractBlockEntityVisual(VisualizationContext ctx, T blockEntity, float partialTick) {
      super(ctx, blockEntity.getLevel(), partialTick);
      this.blockEntity = blockEntity;
      this.pos = blockEntity.getBlockPos();
      this.blockState = blockEntity.getBlockState();
      this.visualPos = this.pos.subtract(ctx.renderOrigin());
   }

   @Override
   public void setSectionCollector(SectionTrackedVisual.SectionCollector sectionCollector) {
      this.lightSections = sectionCollector;
      this.lightSections.sections(LongSet.of(SectionPos.asLong(this.pos)));
   }

   public BlockPos getVisualPosition() {
      return this.visualPos;
   }

   public boolean isVisible(FrustumIntersection frustum) {
      float x = (float)this.visualPos.getX() + 0.5F;
      float y = (float)this.visualPos.getY() + 0.5F;
      float z = (float)this.visualPos.getZ() + 0.5F;
      return frustum.testSphere(x, y, z, MoreMath.SQRT_3_OVER_2);
   }

   public boolean doDistanceLimitThisFrame(DynamicVisual.Context context) {
      return !context.limiter().shouldUpdate(this.pos.distToCenterSqr(context.camera().getPosition()));
   }

   protected int computePackedLight() {
      return LevelRenderer.getLightColor(this.level, this.pos);
   }

   protected void relight(BlockPos pos, @Nullable FlatLit... instances) {
      FlatLit.relight(LevelRenderer.getLightColor(this.level, pos), instances);
   }

   protected void relight(@Nullable FlatLit... instances) {
      this.relight(this.pos, instances);
   }

   protected void relight(BlockPos pos, Iterator<FlatLit> instances) {
      FlatLit.relight(LevelRenderer.getLightColor(this.level, pos), instances);
   }

   protected void relight(Iterator<FlatLit> instances) {
      this.relight(this.pos, instances);
   }

   protected void relight(BlockPos pos, Iterable<FlatLit> instances) {
      FlatLit.relight(LevelRenderer.getLightColor(this.level, pos), instances);
   }

   protected void relight(Iterable<FlatLit> instances) {
      this.relight(this.pos, instances);
   }
}
