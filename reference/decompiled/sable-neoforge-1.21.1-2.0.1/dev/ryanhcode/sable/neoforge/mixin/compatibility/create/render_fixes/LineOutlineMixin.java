package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.render_fixes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.outliner.LineOutline;
import net.createmod.catnip.outliner.Outline;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({LineOutline.class})
public abstract class LineOutlineMixin extends Outline {
   public void bufferCuboidLine(
      PoseStack poseStack,
      VertexConsumer consumer,
      Vec3 camera,
      Vector3d start,
      Vector3d end,
      float width,
      Vector4f color,
      int lightmap,
      boolean disableNormals
   ) {
      ActiveSableCompanion helper = Sable.HELPER;
      ClientSubLevel startSubLevel = helper.getContainingClient(start);
      ClientSubLevel endSubLevel = helper.getContainingClient(end);
      if (startSubLevel != null) {
         startSubLevel.renderPose().transformPosition(start);
      }

      if (endSubLevel != null) {
         endSubLevel.renderPose().transformPosition(end);
      }

      Vector3f diff = this.diffPosTemp;
      diff.set((float)(end.x - start.x), (float)(end.y - start.y), (float)(end.z - start.z));
      float length = Mth.sqrt(diff.x() * diff.x() + diff.y() * diff.y() + diff.z() * diff.z());
      float hAngle = AngleHelper.deg(Mth.atan2((double)diff.x(), (double)diff.z()));
      float hDistance = Mth.sqrt(diff.x() * diff.x() + diff.z() * diff.z());
      float vAngle = AngleHelper.deg(Mth.atan2((double)hDistance, (double)diff.y())) - 90.0F;
      poseStack.pushPose();
      ((PoseTransformStack)((PoseTransformStack)TransformStack.of(poseStack).translate(start.x - camera.x, start.y - camera.y, start.z - camera.z))
            .rotateYDegrees(hAngle))
         .rotateXDegrees(vAngle);
      this.bufferCuboidLine(poseStack.last(), consumer, new Vector3f(), Direction.SOUTH, length, width, color, lightmap, disableNormals);
      poseStack.popPose();
   }
}
