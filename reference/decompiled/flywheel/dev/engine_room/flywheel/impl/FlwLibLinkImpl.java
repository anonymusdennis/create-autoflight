package dev.engine_room.flywheel.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.engine_room.flywheel.impl.compat.IrisCompat;
import dev.engine_room.flywheel.impl.compat.OptifineCompat;
import dev.engine_room.flywheel.impl.extension.PoseStackExtension;
import dev.engine_room.flywheel.impl.mixin.ModelPartAccessor;
import dev.engine_room.flywheel.impl.mixin.PoseStackAccessor;
import dev.engine_room.flywheel.lib.internal.FlwLibLink;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import java.util.Deque;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.slf4j.Logger;

public class FlwLibLinkImpl implements FlwLibLink {
   @Override
   public Logger getLogger() {
      return FlwImpl.LOGGER;
   }

   @Override
   public PoseTransformStack getPoseTransformStackOf(PoseStack stack) {
      return ((PoseStackExtension)stack).flywheel$transformStack();
   }

   @Override
   public Map<String, ModelPart> getModelPartChildren(ModelPart part) {
      return ((ModelPartAccessor)part).flywheel$children();
   }

   @Override
   public void compileModelPart(ModelPart part, Pose pose, VertexConsumer consumer, int light, int overlay, int color) {
      ((ModelPartAccessor)part).flywheel$compile(pose, consumer, light, overlay, color);
   }

   @Override
   public Deque<Pose> getPoseStack(PoseStack stack) {
      return ((PoseStackAccessor)stack).flywheel$getPoseStack();
   }

   @Override
   public boolean isIrisLoaded() {
      return IrisCompat.ACTIVE;
   }

   @Override
   public boolean isOptifineInstalled() {
      return OptifineCompat.IS_INSTALLED;
   }

   @Override
   public boolean isShaderPackInUse() {
      if (IrisCompat.ACTIVE) {
         return IrisCompat.isShaderPackInUse();
      } else {
         return OptifineCompat.IS_INSTALLED ? OptifineCompat.isShaderPackInUse() : false;
      }
   }

   @Override
   public boolean isRenderingShadowPass() {
      if (IrisCompat.ACTIVE) {
         return IrisCompat.isRenderingShadowPass();
      } else {
         return OptifineCompat.IS_INSTALLED ? OptifineCompat.isRenderingShadowPass() : false;
      }
   }
}
