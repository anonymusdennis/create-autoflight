package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.trains;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.render.ContraptionVisual;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.Translate;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.compatibility.flywheel.FlywheelCompatNeoForge;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({CarriageContraptionVisual.class})
public abstract class CarriageContraptionVisualMixin extends ContraptionVisual<CarriageContraptionEntity> {
   public CarriageContraptionVisualMixin(VisualizationContext ctx, CarriageContraptionEntity entity, float partialTick) {
      super(ctx, entity, partialTick);
   }

   @Redirect(
      method = {"animate"},
      at = @At(
         value = "INVOKE",
         target = "Ldev/engine_room/flywheel/lib/transform/PoseTransformStack;translate(Lorg/joml/Vector3fc;)Ldev/engine_room/flywheel/lib/transform/Translate;"
      )
   )
   private Translate sable$translate(
      PoseTransformStack instance, Vector3fc vector3fc, @Local Vector3f visualPosition, @Local(argsOnly = true) float partialTick
   ) {
      Vec3 pos = ((CarriageContraptionEntity)this.entity).position();
      SubLevelContainer container = SubLevelContainer.getContainer(((CarriageContraptionEntity)this.entity).level());
      if (container == null) {
         instance.translate(vector3fc);
         return instance;
      } else {
         ChunkPos chunkPos = ((CarriageContraptionEntity)this.entity).chunkPosition();
         boolean inBounds = container.inBounds(chunkPos);
         if (!inBounds) {
            instance.translate(vector3fc);
            return instance;
         } else {
            int plotX = (chunkPos.x >> container.getLogPlotSize()) - container.getOrigin().x;
            int plotZ = (chunkPos.z >> container.getLogPlotSize()) - container.getOrigin().y;
            FlywheelCompatNeoForge.SubLevelFlwRenderState state = FlywheelCompatNeoForge.getInfo(ChunkPos.asLong(plotX, plotZ));
            if (state == null) {
               instance.translate(vector3fc);
               return instance;
            } else {
               double entityX = Mth.lerp((double)partialTick, ((CarriageContraptionEntity)this.entity).xOld, pos.x);
               double entityY = Mth.lerp((double)partialTick, ((CarriageContraptionEntity)this.entity).yOld, pos.y);
               double entityZ = Mth.lerp((double)partialTick, ((CarriageContraptionEntity)this.entity).zOld, pos.z);
               Vec3i origin = this.renderOrigin();
               Vec3 renderPos = state.renderPose
                  .transformPosition(new Vec3(entityX, entityY, entityZ))
                  .subtract((double)origin.getX(), (double)origin.getY(), (double)origin.getZ());
               instance.translate(renderPos.x, renderPos.y, renderPos.z);
               instance.rotate(new Quaternionf(state.renderPose.orientation()));
               return instance;
            }
         }
      }
   }
}
