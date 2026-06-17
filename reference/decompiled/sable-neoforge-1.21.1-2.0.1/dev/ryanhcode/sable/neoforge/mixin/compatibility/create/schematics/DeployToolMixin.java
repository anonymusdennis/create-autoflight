package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.schematics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.schematics.client.tools.DeployTool;
import com.simibubi.create.content.schematics.client.tools.SchematicToolBase;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.renderers.AABBOutlineRenderingOptions;
import dev.ryanhcode.sable.util.SublevelRenderOffsetHelper;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({DeployTool.class})
public abstract class DeployToolMixin extends SchematicToolBase {
   @WrapOperation(
      method = {"renderTool"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"
      )}
   )
   public void sable$manualTransformBB(
      PoseStack instance,
      double x,
      double y,
      double z,
      Operation<Void> original,
      @Local(ordinal = 0) int centerX,
      @Local(ordinal = 1) int centerZ,
      @Local(argsOnly = true) Vec3 camera
   ) {
      SublevelRenderOffsetHelper.posePlotToProjected(Sable.HELPER.getContainingClient(this.selectedPos), instance);
      Vec3 trans = SublevelRenderOffsetHelper.translation(this.selectedPos.getCenter());
      original.call(new Object[]{instance, x - trans.x, y - trans.y, z - trans.z});
      ((AABBOutlineRenderingOptions)this.schematicHandler.getOutline()).sable$shouldTransform(false);
   }
}
