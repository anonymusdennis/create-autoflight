package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.render_fixes;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.SublevelRenderOffsetHelper;
import net.createmod.catnip.outliner.BlockClusterOutline;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockClusterOutline.class})
public class BlockClusterOutlineMixin {
   @Unique
   private Iterable<BlockPos> sable$collection = null;
   @Unique
   private Vec3 sable$center = Vec3.ZERO;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")},
      remap = false
   )
   private void sable$gatherSublevel(Iterable<BlockPos> selection, CallbackInfo ci) {
      this.sable$collection = selection;
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      remap = false
   )
   private void sable$projectFromSublevel(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt, CallbackInfo ci) {
      ms.pushPose();

      for (BlockPos pos : this.sable$collection) {
         SubLevel sublevel = Sable.HELPER.getContainingClient((double)pos.getX() + 0.5, (double)pos.getZ() + 0.5);
         if (sublevel != null) {
            this.sable$center = Vec3.atCenterOf(pos);
            SublevelRenderOffsetHelper.posePlotToProjected(sublevel, ms);
            break;
         }
      }
   }

   @ModifyVariable(
      method = {"render"},
      at = @At("HEAD"),
      remap = false,
      argsOnly = true
   )
   private Vec3 sable$modifyCamera(Vec3 camera) {
      if (this.sable$center == null) {
         for (BlockPos pos : this.sable$collection) {
            SubLevel sublevel = Sable.HELPER.getContainingClient((double)pos.getX() + 0.5, (double)pos.getZ() + 0.5);
            if (sublevel != null) {
               this.sable$center = Vec3.atCenterOf(pos);
               break;
            }
         }
      }

      return camera.add(SublevelRenderOffsetHelper.translation(this.sable$center));
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")},
      remap = false
   )
   private void sable$popPose(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt, CallbackInfo ci) {
      ms.popPose();
      this.sable$center = Vec3.ZERO;
   }
}
