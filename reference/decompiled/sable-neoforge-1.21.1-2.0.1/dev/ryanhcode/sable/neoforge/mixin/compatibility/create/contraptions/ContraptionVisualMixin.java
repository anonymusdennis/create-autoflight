package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.contraptions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.render.ContraptionVisual;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractEntityVisual;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.neoforge.compatibility.flywheel.FlywheelCompatNeoForge;
import dev.ryanhcode.sable.neoforge.mixinterface.compatibility.flywheel.EmbeddedEnvironmentExtension;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ContraptionVisual.class})
public abstract class ContraptionVisualMixin extends AbstractEntityVisual<AbstractContraptionEntity> {
   @Shadow
   @Final
   protected VisualEmbedding embedding;
   @Shadow
   @Final
   private PoseStack contraptionMatrix;

   public ContraptionVisualMixin(VisualizationContext ctx, AbstractContraptionEntity entity, float partialTick) {
      super(ctx, entity, partialTick);
   }

   @Inject(
      method = {"setEmbeddingMatrices"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$setEmbeddingMatrices(float partialTick, CallbackInfo ci) {
      SubLevelContainer container = SubLevelContainer.getContainer(((AbstractContraptionEntity)this.entity).level());
      if (container != null) {
         ChunkPos chunkPos = ((AbstractContraptionEntity)this.entity).chunkPosition();
         boolean inBounds = container.inBounds(chunkPos);
         if (inBounds) {
            int plotX = (chunkPos.x >> container.getLogPlotSize()) - container.getOrigin().x;
            int plotZ = (chunkPos.z >> container.getLogPlotSize()) - container.getOrigin().y;
            FlywheelCompatNeoForge.SubLevelFlwRenderState state = FlywheelCompatNeoForge.getInfo(ChunkPos.asLong(plotX, plotZ));
            if (state != null) {
               Vec3i origin = this.renderOrigin();
               Vector3d pos = new Vector3d();
               if (((AbstractContraptionEntity)this.entity).isPrevPosInvalid()) {
                  pos.x = ((AbstractContraptionEntity)this.entity).getX();
                  pos.y = ((AbstractContraptionEntity)this.entity).getY();
                  pos.z = ((AbstractContraptionEntity)this.entity).getZ();
               } else {
                  pos.x = Mth.lerp((double)partialTick, ((AbstractContraptionEntity)this.entity).xo, ((AbstractContraptionEntity)this.entity).getX());
                  pos.y = Mth.lerp((double)partialTick, ((AbstractContraptionEntity)this.entity).yo, ((AbstractContraptionEntity)this.entity).getY());
                  pos.z = Mth.lerp((double)partialTick, ((AbstractContraptionEntity)this.entity).zo, ((AbstractContraptionEntity)this.entity).getZ());
               }

               ChunkPos centerChunk = state.centerChunk;
               PoseStack sceneMatrix = new PoseStack();
               sceneMatrix.translate((float)(pos.x - (double)centerChunk.getMinBlockX()), (float)pos.y, (float)(pos.z - (double)centerChunk.getMinBlockZ()));
               ((AbstractContraptionEntity)this.entity).applyLocalTransforms(sceneMatrix, partialTick);
               Pose3dc renderPose = state.renderPose;
               renderPose.transformPosition(pos).sub((double)origin.getX(), (double)origin.getY(), (double)origin.getZ());
               this.contraptionMatrix.setIdentity();
               this.contraptionMatrix.translate(pos.x, pos.y, pos.z);
               this.contraptionMatrix.mulPose(new Quaternionf(renderPose.orientation()));
               ((AbstractContraptionEntity)this.entity).applyLocalTransforms(this.contraptionMatrix, partialTick);
               this.embedding.transforms(this.contraptionMatrix.last().pose(), this.contraptionMatrix.last().normal());
               if (this.embedding instanceof EmbeddedEnvironmentExtension extension) {
                  extension.sable$setLightingInfo(sceneMatrix.last().pose(), state.sceneID, state.latestSkyLightScale / 15.0F);
               }

               ci.cancel();
            }
         }
      }
   }
}
