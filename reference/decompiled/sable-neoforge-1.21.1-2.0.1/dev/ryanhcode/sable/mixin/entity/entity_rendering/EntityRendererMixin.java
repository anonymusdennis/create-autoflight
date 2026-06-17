package dev.ryanhcode.sable.mixin.entity.entity_rendering;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderer.class})
public abstract class EntityRendererMixin {
   @Shadow
   @Final
   protected EntityRenderDispatcher entityRenderDispatcher;

   @ModifyReturnValue(
      method = {"getPackedLightCoords"},
      at = {@At("RETURN")}
   )
   public final int getPackedLightCoords(int original, Entity arg, float f) {
      Vec3 lightProbeOffset = arg.getLightProbePosition(f).subtract(arg.getEyePosition(f));
      Vector3d lightProbePosition = JOMLConversion.toJOML(Sable.HELPER.getEyePositionInterpolated(arg, f))
         .add(lightProbeOffset.x, lightProbeOffset.y, lightProbeOffset.z);
      BlockPos blockpos = BlockPos.containing(lightProbePosition.x, lightProbePosition.y, lightProbePosition.z);
      return LightTexture.pack(
         sable$getSubLevelAccountedBlockLight(original, arg.level(), LightLayer.BLOCK, blockpos, lightProbePosition),
         sable$getSubLevelAccountedSkyLight(original, arg.level(), LightLayer.SKY, blockpos, lightProbePosition)
      );
   }

   @Redirect(
      method = {"getSkyLightLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I"
      )
   )
   private int sable$getSkyLightLevel(Level instance, LightLayer lightLayer, BlockPos blockPos) {
      return sable$getSubLevelAccountedSkyLight(-1, instance, lightLayer, blockPos, JOMLConversion.atCenterOf(blockPos));
   }

   @Unique
   private static int sable$getSubLevelAccountedSkyLight(int original, Level instance, LightLayer lightLayer, BlockPos blockPos, Vector3dc probePosition) {
      Iterable<SubLevel> all = Sable.HELPER.getAllIntersecting(instance, new BoundingBox3d(blockPos));
      int baseBrightness = original == -1 ? instance.getBrightness(lightLayer, blockPos) : LightTexture.sky(original);
      MutableBlockPos localPosition = new MutableBlockPos();
      MutableBlockPos heightmapPos = new MutableBlockPos();
      Vector3d tempProbePosition = new Vector3d();

      for (SubLevel subLevel : all) {
         ClientSubLevel clientSubLevel = (ClientSubLevel)subLevel;
         clientSubLevel.renderPose().transformPositionInverse(probePosition, tempProbePosition);
         localPosition.set(tempProbePosition.x, tempProbePosition.y, tempProbePosition.z);
         Level level = subLevel.getLevel();
         heightmapPos.setWithOffset(localPosition, Direction.UP);
         LevelPlot plot = subLevel.getPlot();
         boolean isAboveGround = false;

         while (heightmapPos.getY() >= plot.getBoundingBox().minY()) {
            if (!level.getBlockState(heightmapPos).isAir()) {
               isAboveGround = true;
               break;
            }

            heightmapPos.move(Direction.DOWN);
         }

         if (isAboveGround) {
            if (lightLayer == LightLayer.BLOCK) {
               baseBrightness = Math.max(baseBrightness, level.getBrightness(lightLayer, localPosition));
            } else if (lightLayer == LightLayer.SKY) {
               int brightness = clientSubLevel.scaleSkyLight(level.getBrightness(lightLayer, localPosition));
               baseBrightness = Math.min(baseBrightness, brightness);
            }
         }
      }

      return baseBrightness;
   }

   @Redirect(
      method = {"getBlockLightLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I"
      )
   )
   private int sable$getBlockLightLevel(Level instance, LightLayer lightLayer, BlockPos blockPos) {
      return sable$getSubLevelAccountedBlockLight(-1, instance, lightLayer, blockPos, JOMLConversion.atCenterOf(blockPos));
   }

   @Unique
   private static int sable$getSubLevelAccountedBlockLight(int original, Level instance, LightLayer lightLayer, BlockPos blockPos, Vector3dc lightProbePosition) {
      Iterable<SubLevel> all = Sable.HELPER.getAllIntersecting(instance, new BoundingBox3d(blockPos).expand(2.0));
      int l = original == -1 ? instance.getBrightness(lightLayer, blockPos) : LightTexture.block(original);
      MutableBlockPos probeBlockPos = new MutableBlockPos();
      Vector3d tempProbePosition = new Vector3d();

      for (SubLevel subLevel : all) {
         ClientSubLevel clientSubLevel = (ClientSubLevel)subLevel;
         clientSubLevel.renderPose().transformPositionInverse(lightProbePosition, tempProbePosition);
         l = Math.max(l, subLevel.getLevel().getBrightness(lightLayer, probeBlockPos.set(tempProbePosition.x, tempProbePosition.y, tempProbePosition.z)));
      }

      return l;
   }

   @Inject(
      method = {"shouldRender"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private <E extends Entity> void sable$shouldRender(E entity, Frustum frustum, double pCamX, double pCamY, double pCamZ, CallbackInfoReturnable<Boolean> cir) {
      if (entity.noCulling) {
         cir.setReturnValue(true);
      } else {
         ClientSubLevel subLevel = Sable.HELPER.getContainingClient(entity);
         if (subLevel != null) {
            Vec3 globalPos = subLevel.renderPose().transformPosition(entity.position());
            AABB aabb = new AABB(globalPos.x - 2.0, globalPos.y - 2.0, globalPos.z - 2.0, globalPos.x + 2.0, globalPos.y + 2.0, globalPos.z + 2.0);
            cir.setReturnValue(frustum.isVisible(aabb));
         } else {
            SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(entity);
            if (trackingSubLevel != null) {
               float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
               Vec3 positionInterpolated = Sable.HELPER.getEyePositionInterpolated(entity, pt).subtract(0.0, (double)entity.getEyeHeight(), 0.0);
               AABB aABB = entity.getBoundingBoxForCulling().inflate(0.5);
               if (aABB.hasNaN() || aABB.getSize() == 0.0) {
                  aABB = new AABB(entity.getX() - 2.0, entity.getY() - 2.0, entity.getZ() - 2.0, entity.getX() + 2.0, entity.getY() + 2.0, entity.getZ() + 2.0);
               }

               aABB = aABB.move(positionInterpolated.subtract(entity.position()));
               if (frustum.isVisible(aABB)) {
                  cir.setReturnValue(true);
               } else {
                  if (entity instanceof Leashable leashable) {
                     Entity entity2 = leashable.getLeashHolder();
                     if (entity2 != null) {
                        cir.setReturnValue(frustum.isVisible(entity2.getBoundingBoxForCulling()));
                        return;
                     }
                  }

                  cir.setReturnValue(false);
               }
            }
         }
      }
   }
}
