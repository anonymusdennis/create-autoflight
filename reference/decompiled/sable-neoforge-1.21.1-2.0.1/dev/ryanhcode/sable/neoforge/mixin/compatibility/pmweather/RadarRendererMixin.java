package dev.ryanhcode.sable.neoforge.mixin.compatibility.pmweather;

import com.llamalad7.mixinextras.sugar.Local;
import dev.protomanly.pmweather.block.entity.RadarBlockEntity;
import dev.protomanly.pmweather.render.RadarRenderer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({RadarRenderer.class})
public class RadarRendererMixin {
   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"
      )
   )
   private double sable$redirectDistanceTo(Vec3 position, Vec3 blockEntityPos) {
      ClientLevel level = Minecraft.getInstance().level;
      return Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(level, position, blockEntityPos));
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Ldev/protomanly/pmweather/block/entity/RadarBlockEntity;getBlockPos()Lnet/minecraft/core/BlockPos;"
      )
   )
   public BlockPos sable$render(RadarBlockEntity instance) {
      Vec3 globalPos = Sable.HELPER.projectOutOfSubLevel(instance.getLevel(), instance.getBlockPos().getCenter());
      return BlockPos.containing(globalPos);
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;",
         ordinal = 0
      )
   )
   public Vec3 sable$renderWorldPos(Vec3 instance, double x, double y, double z, @Local RadarBlockEntity blockEntity) {
      SubLevel subLevel = Sable.HELPER.getContaining(blockEntity);
      if (subLevel == null) {
         return instance.multiply(x, y, z);
      } else {
         Vec3 globalDir = subLevel.logicalPose().transformNormal(new Vec3(instance.x, 0.0, instance.z));
         return new Vec3(globalDir.x, 0.0, globalDir.z).multiply(x, y, z);
      }
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec2;normalized()Lnet/minecraft/world/phys/Vec2;",
         ordinal = 0
      )
   )
   public Vec2 sable$renderWind(Vec2 instance, @Local RadarBlockEntity blockEntity) {
      SubLevel subLevel = Sable.HELPER.getContaining(blockEntity);
      if (subLevel == null) {
         return instance.normalized();
      } else {
         Vec3 globalDir = subLevel.logicalPose().transformNormal(new Vec3((double)instance.x, 0.0, (double)instance.y));
         return new Vec2((float)globalDir.x, (float)globalDir.z).normalized();
      }
   }
}
