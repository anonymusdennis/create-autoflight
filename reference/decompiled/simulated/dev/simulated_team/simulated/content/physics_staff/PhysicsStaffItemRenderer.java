package dev.simulated_team.simulated.content.physics_staff;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.SimMathUtils;
import foundry.veil.Veil;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.UUID;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.irisshaders.iris.Iris;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class PhysicsStaffItemRenderer extends CustomRenderedItemModelRenderer {
   private static final Vector3d focusPos = new Vector3d();
   private static final Matrix4f itemProjMat = new Matrix4f();

   public static Vec3 getFirstPersonFocusPos(float pt) {
      GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
      Camera camera = gameRenderer.getMainCamera();
      Vector3d focusPoint = new Vector3d(focusPos);
      Quaternionf orientation = camera.rotation();
      orientation.transformInverse(focusPoint);
      Vector4f v4 = new Vector4f((float)focusPoint.x, (float)focusPoint.y, (float)focusPoint.z, 1.0F);
      Matrix4f actualProjMat = gameRenderer.getProjectionMatrix(gameRenderer.getFov(camera, AnimationTickHolder.getPartialTicks(), true));
      actualProjMat.invert(new Matrix4f()).transform(v4);
      itemProjMat.transform(v4);
      focusPoint.set((double)v4.x, (double)v4.y, (double)v4.z);
      orientation.transform(focusPoint);
      double fov = gameRenderer.getFov(camera, pt, true);
      focusPoint.mul(100.0 / fov);
      return JOMLConversion.toMojang(focusPoint);
   }

   protected void render(
      ItemStack stack,
      CustomRenderedItemModel model,
      PartialItemModelRenderer renderer,
      ItemDisplayContext context,
      PoseStack ms,
      MultiBufferSource buffer,
      int light,
      int overlay
   ) {
      float openAmount = 0.0F;
      float cubeScale = 0.0F;
      PhysicsStaffClientHandler clientHandler = SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER;
      Minecraft minecraft = Minecraft.getInstance();
      float partialTicks = AnimationTickHolder.getPartialTicks();
      Player player = SimDistUtil.getClientPlayer();
      if (player != null
         && (context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
         if (player.getMainHandItem() != stack && player.getOffhandItem() != stack) {
            ObjectIterator tiltAmount = clientHandler.beams.keySet().iterator();

            while (tiltAmount.hasNext()) {
               UUID playerUUID = (UUID)tiltAmount.next();
               Player otherPlayer = minecraft.level.getPlayerByUUID(playerUUID);
               if (otherPlayer != null && (otherPlayer.getMainHandItem() == stack || otherPlayer.getOffhandItem() == stack)) {
                  openAmount = Mth.lerp(
                     partialTicks,
                     ((PhysicsStaffClientHandler.PhysicsBeam)clientHandler.beams.get(playerUUID)).previousExtension,
                     ((PhysicsStaffClientHandler.PhysicsBeam)clientHandler.beams.get(playerUUID)).extension
                  );
                  cubeScale = Mth.lerp(
                     partialTicks,
                     ((PhysicsStaffClientHandler.PhysicsBeam)clientHandler.beams.get(playerUUID)).previousCubeScale,
                     ((PhysicsStaffClientHandler.PhysicsBeam)clientHandler.beams.get(playerUUID)).cubeScale
                  );
                  break;
               }
            }
         } else {
            openAmount = Mth.lerp(partialTicks, clientHandler.previousExtension, clientHandler.extension);
            cubeScale = Mth.lerp(partialTicks, clientHandler.previousCubeScale, clientHandler.cubeScale);
         }
      }

      float tiltAmount = Mth.lerp(partialTicks, clientHandler.previousTilt, clientHandler.tilt);
      Quaternionf utilQuat = new Quaternionf();
      boolean shadersActive = Veil.IRIS && Iris.isPackInUseQuick();
      if (context.firstPerson()) {
         if (clientHandler.getDragSession() != null) {
            PhysicsStaffClientHandler.ClientDragSession dragSession = clientHandler.getDragSession();
            Quaternionf rotation = minecraft.gameRenderer.getMainCamera().rotation();
            Vector3d globalAnchor = ((ClientSubLevel)dragSession.dragSubLevel()).renderPose().transformPosition(new Vector3d(dragSession.dragLocalAnchor()));
            Vector3d dirToAnchor = globalAnchor.sub(JOMLConversion.toJOML(player.getEyePosition(partialTicks))).normalize();
            rotation.transformInverse(dirToAnchor);
            Quaternionf quat = SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0.0, 0.0, -1.0), dirToAnchor);
            ms.mulPose(utilQuat.identity().rotateY((float) (-Math.PI / 2)));
            ms.mulPose(quat.slerp(utilQuat.identity(), 0.6F));
            ms.mulPose(utilQuat.identity().rotateY((float) (Math.PI / 2)));
         }

         float tiltMultiplier = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1.0F : 1.0F;
         ms.mulPose(utilQuat.identity().rotateZ((float)Math.toRadians(((double)tiltAmount * 0.5 + 0.5) * -61.0) * tiltMultiplier));
      }

      renderer.render(model.getOriginalModel(), Sheets.cutoutBlockSheet(), light);
      renderer.render(SimPartialModels.PHYSICS_STAFF_CORE.get(), SimRenderTypes.itemGlowingSolid(shadersActive), 15728880);
      renderer.render(SimPartialModels.PHYSICS_STAFF_CORE_GLOW.get(), SimRenderTypes.itemGlowingTranslucent(shadersActive), 15728880);
      float worldTime = AnimationTickHolder.getRenderTime() / 20.0F;
      ms.pushPose();
      ms.translate(0.0, 0.40625, 0.0);
      renderer.render(SimPartialModels.PHYSICS_STAFF_RING.get(), Sheets.cutoutBlockSheet(), light);
      ms.popPose();
      ms.translate(0.0, 0.5625, 0.0);

      for (int i = 0; i < 2; i++) {
         ms.pushPose();
         ms.mulPose(Axis.YP.rotationDegrees((float)(i * 180)));
         ms.translate(-0.1875, 0.0, 0.0);
         ms.mulPose(Axis.ZP.rotationDegrees(openAmount * 20.0F));
         renderer.render(SimPartialModels.PHYSICS_STAFF_SIGMA.get(), Sheets.cutoutBlockSheet(), light);
         ms.popPose();
      }

      ms.translate(0.0, 0.375, 0.0);
      if (context.firstPerson()) {
         if (clientHandler.getDragSession() != null) {
            clientHandler.lastCubeOrientation.set(clientHandler.getDragSession().dragOrientation());
         }

         Matrix4f m = new Matrix4f(ms.last().pose());
         m.m30(0.0F).m31(0.0F).m32(0.0F);
         m.invert();
         m.rotate(clientHandler.lastCubeOrientation);
         ms.mulPose(m);
      }

      cubeScale = Mth.lerp(cubeScale, -0.05F, 1.0F);
      cubeScale = Mth.clamp(cubeScale, 0.0F, 1.0F);
      cubeScale *= 0.8F;
      ms.scale(cubeScale, cubeScale, cubeScale);
      renderer.render(SimPartialModels.PHYSICS_STAFF_INNER_CUBE.get(), SimRenderTypes.itemGlowingSolid(shadersActive), 15728880);
      if (context.firstPerson()) {
         Vector3f focusPoint = new Vector3f();
         ms.last().pose().transformPosition(focusPoint);
         itemProjMat.set(RenderSystem.getProjectionMatrix());
         focusPos.set((double)focusPoint.x, (double)focusPoint.y, (double)focusPoint.z);
      }

      ms.scale(1.2F, 1.2F, 1.2F);
      renderer.render(SimPartialModels.PHYSICS_STAFF_OUTER_CUBE.get(), SimRenderTypes.itemGlowingTranslucent(shadersActive), 15728880);
      if (Veil.IRIS && !shadersActive) {
         Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
      }
   }
}
