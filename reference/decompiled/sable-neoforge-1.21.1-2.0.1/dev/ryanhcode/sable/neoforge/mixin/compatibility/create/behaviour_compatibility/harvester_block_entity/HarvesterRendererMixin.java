package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.behaviour_compatibility.harvester_block_entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterBlockEntity;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterRenderer;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.harvester.HarvesterLerpedSpeed;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({HarvesterRenderer.class})
public class HarvesterRendererMixin {
   @WrapOperation(
      method = {"renderSafe(Lcom/simibubi/create/content/contraptions/actors/harvester/HarvesterBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/contraptions/actors/harvester/HarvesterRenderer;transform(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Direction;Lnet/createmod/catnip/render/SuperByteBuffer;FLnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   public void sable$smoothSpeed(
      Level world,
      Direction facing,
      SuperByteBuffer superBuffer,
      float speed,
      Vec3 pivot,
      Operation<Void> original,
      @Local HarvesterBlockEntity be,
      @Local float pt
   ) {
      if (be.getAnimatedSpeed() != 0.0F) {
         original.call(new Object[]{world, facing, superBuffer, speed, pivot});
      } else {
         float originOffset = 0.0625F;
         Vec3 rotOffset = new Vec3(0.0, pivot.y * 0.0625, pivot.z * 0.0625);
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)superBuffer.rotateCentered(
                     AngleHelper.rad((double)AngleHelper.horizontalAngle(facing)), Direction.UP
                  ))
                  .translate(rotOffset.x, rotOffset.y, rotOffset.z))
               .rotate(AngleHelper.rad((double)(-((HarvesterLerpedSpeed)be).sable$getLerpedFloat().getValue(pt))), Direction.WEST))
            .translate(-rotOffset.x, -rotOffset.y, -rotOffset.z);
      }
   }
}
