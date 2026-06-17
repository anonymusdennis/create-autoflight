package dev.ryanhcode.sable.util;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3dc;

public class SublevelRenderOffsetHelper {
   public static void posePlotToProjected(SubLevel subLevel, PoseStack ps) {
      if (subLevel != null) {
         Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
         Pose3dc pose = ((ClientSubLevel)subLevel).renderPose();
         Vector3dc pos = pose.position();
         Vector3dc scale = pose.scale();
         Quaterniondc orientation = pose.orientation();
         ps.translate(pos.x() - camera.x, pos.y() - camera.y, pos.z() - camera.z);
         ps.mulPose(new Quaternionf(orientation));
         ps.translate(camera.x, camera.y, camera.z);
         ps.scale((float)scale.x(), (float)scale.y(), (float)scale.z());
      }
   }

   public static Vec3 translation(Vec3 center) {
      ClientSubLevel subLevel = Sable.HELPER.getContainingClient(center);
      if (subLevel != null) {
         Pose3dc pose = subLevel.renderPose();
         return JOMLConversion.toMojang(pose.rotationPoint()).scale(1.0);
      } else {
         return Vec3.ZERO;
      }
   }
}
